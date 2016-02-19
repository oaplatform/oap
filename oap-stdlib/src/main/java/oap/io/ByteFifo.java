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

package oap.io;

import sun.misc.Unsafe;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ByteFifo implements Closeable {
    static final int HEAD_POSITION = 0;
    static final int TAIL1_POSITION = HEAD_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    static final int TAIL2_POSITION = TAIL1_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    static final int BODY_POSITION = TAIL2_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final int length;
    private final Object readLock = new Object();
    volatile int head;
    volatile int tail1;
    volatile int tail2;
    private MappedByteBuffer writeBuffer;
    private MappedByteBuffer readBuffer;

    private ByteFifo( Path segment ) {
        try {
            file = new RandomAccessFile( segment.toFile(), "rw" );
            channel = file.getChannel();
            length = (int) file.length();
            writeBuffer = channel.map( FileChannel.MapMode.READ_WRITE, 0, length );
            readBuffer = channel.map( FileChannel.MapMode.READ_ONLY, 0, length );

            head = readBuffer.getInt();
            tail1 = readBuffer.getInt();
            tail2 = readBuffer.getInt();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static ByteFifo open( Path path, int size ) {
        Files.ensureFile( path );
        try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw" ) ) {
            if( file.length() < BODY_POSITION ) {
                final MappedByteBuffer buffer = file.getChannel().map( FileChannel.MapMode.READ_WRITE, 0, size );
                buffer.putInt( HEAD_POSITION, BODY_POSITION );
                buffer.putInt( TAIL1_POSITION, BODY_POSITION );
                buffer.putInt( TAIL2_POSITION, BODY_POSITION );
                Closeables.close( buffer );
            }
            return new ByteFifo( path );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public int used() {
        return (tail2 - head) + (tail1 - BODY_POSITION);
    }

    public int free() {
        return length - used();
    }

    static int sizeOf( byte[] bytes ) {
        return Unsafe.ARRAY_INT_INDEX_SCALE + bytes.length;
    }

    public final synchronized boolean offer( byte[] bytes ) {
        final int size = sizeOf( bytes );
        if( length - tail2 < size ) {
            if( head - tail1 < size ) {
                return false;
            } else {
                writeBuffer.position( tail1 );
                writeBuffer.putInt( bytes.length );
                writeBuffer.put( bytes );
                tail1 = writeBuffer.position();
                writeBuffer.putInt( TAIL1_POSITION, tail1 );
            }
        } else {
            writeBuffer.position( tail2 );
            writeBuffer.putInt( bytes.length );
            writeBuffer.put( bytes );
            tail2 = writeBuffer.position();
            writeBuffer.putInt( TAIL2_POSITION, tail2 );
        }

        return true;
    }

    public synchronized void close() {
        synchronized( readLock ) {
            Closeables.close( channel );
            Closeables.close( file );
            if( readBuffer != null ) Closeables.close( readBuffer );
            readBuffer = null;
            if( writeBuffer != null ) Closeables.close( writeBuffer );
            writeBuffer = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if( readBuffer != null || writeBuffer != null ) close();
        super.finalize();
    }

    public boolean isEmpty() {
        return used() == 0;
    }

    public byte[] poll() {
        AtomicReference<byte[]> result = new AtomicReference<>();
        polling( result::set );
        return result.get();
    }

    public void polling( Consumer<byte[]> consumer ) {
        polling( Integer.MAX_VALUE, consumer );
    }

    public void polling( int count, Consumer<byte[]> consumer ) {
        synchronized( readLock ) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                count = readEntries( count, bytes, head, tail2 );

                if( count > 0 && tail1 > BODY_POSITION && readBuffer.position() > tail1 )
                    readEntries( count, bytes, BODY_POSITION, tail1 );

                bytes.close();

                consumer.accept( bytes.toByteArray() );

                if( ByteFifo.this.tail1 > BODY_POSITION
                    && readBuffer.position() < ByteFifo.this.tail1 ) {
                    ByteFifo.this.head = readBuffer.position();
                    ByteFifo.this.tail2 = ByteFifo.this.tail1;
                    ByteFifo.this.tail1 = BODY_POSITION;
                } else if( readBuffer.position() == ByteFifo.this.tail1 ) {
                    ByteFifo.this.tail1 = BODY_POSITION;
                    ByteFifo.this.tail2 = BODY_POSITION;
                    ByteFifo.this.head = BODY_POSITION;
                } else if( readBuffer.position() == ByteFifo.this.tail2 ) {
                    ByteFifo.this.tail2 = ByteFifo.this.tail1;
                    ByteFifo.this.tail1 = BODY_POSITION;
                    ByteFifo.this.head = BODY_POSITION;
                } else ByteFifo.this.head = readBuffer.position();
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }

        }
    }

    private int readEntries( int count, ByteArrayOutputStream bytes, int head, int tail ) throws IOException {
        synchronized( readLock ) {
            readBuffer.position( head );

            while( readBuffer.position() < tail && count-- > 0 ) {
                final byte[] data = new byte[readBuffer.getInt()];
                readBuffer.get( data );
                bytes.write( data );
            }
            return count;
        }
    }
}
