package oap.hadoop;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public class FileSystem {
    private final OapHadoopConfiguration configuration;

    public FileSystem( OapHadoopConfiguration configuration ) {
        this.configuration = configuration;
    }

    public InputStream getInputStream( Path path ) throws UncheckedIOException {
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );
            return fileSystem.open( path );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public OutputStream getOutputStream( Path path, boolean overwrite ) throws UncheckedIOException {
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            return fileSystem.create( path, overwrite );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public boolean exists( Path path ) throws UncheckedIOException {
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            return fileSystem.exists( path );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void copy( java.nio.file.Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        copy( new Path( src.toUri() ), dst, overwrite );
    }

    public void copy( Path src, java.nio.file.Path dst, boolean overwrite ) throws UncheckedIOException {
        copy( src, new Path( dst.toUri() ), overwrite );
    }

    public void copy( Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        try {
            FileUtil.copy( src.getFileSystem( configuration ), src, dst.getFileSystem( configuration ), dst, false, overwrite, configuration );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void delete( Path path ) throws UncheckedIOException {
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            fileSystem.delete( path, true );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void move( Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        copy( src, dst, overwrite );
        delete( src );
    }
}
