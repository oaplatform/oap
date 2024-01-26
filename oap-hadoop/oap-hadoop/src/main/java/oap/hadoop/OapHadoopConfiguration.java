package oap.hadoop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ToString
public class OapHadoopConfiguration extends Configuration {
    public OapFileSystemType fileSystemType;

    @JsonCreator
    public OapHadoopConfiguration( @JsonProperty OapFileSystemType fileSystemType,
                                   @JsonProperty Map<String, Object> configuration ) {
        super( false );
        this.fileSystemType = fileSystemType;

        log.info( "hadoop filesystem {} conf {}", fileSystemType, configuration );

        Preconditions.checkArgument( !configuration.containsKey( "fs.defaultFS" ) );

        // No way to declare key/values (containing dots) sharing a part of the "path"
        // https://github.com/lightbend/config/issues/493
        Map<String, String> configurationFixed = fixMap( configuration );

        configurationFixed.forEach( this::set );

        set( "fs.sftp.impl", "org.apache.hadoop.fs.sftp.SFTPFileSystem" );
        set( "fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem" );
        set( "fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem" );
        set( "fs.s3a.path.style.access", "true" );
        set( "hadoop.tmp.dir", "/tmp" );

        log.info( "filesystem {} fs.defaultFS {}", fileSystemType.name(), this.get( "fs.defaultFS" ) );
    }

    private Map<String, String> fixMap( Map<String, Object> configuration ) {
        var result = new LinkedHashMap<String, String>();

        fixMap( configuration, "", result );

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private static void fixMap( Map<String, Object> configuration, String prefixKey, LinkedHashMap<String, String> result ) {
        configuration.forEach( ( k, v ) -> {
            String key = concat( prefixKey, k );

            if( v instanceof Map ) {
                fixMap( ( Map<String, Object> ) v, key, result );
            } else {
                result.put( key, String.valueOf( v ) );
            }
        } );
    }

    private static String concat( String left, String right ) {
        if( left.isEmpty() ) return right;

        return left + '.' + right;
    }

    public Path getPath( String name ) {
        return fileSystemType.getPath( name, this );
    }

    @JsonProperty
    public Map<String, String> getConfiguration() {
        var ret = new LinkedHashMap<String, String>();
        getProps().forEach( ( k, v ) -> ret.put( ( String ) k, ( String ) v ) );
        return ret;
    }

    public void set( OapHadoopConfiguration oapHadoopConfiguration ) {
        reset();

        this.fileSystemType = oapHadoopConfiguration.fileSystemType;

        oapHadoopConfiguration.getConfiguration().forEach( this::set );
    }

    public void reset() {
        reloadConfiguration();
    }
}
