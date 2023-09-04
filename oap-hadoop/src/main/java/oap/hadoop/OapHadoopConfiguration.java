package oap.hadoop;

import com.google.common.base.Preconditions;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

@Slf4j
@ToString
public class OapHadoopConfiguration extends Configuration {
    private final OapFileSystemType fileSystemType;

    public OapHadoopConfiguration( OapFileSystemType fileSystemType, Map<String, String> configuration ) {
        super( false );
        this.fileSystemType = fileSystemType;

        log.info( "hadoop filesystem {} conf {}", fileSystemType, configuration );

        Preconditions.checkArgument( !configuration.containsKey( "fs.defaultFS" ) );

        configuration.forEach( this::set );
        set( "fs.defaultFS", fileSystemType.fsDefaultFS );
    }

    public InputStream getInputStream( String path, boolean decode ) throws UncheckedIOException {
        try {
            FileSystem fileSystem = getFileSystem();

            org.apache.hadoop.fs.Path hadoopPath = getPath( path );

            InputStream rawStream = fileSystem.open( hadoopPath );
            return decode
                ? IoStreams.in( rawStream, IoStreams.Encoding.from( path ) )
                : rawStream;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public Path getPath( String path ) {
        return new Path( fileSystemType.root( this ), path );
    }

    public FileSystem getFileSystem() throws UncheckedIOException {
        try {
            return FileSystem.get( this );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
