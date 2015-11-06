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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Fifo implements Closeable {
    static final int HEAD_POSITION = 0;
    static final int TAIL1_POSITION = HEAD_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    static final int TAIL2_POSITION = TAIL1_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    static final int BODY_POSITION = TAIL2_POSITION + Unsafe.ARRAY_INT_INDEX_SCALE;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final long length;
    private final Object poolLock = new Object();
    volatile int head;
    volatile int tail1;
    volatile int tail2;
    private MappedByteBuffer writeBuffer;
    private MappedByteBuffer readBuffer;

    private Fifo( Path segment ) {
        try {
            file = new RandomAccessFile( segment.toFile(), "rw" );
            channel = file.getChannel();
            length = file.length();
            writeBuffer = channel.map( FileChannel.MapMode.READ_WRITE, 0, length );
            readBuffer = channel.map( FileChannel.MapMode.READ_ONLY, 0, length );

            head = readBuffer.getInt();
            tail1 = readBuffer.getInt();
            tail2 = readBuffer.getInt();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static Fifo newSegment( Path segment, long size ) {
        segment.getParent().toFile().mkdirs();
        try( RandomAccessFile file = new RandomAccessFile( segment.toFile(), "rw" ) ) {
            if( file.length() < BODY_POSITION ) {
                final MappedByteBuffer buffer = file.getChannel().map( FileChannel.MapMode.READ_WRITE, 0, size );
                buffer.putInt( HEAD_POSITION, BODY_POSITION );
                buffer.putInt( TAIL1_POSITION, BODY_POSITION );
                buffer.putInt( TAIL2_POSITION, BODY_POSITION );

                Closeables.close( buffer );
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }

        return new Fifo( segment );
    }

    public int used() {
        return (tail2 - head) + (tail1 - BODY_POSITION);
    }

    public int free() {
        return (int) (length - used());
    }

    protected static int sizeOf( byte[] bytes ) {
        return Unsafe.ARRAY_INT_INDEX_SCALE + bytes.length;
    }

    public final boolean add( byte[] bytes ) {
        final int size = sizeOf( bytes );

        synchronized( this ) {
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
    }

    public void close() throws IOException {
        synchronized( poolLock ) {
            synchronized( this ) {
                Closeables.close( channel );
                Closeables.close( file );
                if( readBuffer != null ) Closeables.close( readBuffer );
                readBuffer = null;
                if( writeBuffer != null ) Closeables.close( writeBuffer );
                writeBuffer = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if( readBuffer != null || writeBuffer != null ) {
            close();
        }

        super.finalize();
    }

    public boolean isEmpty() {
        return used() == 0;
    }

    public Entries peek() {
        return peek( Integer.MAX_VALUE );
    }

    public Entries peek( int count ) {
        final ArrayList<byte[]> result = new ArrayList<>();

        int headPosition = peek( head, tail2, count, result );

        return new Entries( headPosition, result );
    }

    private int peek( int headPosition, int tailPosition, int count, ArrayList<byte[]> result ) {
        synchronized( poolLock ) {
            if( headPosition == tailPosition ) return this.head;

            readBuffer.position( headPosition );

            while( readBuffer.position() < tailPosition && result.size() < count ) {
                final byte[] data = new byte[readBuffer.getInt()];
                readBuffer.get( data );
                result.add( data );
            }

            int currentPosition = readBuffer.position();

            if( result.size() < count && tail1 > BODY_POSITION && currentPosition > tail1 )
                peek( BODY_POSITION, tail1, count - result.size(), result );

            return readBuffer.position();
        }
    }

    public final class Entries extends ArrayList<byte[]> {

        private int tail;
        private boolean committed = false;

        public Entries( int tail, List<byte[]> entries ) {
            super( entries );
            this.tail = tail;
        }

        public Entries commit() {
            synchronized( Fifo.this ) {
                if( committed ) throw new IllegalStateException( "datalog already committed" );

                committed = true;

                if( isEmpty() ) return this;

                if( Fifo.this.tail1 > BODY_POSITION && tail < Fifo.this.tail1 ) {
                    Fifo.this.head = tail;
                    Fifo.this.tail2 = Fifo.this.tail1;
                    Fifo.this.tail1 = BODY_POSITION;
                } else if( this.tail == Fifo.this.tail1 ) {
                    Fifo.this.tail1 = Fifo.this.tail2 = Fifo.this.head = BODY_POSITION;
                } else if( this.tail == Fifo.this.tail2 ) {
                    Fifo.this.tail2 = Fifo.this.tail1;
                    Fifo.this.tail1 = Fifo.this.head = BODY_POSITION;
                } else {
                    Fifo.this.head = tail;
                }

                final ByteArrayOutputStream bytes = new ByteArrayOutputStream( 3 * 4 );
                try( DataOutputStream out = new DataOutputStream( bytes ) ) {
                    out.writeInt( Fifo.this.head );
                    out.writeInt( Fifo.this.tail1 );
                    out.writeInt( Fifo.this.tail2 );
                } catch( IOException ignore ) {

                }
            }

            return this;
        }
    }
}
