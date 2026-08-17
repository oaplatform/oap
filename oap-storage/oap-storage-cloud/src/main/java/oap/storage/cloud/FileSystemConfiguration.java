package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;
import static oap.util.Pair.__;

/**
 * fs.[s3|gcs|ab][.container?].endpoint
 * fs.[s3|gcs|ab][.container?].identity
 * fs.[s3|gcs|ab][.container?].credential
 */
@Slf4j
public class FileSystemConfiguration {
    private final LinkedHashMap<String, Map<String, Object>> properties;

    public FileSystemConfiguration( Map<String, Object> configuration ) {
        this.properties = parse( configuration );
        logDefaults();
    }

    private FileSystemConfiguration( LinkedHashMap<String, Map<String, Object>> properties ) {
        this.properties = properties;
        logDefaults();
    }

    /**
     * Returns a new configuration with `newConfiguration` merged over this one: ids/keys absent from
     * `newConfiguration` keep their value from this configuration, ids/keys present in both are overwritten.
     */
    public FileSystemConfiguration copyWith( FileSystemConfiguration newConfiguration ) {
        LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for( Map.Entry<String, Map<String, Object>> entry : this.properties.entrySet() ) {
            merged.put( entry.getKey(), new LinkedHashMap<>( entry.getValue() ) );
        }

        for( Map.Entry<String, Map<String, Object>> entry : newConfiguration.properties.entrySet() ) {
            merged.computeIfAbsent( entry.getKey(), x -> new LinkedHashMap<>() ).putAll( entry.getValue() );
        }

        return new FileSystemConfiguration( merged );
    }

    /**
     * Returns a new configuration with `newConfiguration` merged over this one: ids/keys absent from
     * `newConfiguration` keep their value from this configuration, ids/keys present in both are overwritten.
     */
    public FileSystemConfiguration copyWith( Map<String, Object> newConfiguration ) {
        LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for( Map.Entry<String, Map<String, Object>> entry : this.properties.entrySet() ) {
            merged.put( entry.getKey(), new LinkedHashMap<>( entry.getValue() ) );
        }

        for( Map.Entry<String, Map<String, Object>> entry : parse( newConfiguration ).entrySet() ) {
            merged.computeIfAbsent( entry.getKey(), x -> new LinkedHashMap<>() ).putAll( entry.getValue() );
        }

        return new FileSystemConfiguration( merged );
    }

    private static LinkedHashMap<String, Map<String, Object>> parse( Map<String, Object> configuration ) {
        LinkedHashMap<String, Map<String, Object>> properties = new LinkedHashMap<>();

        LinkedHashMap<String, Object> fsList = toStringList( configuration );
        log.trace( "string fs {}", fsList );

        for( Map.Entry<String, Object> entry : fsList.entrySet() ) {
            String[] toks = entry.getKey().split( "(?<!\\\\)\\." );

            Preconditions.checkArgument( "fs".equals( toks[0] ) );
            String id = toks[1];

            int start = 2;
            if( !toks[2].equals( "jclouds" ) && !toks[2].equals( "clouds" ) ) {
                id = id + "." + toks[2].replace( "\\.", "." );
                start++;
            }

            if( start < toks.length - 1 ) {
                if( toks[start].equals( "clouds" ) ) {
                    toks[start] = "jclouds";
                }
            }

            String property = StringUtils.join( toks, ".", start, toks.length );

            String value = new StringSubstitutor( key -> {
                if( key.startsWith( "env." ) ) {
                    return System.getenv( key.substring( 4 ) );
                } else {
                    return System.getProperty( key );
                }
            }, "${", "}", '\\' ).replace( entry.getValue() );

            properties.computeIfAbsent( id, x -> new LinkedHashMap<>() ).put( property, value );

        }

        return properties;
    }

    private void logDefaults() {
        String defaultScheme = getDefaultScheme();
        String defaultContainer = tryGetDefaultContainer();

        log.info( "DefaultScheme {} DefaultContainer {}", defaultScheme, defaultContainer );
        log.info( "fs {}", properties );
    }

    public String getDefaultScheme() {
        return getDefault( "scheme" );
    }

    public String getDefaultContainer() {
        return getDefault( "container" );
    }

    @Nullable
    public String tryGetDefaultContainer() {
        return tryGetDefault( "container" );
    }

    private String getDefault( String parameter ) {
        return Preconditions.checkNotNull( tryGetDefault( parameter ), "fs.default.jclouds." + parameter + " is required" );
    }

    @Nullable
    private String tryGetDefault( String parameter ) {
        Map<String, Object> defaults = properties.get( "default" );
        Preconditions.checkNotNull( defaults, "fs.default is required" );
        return ( String ) defaults.get( "jclouds." + parameter );
    }

    private static LinkedHashMap<String, Object> toStringList( Object configuration ) {
        var ret = new LinkedHashMap<String, Object>();

        toStringList( configuration, ret, "" );

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    private static void toStringList( Object configuration, LinkedHashMap<String, Object> map, String prefix ) {
        if( configuration instanceof Map ) {
            Map<String, Object> objectMap = ( Map<String, Object> ) configuration;

            for( Map.Entry<String, Object> entry : objectMap.entrySet() ) {
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

        for( Map.Entry<String, Object> entry : fs.entrySet() ) {
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

    public Object get( String scheme, String container, String name ) {
        Map<String, Object> conf = get( scheme, container );

        return conf.get( name );
    }

    public Object getOrThrow( String scheme, String container, String name ) {
        Object res = get( scheme, container, name );
        if( res == null ) {
            throw new CloudException( s( "fs.${scheme}.${name} is required" ) );
        }
        return res;
    }

    @Override
    public String toString() {
        return Binder.json.marshal( properties );
    }
}
