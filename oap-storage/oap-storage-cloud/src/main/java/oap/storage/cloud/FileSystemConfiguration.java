package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static oap.util.Pair.__;

/**
 * fs.[s3|gcs|ab][.container?].endpoint
 * fs.[s3|gcs|ab][.container?].identity
 * fs.[s3|gcs|ab][.container?].credential
 */
@ToString
@Slf4j
public class FileSystemConfiguration {
    private final LinkedHashMap<String, Map<String, Object>> properties = new LinkedHashMap<>();

    public FileSystemConfiguration( Map<String, Object> configuration ) {
        LinkedHashMap<String, Object> fsList = toStringList( configuration );
        log.trace( "string fs {}", fsList );

        for( var entry : fsList.entrySet() ) {
            String[] toks = entry.getKey().split( "(?<!\\\\)\\." );

            Preconditions.checkArgument( "fs".equals( toks[0] ) );
            String id = toks[1];

            int start = 2;
            if( !toks[2].equals( "jclouds" ) ) {
                id = id + "." + toks[2];
                start++;
            }

            var property = StringUtils.join( toks, ".", start, toks.length );
            properties.computeIfAbsent( id, x -> new LinkedHashMap<>() ).put( property, entry.getValue() );

        }

        String defaultScheme = getDefaultScheme();
        String defaultContainer = getDefaultContainer();

        log.info( "DefaultScheme {} DefaultContainer {}", defaultScheme, defaultContainer );
        log.info( "fs {}", properties );
    }

    public String getDefaultScheme() {
        return getDefault( "scheme" );
    }

    public String getDefaultContainer() {
        return getDefault( "container" );
    }

    private String getDefault( String parameter ) {
        Map<String, Object> defaults = properties.get( "default" );
        Preconditions.checkNotNull( defaults, "fs.default is required" );
        return Preconditions.checkNotNull( ( String ) defaults.get( "jclouds." + parameter ), "fs.default.jclouds." + parameter + " is required" );
    }

    private LinkedHashMap<String, Object> toStringList( Object configuration ) {
        var ret = new LinkedHashMap<String, Object>();

        toStringList( configuration, ret, "" );

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    private void toStringList( Object configuration, LinkedHashMap<String, Object> map, String prefix ) {
        if( configuration instanceof Map ) {
            Map<String, Object> objectMap = ( Map<String, Object> ) configuration;

            for( var entry : objectMap.entrySet() ) {
                String keyPrefix = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();

                toStringList( entry.getValue(), map, keyPrefix );
            }
        } else {
            map.put( prefix, configuration );
        }
    }

    private Pair<Map<String, Map<String, Object>>, Map<String, Map<String, Map<String, Object>>>> splitBySize( Map<String, Object> fs ) {
        Map<String, Map<String, Object>> defaultFs = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, Object>>> containerFs = new LinkedHashMap<>();

        for( var entry : fs.entrySet() ) {
            String[] toks = entry.getKey().split( "(?<!\\\\)\\." );
            log.trace( "toks {}", List.of( toks ) );

            if( toks.length > 3 ) {
                toMap( containerFs, toks, entry.getValue() );
            } else {
                toMap( defaultFs, toks, entry.getValue() );
            }
        }

        return __( defaultFs, containerFs );
    }

    private void toMap( Map<String, ? extends Object> map, String[] toks, Object value ) {
        Object l = map;
        for( int i = 1; i < toks.length; i++ ) {
            int finalI = i;
            l = ( ( Map ) l ).computeIfAbsent( toks[i], t -> finalI < toks.length - 1 ? new HashMap<String, Object>()
                : value );
        }
    }

    public Map<String, Object> get( String scheme, String container ) {
        Map<String, Object> conf = properties.get( scheme + "." + container );
        if( conf == null ) {
            conf = properties.get( scheme );
        }
        if( conf == null ) {
            conf = Map.of();
        }

        return conf;
    }
}
