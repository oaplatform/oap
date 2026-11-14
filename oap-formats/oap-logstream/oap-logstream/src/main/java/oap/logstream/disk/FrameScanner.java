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

package oap.logstream.disk;

import com.github.luben.zstd.ZstdInputStream;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import oap.logstream.LogStreamCompression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

/**
 * Walks the frames of a logstream data file (gzip members, LZ4 frames, zstd frames) from the beginning and
 * finds the end of the last complete, valid frame. Used by the crash recovery: everything after it is a torn write.
 * <p>
 * A frame is valid when its structure is complete and it decompresses. The content checksum (gzip CRC32,
 * LZ4 / zstd content checksum) is verified when the frame has one; LZ4 / zstd frames without a checksum
 * (files written before checksums were enabled, not yet updated clients) are accepted on structure and decompression alone.
 * <p>
 * Skippable frames (magic 0x184D2A50..0x184D2A5F, LZ4 and zstd) are accepted as is.
 */
@Slf4j
final class FrameScanner {
    private static final int GZIP_ID1 = 0x1f;
    private static final int GZIP_ID2 = 0x8b;
    private static final int GZIP_CM_DEFLATE = 8;
    private static final int GZIP_FHCRC = 0x02;
    private static final int GZIP_FEXTRA = 0x04;
    private static final int GZIP_FNAME = 0x08;
    private static final int GZIP_FCOMMENT = 0x10;
    private static final int GZIP_RESERVED = 0xE0;

    private static final int LZ4_MAGIC = 0x184D2204;
    private static final int ZSTD_MAGIC = 0xFD2FB528;
    private static final int SKIPPABLE_MAGIC = 0x184D2A50;
    private static final int SKIPPABLE_MAGIC_MASK = 0xFFFFFFF0;

    private static final int CHUNK = 64 * 1024;
    private static final XXHash32 XXHASH32 = XXHashFactory.fastestInstance().hash32();

    private final FileChannel channel;
    private final long size;
    private final byte[] small = new byte[16];
    private final byte[] chunk = new byte[CHUNK];
    private final byte[] sink = new byte[CHUNK];

    private FrameScanner( FileChannel channel ) throws IOException {
        this.channel = channel;
        this.size = channel.size();
    }

    /**
     * @return frame ends in ascending order; {@link Scan#validEnd()} is the end of the last valid frame (0 - none)
     */
    static Scan scan( FileChannel channel, LogStreamCompression compression ) throws IOException {
        FrameScanner scanner = new FrameScanner( channel );

        long[] ends = new long[16];
        int count = 0;
        long position = 0;
        while( position < scanner.size ) {
            long end = scanner.frameEnd( compression, position );
            if( end <= position ) break;

            if( count == ends.length ) ends = Arrays.copyOf( ends, count * 2 );
            ends[count++] = end;
            position = end;
        }

        return new Scan( Arrays.copyOf( ends, count ), position, scanner.size );
    }

    private long frameEnd( LogStreamCompression compression, long position ) {
        try {
            return switch( compression ) {
                case GZIP -> gzipMemberEnd( position );
                case LZ4 -> lz4FrameEnd( position );
                case ZSTD -> zstdFrameEnd( position );
            };
        } catch( Exception e ) {
            log.trace( "invalid {} frame at {}: {}", compression, position, e.getMessage() );
            return -1;
        }
    }

    /**
     * reads exactly length bytes at position into small
     *
     * @return false - beyond the end of the file
     */
    private boolean read( long position, int length ) throws IOException {
        if( position < 0 || position + length > size ) return false;
        ByteBuffer buffer = ByteBuffer.wrap( small, 0, length );
        long pos = position;
        while( buffer.hasRemaining() ) {
            int n = channel.read( buffer, pos );
            if( n < 0 ) return false;
            pos += n;
        }
        return true;
    }

    private byte[] readFrame( long position, long end ) throws IOException {
        long length = end - position;
        if( length > Integer.MAX_VALUE - 8 ) throw new IOException( "frame is too large: " + length );
        byte[] frame = new byte[( int ) length];
        ByteBuffer buffer = ByteBuffer.wrap( frame );
        long pos = position;
        while( buffer.hasRemaining() ) {
            int n = channel.read( buffer, pos );
            if( n < 0 ) throw new IOException( "unexpected end of file" );
            pos += n;
        }
        return frame;
    }

    private int u8( int index ) {
        return small[index] & 0xFF;
    }

    private int intLE( int index ) {
        return u8( index ) | ( u8( index + 1 ) << 8 ) | ( u8( index + 2 ) << 16 ) | ( u8( index + 3 ) << 24 );
    }

    private boolean isSkippable( int magic ) {
        return ( magic & SKIPPABLE_MAGIC_MASK ) == SKIPPABLE_MAGIC;
    }

    private long skippableEnd( long position ) throws IOException {
        if( !read( position + 4, 4 ) ) return -1;
        long end = position + 8 + Integer.toUnsignedLong( intLE( 0 ) );
        return end <= size ? end : -1;
    }

    private void drain( InputStream in ) throws IOException {
        int n;
        do {
            n = in.read( sink );
        } while( n >= 0 );
    }

    /**
     * RFC 1952: header, deflate stream, CRC32 + ISIZE (verified)
     */
    private long gzipMemberEnd( long position ) throws Exception {
        if( !read( position, 10 ) ) return -1;
        if( u8( 0 ) != GZIP_ID1 || u8( 1 ) != GZIP_ID2 || u8( 2 ) != GZIP_CM_DEFLATE ) return -1;
        int flags = u8( 3 );
        if( ( flags & GZIP_RESERVED ) != 0 ) return -1;

        long pos = position + 10;
        if( ( flags & GZIP_FEXTRA ) != 0 ) {
            if( !read( pos, 2 ) ) return -1;
            pos += 2 + ( u8( 0 ) | ( u8( 1 ) << 8 ) );
        }
        if( ( flags & GZIP_FNAME ) != 0 ) pos = zeroTerminatedEnd( pos );
        if( pos >= 0 && ( flags & GZIP_FCOMMENT ) != 0 ) pos = zeroTerminatedEnd( pos );
        if( pos < 0 ) return -1;
        if( ( flags & GZIP_FHCRC ) != 0 ) pos += 2;
        if( pos >= size ) return -1;

        CRC32 crc32 = new CRC32();
        Inflater inflater = new Inflater( true );
        try {
            long fed = pos;
            while( !inflater.finished() ) {
                if( inflater.needsInput() ) {
                    if( fed >= size ) return -1;
                    int length = ( int ) Math.min( CHUNK, size - fed );
                    ByteBuffer buffer = ByteBuffer.wrap( chunk, 0, length );
                    while( buffer.hasRemaining() ) {
                        if( channel.read( buffer, fed + buffer.position() ) < 0 ) return -1;
                    }
                    inflater.setInput( chunk, 0, length );
                    fed += length;
                } else if( inflater.needsDictionary() ) {
                    return -1;
                }

                int n = inflater.inflate( sink );
                crc32.update( sink, 0, n );
            }

            long deflateEnd = fed - inflater.getRemaining();
            if( !read( deflateEnd, 8 ) ) return -1;
            if( intLE( 0 ) != ( int ) crc32.getValue() ) return -1;
            if( intLE( 4 ) != ( int ) inflater.getBytesWritten() ) return -1;

            return deflateEnd + 8;
        } finally {
            inflater.end();
        }
    }

    private long zeroTerminatedEnd( long position ) throws IOException {
        long pos = position;
        while( pos < size ) {
            int length = ( int ) Math.min( small.length, size - pos );
            if( !read( pos, length ) ) return -1;
            for( int i = 0; i < length; i++ ) {
                if( small[i] == 0 ) return pos + i + 1;
            }
            pos += length;
        }
        return -1;
    }

    /**
     * LZ4 frame format 1.6: magic, FLG, BD, [content size], [dict id], HC, blocks, end mark, [content checksum]
     */
    private long lz4FrameEnd( long position ) throws IOException {
        if( !read( position, 4 ) ) return -1;
        int magic = intLE( 0 );
        if( isSkippable( magic ) ) return skippableEnd( position );
        if( magic != LZ4_MAGIC ) return -1;

        if( !read( position + 4, 2 ) ) return -1;
        int flg = u8( 0 );
        if( ( flg >>> 6 ) != 1 || ( flg & 0x02 ) != 0 ) return -1;
        boolean blockChecksum = ( flg & 0x10 ) != 0;
        boolean contentSize = ( flg & 0x08 ) != 0;
        boolean contentChecksum = ( flg & 0x04 ) != 0;
        boolean dictId = ( flg & 0x01 ) != 0;

        int descriptorLength = 2 + ( contentSize ? 8 : 0 ) + ( dictId ? 4 : 0 );
        if( !read( position + 4, descriptorLength + 1 ) ) return -1;
        int hc = ( XXHASH32.hash( small, 0, descriptorLength, 0 ) >>> 8 ) & 0xFF;
        if( u8( descriptorLength ) != hc ) return -1;

        long pos = position + 4 + descriptorLength + 1;
        while( true ) {
            if( !read( pos, 4 ) ) return -1;
            int blockSize = intLE( 0 );
            pos += 4;
            if( blockSize == 0 ) break;
            pos += ( blockSize & 0x7FFFFFFF ) + ( blockChecksum ? 4 : 0 );
            if( pos > size ) return -1;
        }
        if( contentChecksum ) pos += 4;
        if( pos > size ) return -1;

        try( LZ4FrameInputStream in = new LZ4FrameInputStream( new ByteArrayInputStream( readFrame( position, pos ) ) ) ) {
            drain( in );
        }
        return pos;
    }

    /**
     * RFC 8878: magic, frame header, blocks, [content checksum]
     */
    private long zstdFrameEnd( long position ) throws IOException {
        if( !read( position, 5 ) ) return -1;
        int magic = intLE( 0 );
        if( isSkippable( magic ) ) return skippableEnd( position );
        if( magic != ZSTD_MAGIC ) return -1;

        int fhd = u8( 4 );
        int fcsFlag = fhd >>> 6;
        boolean singleSegment = ( fhd & 0x20 ) != 0;
        if( ( fhd & 0x08 ) != 0 ) return -1;
        boolean contentChecksum = ( fhd & 0x04 ) != 0;
        int dictIdLength = switch( fhd & 0x03 ) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            default -> 4;
        };
        int fcsLength = switch( fcsFlag ) {
            case 0 -> singleSegment ? 1 : 0;
            case 1 -> 2;
            case 2 -> 4;
            default -> 8;
        };

        long pos = position + 5 + ( singleSegment ? 0 : 1 ) + dictIdLength + fcsLength;
        while( true ) {
            if( !read( pos, 3 ) ) return -1;
            int header = u8( 0 ) | ( u8( 1 ) << 8 ) | ( u8( 2 ) << 16 );
            boolean last = ( header & 1 ) != 0;
            int type = ( header >>> 1 ) & 0x03;
            int blockSize = header >>> 3;
            if( type == 3 ) return -1;
            pos += 3 + ( type == 1 ? 1 : blockSize );
            if( pos > size ) return -1;
            if( last ) break;
        }
        if( contentChecksum ) pos += 4;
        if( pos > size ) return -1;

        try( ZstdInputStream in = new ZstdInputStream( new ByteArrayInputStream( readFrame( position, pos ) ) ) ) {
            drain( in );
        }
        return pos;
    }

    /**
     * @param ends     frame ends, ascending
     * @param validEnd end of the last valid frame, 0 - no valid frames
     * @param size     file size
     */
    record Scan( long[] ends, long validEnd, long size ) {
        boolean complete() {
            return validEnd == size;
        }

        /**
         * 0 or the end of a valid frame
         */
        boolean isBoundary( long position ) {
            return position == 0 || Arrays.binarySearch( ends, position ) >= 0;
        }

        /**
         * number of valid frames that end at or before position
         */
        int framesUpTo( long position ) {
            int index = Arrays.binarySearch( ends, position );
            return index >= 0 ? index + 1 : -index - 1;
        }
    }
}
