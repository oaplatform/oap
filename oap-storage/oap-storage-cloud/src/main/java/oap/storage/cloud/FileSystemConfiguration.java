package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * fs.[s3|gcs|ab|ftp|ftps|smb|file].<property>[.<configurationId>]
 */
@Slf4j
public class FileSystemConfiguration {
    private final LinkedHashMap<String, Map<String, Object>> properties;
    private final Map<String, String> configurationIdToScheme;

    public FileSystemConfiguration( Map<String, Object> configuration ) {
        this.properties = parse( configuration );
        this.configurationIdToScheme = buildConfigurationIdRegistry( properties );
        logDefaults();
    }

    private FileSystemConfiguration( LinkedHashMap<String, Map<String, Object>> properties ) {
        this.properties = properties;
        this.configurationIdToScheme = buildConfigurationIdRegistry( properties );
        logDefaults();
    }

    /**
     * Builds the id-&gt;property-&gt;value structure from `configuration`, overlaid with `fs.*` JVM system
     * properties and `fs.*` OS environment variables. Priority, highest first: env, system properties, `configuration`.
     * `_` is not a valid character in this class's key namespace — any `_` in a system-property/env key is
     * normalized to `-` (e.g. `fs.file.filesystem.remove_empty_folders` behaves as `...remove-empty-folders`).
     */
    private static LinkedHashMap<String, Map<String, Object>> parse( Map<String, Object> configuration ) {
        LinkedHashMap<String, Map<String, Object>> properties = new LinkedHashMap<>();

        LinkedHashMap<String, Object> fsList = toStringList( configuration );
        log.trace( "string fs {}", fsList );

        for( String key : System.getProperties().stringPropertyNames() ) {
            if( key.startsWith( "fs." ) ) fsList.put( key.replace( '_', '-' ), System.getProperty( key ) );
        }

        for( Map.Entry<String, String> entry : System.getenv().entrySet() ) {
            if( entry.getKey().startsWith( "fs." ) ) fsList.put( entry.getKey().replace( '_', '-' ), entry.getValue() );
        }

        for( Map.Entry<String, Object> entry : fsList.entrySet() ) {
            String[] toks = entry.getKey().split( "\\.", 3 );

            Preconditions.checkArgument( toks.length == 3 && "fs".equals( toks[0] ),
                "invalid fs configuration key: " + entry.getKey() );

            String id = toks[1];
            String property = toks[2];

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

    /**
     * Discovers the configurationId -> scheme registry: every {@code fs.<scheme>.container.<configurationId>} key
     * registers {@code configurationId -> scheme}. A configurationId not found here (e.g. one simply named after
     * its own scheme) resolves instead via the "bare configurationId == scheme name" convention applied by
     * {@link #findConfigurationIdByContainer} and by {@link FileSystem}'s dispatch.
     */
    private static Map<String, String> buildConfigurationIdRegistry( Map<String, Map<String, Object>> properties ) {
        Map<String, String> registry = new LinkedHashMap<>();

        for( Map.Entry<String, Map<String, Object>> schemeEntry : properties.entrySet() ) {
            String scheme = schemeEntry.getKey();
            if( "default".equals( scheme ) ) continue;

            for( String property : schemeEntry.getValue().keySet() ) {
                if( !property.startsWith( "container." ) ) continue;

                String configurationId = property.substring( "container.".length() );
                String existingScheme = registry.put( configurationId, scheme );
                if( existingScheme != null && !existingScheme.equals( scheme ) ) {
                    throw new CloudException( s( "fs: configurationId '${configurationId}' cannot be registered to multiple schemes: ${existingScheme}, ${scheme}" ) );
                }
            }
        }

        return registry;
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

    private void logDefaults() {
        log.info( "fs {}", properties );
    }

    /**
     * Resolves a configurationId to its scheme via the discovered registry
     * (fs.&lt;scheme&gt;.container.&lt;configurationId&gt; entries). Does not know about the implicit
     * self-configurationId-equals-scheme-name fallback — that requires the set of installed backend schemes,
     * which only {@link FileSystem} knows.
     */
    public Optional<String> findScheme( String configurationId ) {
        return Optional.ofNullable( configurationIdToScheme.get( configurationId ) );
    }

    public String getScheme( String configurationId ) {
        return findScheme( configurationId ).orElseThrow( () -> new CloudException(
            s( "fs: configurationId '${configurationId}' cannot be resolved to a scheme; declare fs.<scheme>.container.${configurationId}" ) ) );
    }

    /**
     * Throws if `configurationId` isn't registered to any scheme (i.e. no `fs.<scheme>.container.<configurationId>`
     * declares it). Use to validate a configurationId up front, without needing its resolved scheme.
     */
    public FileSystemConfiguration required( String configurationId ) {
        getScheme( configurationId );

        return this;
    }

    /**
     * Finds the configurationId registered under `scheme` whose resolved `container` property equals `container` —
     * used to map a legacy `scheme://container/path` URI onto a configurationId. Falls back to the scheme's own
     * name (the "bare configurationId == scheme name" convention) when `container` matches the scheme-wide container.
     */
    public Optional<String> findConfigurationIdByContainer( String scheme, String container ) {
        Map<String, Object> schemeMap = properties.get( scheme );
        if( schemeMap == null ) return Optional.empty();

        for( Map.Entry<String, Object> entry : schemeMap.entrySet() ) {
            if( entry.getKey().startsWith( "container." ) && container.equals( entry.getValue() ) ) {
                return Optional.of( entry.getKey().substring( "container.".length() ) );
            }
        }

        Object schemeWideContainer = schemeMap.get( "container" );
        if( container.equals( schemeWideContainer ) ) {
            return Optional.of( scheme );
        }

        return Optional.empty();
    }

    public Object get( String scheme, @Nullable String configurationId, String property ) {
        Map<String, Object> schemeMap = properties.getOrDefault( scheme, Map.of() );

        if( configurationId != null ) {
            Object value = schemeMap.get( s( "${property}.${configurationId}" ) );
            if( value != null ) return value;
        }

        Object value = schemeMap.get( property );
        if( value != null ) return value;

        return properties.getOrDefault( "default", Map.of() ).get( property );
    }

    public Object getOrThrow( String scheme, @Nullable String configurationId, String property ) {
        Object res = get( scheme, configurationId, property );
        if( res == null ) {
            throw new CloudException( "fs." + scheme + "." + property + ( configurationId != null ? "." + configurationId : "" ) + " is required" );
        }
        return res;
    }

    @Override
    public String toString() {

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        properties.forEach( ( k, m ) -> {
            m.forEach( ( k2, v ) -> map.put( s( "fs.${k}.${k2}" ), v ) );
        } );

        return Binder.json.marshal( map );
    }
}
