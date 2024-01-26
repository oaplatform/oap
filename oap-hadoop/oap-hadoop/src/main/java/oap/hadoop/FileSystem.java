package oap.hadoop;

import lombok.extern.slf4j.Slf4j;
import oap.util.Throwables;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

@Slf4j
public class FileSystem {
    private final OapHadoopConfiguration configuration;

    public FileSystem( OapHadoopConfiguration configuration ) {
        this.configuration = configuration;
    }

    @Nullable
    public InputStream getInputStream( Path path ) throws UncheckedIOException {
        log.trace( "getInputStream {}", path );
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );
            return fileSystem.open( path );
        } catch( FileNotFoundException e ) {
            log.trace( e.getMessage() );
            return null;
        } catch( IOException e ) {
            throw Throwables.propagate( e );
        }
    }

    public OutputStream getOutputStream( Path path, boolean overwrite ) throws UncheckedIOException {
        log.trace( "getOutputStream {} overwrite {}", path, overwrite );
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            return fileSystem.create( path, overwrite );
        } catch( IOException e ) {
            throw Throwables.propagate( e );
        }
    }

    public boolean exists( Path path ) throws UncheckedIOException {
        log.trace( "exists {}", path );
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            return fileSystem.exists( path );
        } catch( IOException e ) {
            throw Throwables.propagate( e );
        }
    }

    public void copy( java.nio.file.Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        copy( new Path( src.toUri() ), dst, overwrite );
    }

    public void copy( Path src, java.nio.file.Path dst, boolean overwrite ) throws UncheckedIOException {
        copy( src, new Path( dst.toUri() ), overwrite );
    }

    public void copy( Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        log.trace( "copy src {} dst {} overwrite {}", src, dst, overwrite );

        try {
            FileUtil.copy( src.getFileSystem( configuration ), src, dst.getFileSystem( configuration ), dst, false, overwrite, configuration );
        } catch( IOException e ) {
            throw Throwables.propagate( e );
        }
    }

    public void delete( Path path ) throws UncheckedIOException {
        log.trace( "delete {}", path );
        try {
            org.apache.hadoop.fs.FileSystem fileSystem = path.getFileSystem( configuration );

            fileSystem.delete( path, true );
        } catch( IOException e ) {
            throw Throwables.propagate( e );
        }
    }

    public void move( Path src, Path dst, boolean overwrite ) throws UncheckedIOException {
        log.trace( "move src {} dst {} overwrite {}", src, dst, overwrite );
        copy( src, dst, overwrite );
        delete( src );
    }
}
