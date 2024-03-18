package oap.logstream.formats.parquet;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class LocalInputFile implements InputFile, Closeable {
    private final RandomAccessFile raf;

    public LocalInputFile( File file ) throws FileNotFoundException {
        raf = new RandomAccessFile( file, "r" );
    }

    @Override
    public long getLength() throws IOException {
        return raf.length();
    }

    @Override
    public SeekableInputStream newStream() throws IOException {
        return new SeekableInputStream() {
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
                raf.readFully( byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining() );
            }

            @Override
            public int read( ByteBuffer byteBuffer ) throws IOException {
                return raf.read( byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining() );
            }

            @Override
            public int read() throws IOException {
                return raf.read();
            }
        };
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
