/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.message.client;

import lombok.ToString;
import oap.message.MessageProtocol;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32C;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Spool segment: an append-only file of messages of one priority.
 * <pre>
 * header: int magic ("OMSP") | byte format version
 * record: int length (of data) | byte messageType | short version | long time (enqueued, ms) | byte[16] md5 | data
 *         | int crc32c (messageType .. data)
 * </pre>
 * A record with an invalid length or crc ends the segment (a torn tail of a crash).
 * <p>
 * Replay state: a segment is deleted when it is read to the end and every read record is final (delivered / dropped).
 */
@ToString( of = { "path", "records", "bytes" } )
final class SpoolSegment {
    static final int MAGIC = 0x4F4D5350;
    static final byte FORMAT_VERSION = 1;
    static final int HEADER_LENGTH = 4 + 1;
    static final int RECORD_HEADER_LENGTH = 4 + 1 + 2 + 8 + MessageProtocol.MD5_LENGTH;
    static final int CRC_LENGTH = 4;
    static final String EXTENSION = ".seg";

    final Path path;
    final int priority;
    /**
     * written under the stream write lock ( or before the segment is published ), read by the gauges
     */
    volatile long bytes;
    volatile int records;
    volatile long firstTime = Long.MAX_VALUE;

    // replay state, guarded by this
    private int acked;
    private int outstanding;
    private boolean readDone;
    private boolean deleted;

    SpoolSegment( Path path, int priority ) {
        this.path = path;
        this.priority = priority;
    }

    static String fileName( long seq ) {
        return String.format( "%016d", seq ) + EXTENSION;
    }

    /**
     * header only scan of a segment written by another sender: number of records, time of the first one, size.
     * The crc is checked on replay.
     */
    static void scan( SpoolSegment segment ) throws IOException {
        try( FileChannel channel = FileChannel.open( segment.path, READ ) ) {
            long size = channel.size();
            segment.bytes = size;

            ByteBuffer header = ByteBuffer.allocate( RECORD_HEADER_LENGTH );
            if( size < HEADER_LENGTH || !readFully( channel, header.limit( HEADER_LENGTH ), 0 )
                || header.flip().getInt() != MAGIC || header.get() != FORMAT_VERSION ) {
                return;
            }

            long position = HEADER_LENGTH;
            int records = 0;
            long firstTime = Long.MAX_VALUE;
            while( position + RECORD_HEADER_LENGTH + CRC_LENGTH <= size ) {
                header.clear();
                if( !readFully( channel, header, position ) ) break;
                header.flip();
                int length = header.getInt();
                if( length < 0 || position + RECORD_HEADER_LENGTH + length + CRC_LENGTH > size ) break;
                header.get();
                header.getShort();
                long time = header.getLong();
                if( records == 0 ) firstTime = time;
                records++;
                position += RECORD_HEADER_LENGTH + length + CRC_LENGTH;
            }
            segment.records = records;
            segment.firstTime = firstTime;
        }
    }

    private static boolean readFully( FileChannel channel, ByteBuffer buffer, long position ) throws IOException {
        long current = position;
        while( buffer.hasRemaining() ) {
            int read = channel.read( buffer, current );
            if( read < 0 ) return false;
            current += read;
        }
        return true;
    }

    Writer writer() throws IOException {
        return new Writer( this );
    }

    Reader reader() throws IOException {
        return new Reader( this );
    }

    synchronized void read() {
        outstanding++;
    }

    /**
     * @return true - the segment can be deleted ( the caller deletes it )
     */
    synchronized boolean ack() {
        outstanding--;
        acked++;
        return canDelete();
    }

    /**
     * @return true - the segment can be deleted ( the caller deletes it )
     */
    synchronized boolean readDone() {
        readDone = true;
        return canDelete();
    }

    private boolean canDelete() {
        if( readDone && outstanding == 0 && !deleted ) {
            deleted = true;
            return true;
        }
        return false;
    }

    /**
     * not yet delivered records ( the corrupted ones are counted until the segment is deleted )
     */
    synchronized int pending() {
        return deleted ? 0 : records - acked;
    }

    record Record( SpoolSegment segment, byte messageType, short version, long time, byte[] md5, byte[] data ) {}

    /**
     * not thread-safe, used under the stream write lock
     */
    static final class Writer implements Closeable {
        final SpoolSegment segment;
        private final FileChannel channel;
        private final ByteBuffer header = ByteBuffer.allocate( RECORD_HEADER_LENGTH );
        private final ByteBuffer crcBuffer = ByteBuffer.allocate( CRC_LENGTH );
        private final ByteBuffer[] buffers = new ByteBuffer[3];
        private final CRC32C crc = new CRC32C();
        long position;
        /**
         * appended after the last force
         */
        volatile boolean dirty;

        private Writer( SpoolSegment segment ) throws IOException {
            this.segment = segment;
            this.channel = FileChannel.open( segment.path, CREATE_NEW, WRITE );
            try {
                ByteBuffer fileHeader = ByteBuffer.allocate( HEADER_LENGTH ).putInt( MAGIC ).put( FORMAT_VERSION ).flip();
                while( fileHeader.hasRemaining() ) channel.write( fileHeader );
            } catch( IOException e ) {
                channel.close();
                throw e;
            }
            position = HEADER_LENGTH;
            segment.bytes = HEADER_LENGTH;
        }

        /**
         * one gathering write per record, the data is not copied; a failed write is truncated
         *
         * @return number of bytes written
         */
        int append( byte messageType, short version, long time, byte[] md5, byte[] data, int offset, int length ) throws IOException {
            header.clear();
            header.putInt( length ).put( messageType ).putShort( version ).putLong( time ).put( md5, 0, MessageProtocol.MD5_LENGTH ).flip();

            crc.reset();
            crc.update( header.array(), 4, RECORD_HEADER_LENGTH - 4 );
            crc.update( data, offset, length );
            crcBuffer.clear();
            crcBuffer.putInt( ( int ) crc.getValue() ).flip();

            buffers[0] = header;
            buffers[1] = ByteBuffer.wrap( data, offset, length );
            buffers[2] = crcBuffer;

            int total = RECORD_HEADER_LENGTH + length + CRC_LENGTH;
            long start = position;
            try {
                long written = 0;
                while( written < total ) written += channel.write( buffers );
            } catch( IOException e ) {
                try {
                    channel.truncate( start );
                } catch( IOException truncateError ) {
                    e.addSuppressed( truncateError );
                }
                throw e;
            } finally {
                buffers[1] = null;
            }

            position += total;
            dirty = true;
            if( segment.records == 0 ) segment.firstTime = time;
            segment.records++;
            segment.bytes = position;

            return total;
        }

        void force() throws IOException {
            dirty = false;
            channel.force( false );
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }

    static final class Reader implements Closeable {
        private final SpoolSegment segment;
        private final DataInputStream in;
        private final CRC32C crc = new CRC32C();
        private final byte[] header = new byte[RECORD_HEADER_LENGTH];
        private long remaining;
        /**
         * the segment ended with an invalid record ( torn tail / corruption )
         */
        boolean corrupted;

        private Reader( SpoolSegment segment ) throws IOException {
            this.segment = segment;
            long size = Files.size( segment.path );
            this.in = new DataInputStream( new BufferedInputStream( Files.newInputStream( segment.path, READ ), 64 * 1024 ) );
            try {
                if( size < HEADER_LENGTH || in.readInt() != MAGIC || in.readByte() != FORMAT_VERSION ) {
                    corrupted = true;
                    remaining = 0;
                } else {
                    remaining = size - HEADER_LENGTH;
                }
            } catch( IOException e ) {
                in.close();
                throw e;
            }
        }

        /**
         * @return null - the end of the segment or the first invalid record ( {@link #corrupted} )
         */
        Record next() throws IOException {
            if( remaining == 0 ) return null;
            if( remaining < RECORD_HEADER_LENGTH + CRC_LENGTH ) return corrupted();

            try {
                in.readFully( header );
                ByteBuffer h = ByteBuffer.wrap( header );
                int length = h.getInt();
                if( length < 0 || length > remaining - RECORD_HEADER_LENGTH - CRC_LENGTH ) return corrupted();
                byte messageType = h.get();
                short version = h.getShort();
                long time = h.getLong();
                byte[] md5 = new byte[MessageProtocol.MD5_LENGTH];
                h.get( md5 );

                byte[] data = new byte[length];
                in.readFully( data );
                int expected = in.readInt();

                crc.reset();
                crc.update( header, 4, RECORD_HEADER_LENGTH - 4 );
                crc.update( data );
                if( ( int ) crc.getValue() != expected ) return corrupted();

                remaining -= RECORD_HEADER_LENGTH + length + CRC_LENGTH;
                return new Record( segment, messageType, version, time, md5, data );
            } catch( EOFException e ) {
                return corrupted();
            }
        }

        SpoolSegment segment() {
            return segment;
        }

        private Record corrupted() {
            corrupted = true;
            remaining = 0;
            return null;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
