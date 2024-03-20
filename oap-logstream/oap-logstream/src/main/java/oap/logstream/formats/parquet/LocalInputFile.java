package oap.logstream.formats.parquet;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * <a href="https://github.com/GeoscienceAustralia/wit_tooling/blob/main/examples/java/parquet-reader/src/main/java/LocalInputFile.java">from</a>
 */
public class LocalInputFile implements InputFile {
    private static final int COPY_BUFFER_SIZE = 8192;

    private final RandomAccessFile raf;

    public LocalInputFile( Path path ) throws FileNotFoundException {
        this( path.toFile() );
    }

    public LocalInputFile( File file ) throws FileNotFoundException {
        raf = new RandomAccessFile( file, "r" );
    }

    private static int readDirectBuffer( ByteBuffer byteBuffer, byte[] tmpBuf, ByteBufReader rdr )
        throws IOException {
        // copy all the bytes that return immediately, stopping at the first
        // read that doesn't return a full buffer.
        int nextReadLength = Math.min( byteBuffer.remaining(), tmpBuf.length );
        int totalBytesRead = 0;
        int bytesRead;

        while( ( bytesRead = rdr.read( tmpBuf, 0, nextReadLength ) ) == tmpBuf.length ) {
            byteBuffer.put( tmpBuf );
            totalBytesRead += bytesRead;
            nextReadLength = Math.min( byteBuffer.remaining(), tmpBuf.length );
        }

        if( bytesRead < 0 ) {
            // return -1 if nothing was read
            return totalBytesRead == 0 ? -1 : totalBytesRead;
        } else {
            // copy the last partial buffer
            byteBuffer.put( tmpBuf, 0, bytesRead );
            totalBytesRead += bytesRead;
            return totalBytesRead;
        }
    }

    private static void readFullyDirectBuffer( ByteBuffer byteBuffer, byte[] tmpBuf, ByteBufReader rdr )
        throws IOException {
        int nextReadLength = Math.min( byteBuffer.remaining(), tmpBuf.length );
        int bytesRead = 0;

        while( nextReadLength > 0 && ( bytesRead = rdr.read( tmpBuf, 0, nextReadLength ) ) >= 0 ) {
            byteBuffer.put( tmpBuf, 0, bytesRead );
            nextReadLength = Math.min( byteBuffer.remaining(), tmpBuf.length );
        }

        if( bytesRead < 0 && byteBuffer.remaining() > 0 ) {
            throw new EOFException(
                "Reached the end of stream with " + byteBuffer.remaining() + " bytes left to read" );
        }
    }

    @Override
    public long getLength() throws IOException {
        return raf.length();
    }

    @Override
    public SeekableInputStream newStream() {
        return new SeekableInputStream() {
            private final byte[] tmpBuf = new byte[COPY_BUFFER_SIZE];
            private long markPos = 0;

            @Override
            public long getPos() throws IOException {
                return raf.getFilePointer();
            }

            @Override
            public void seek( long l ) throws IOException {
                raf.seek( l );
            }

            @Override
            public void readFully( byte[] bytes ) throws IOException {
                raf.readFully( bytes );
            }

            @Override
            public void readFully( byte[] bytes, int i, int i1 ) throws IOException {
                raf.readFully( bytes, i, i1 );
            }

            @Override
            public void readFully( ByteBuffer byteBuffer ) throws IOException {
                readFullyDirectBuffer( byteBuffer, tmpBuf, raf::read );
            }

            @Override
            public int read( ByteBuffer byteBuffer ) throws IOException {
                return readDirectBuffer( byteBuffer, tmpBuf, raf::read );
            }

            @Override
            public int read() throws IOException {
                return raf.read();
            }

            @Override
            public int read( byte[] b ) throws IOException {
                return raf.read( b );
            }

            @Override
            public int read( byte[] b, int off, int len ) throws IOException {
                return raf.read( b, off, len );
            }

            @SuppressWarnings( "checkstyle:ParameterAssignment" )
            @Override
            public long skip( long n ) throws IOException {
                final long savPos = raf.getFilePointer();
                final long amtLeft = raf.length() - savPos;
                n = Math.min( n, amtLeft );
                final long newPos = savPos + n;
                raf.seek( newPos );
                final long curPos = raf.getFilePointer();
                return curPos - savPos;
            }

            @Override
            public int available() {
                return 0;
            }

            @Override
            public void close() throws IOException {
                raf.close();
            }

            @SuppressWarnings( { "unchecked", "unused", "UnusedReturnValue" } )
            private <T extends Throwable, R> R uncheckedExceptionThrow( Throwable t ) throws T {
                throw ( T ) t;
            }

            @Override
            public synchronized void mark( int readlimit ) {
                try {
                    markPos = raf.getFilePointer();
                } catch( IOException e ) {
                    uncheckedExceptionThrow( e );
                }
            }

            @Override
            public synchronized void reset() throws IOException {
                raf.seek( markPos );
            }

            @Override
            public boolean markSupported() {
                return true;
            }
        };
    }

    private interface ByteBufReader {
        int read( byte[] b, int off, int len ) throws IOException;
    }
}
