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
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ToString
public class OapHadoopConfiguration extends Configuration {
    private final OapFileSystemType fileSystemType;

    public OapHadoopConfiguration( OapFileSystemType fileSystemType, Map<String, Object> configuration ) {
        super( false );
        this.fileSystemType = fileSystemType;

        log.info( "hadoop filesystem {} conf {}", fileSystemType, configuration );

        Preconditions.checkArgument( !configuration.containsKey( "fs.defaultFS" ) );

        // No way to declare key/values (containing dots) sharing a part of the "path"
        // https://github.com/lightbend/config/issues/493
        Map<String, String> configurationFixed = fixMap( configuration );

        configurationFixed.forEach( this::set );
        set( "fs.defaultFS", fileSystemType.fsDefaultFS );
    }

    private Map<String, String> fixMap( Map<String, Object> configuration ) {
        var result = new LinkedHashMap<String, String>();

        fixMap( configuration, "", result );

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private void fixMap( Map<String, Object> configuration, String prefixKey, LinkedHashMap<String, String> result ) {
        configuration.forEach( ( k, v ) -> {
            String key = concat( prefixKey, k );

            if( v instanceof Map ) {
                fixMap( ( Map<String, Object> ) v, key, result );
            } else {
                result.put( key, String.valueOf( v ) );
            }
        } );
    }

    private String concat( String left, String right ) {
        if( left.isEmpty() ) return right;

        return left + '.' + right;
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

    public Path getPath( java.nio.file.Path path ) {
        // windows: hadoop.Path.toString() != hadoop.Path.toUri().toString()
        return getPath( path.toUri().toString() );
    }

    public FileSystem getFileSystem() throws UncheckedIOException {
        try {
            return FileSystem.get( this );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
