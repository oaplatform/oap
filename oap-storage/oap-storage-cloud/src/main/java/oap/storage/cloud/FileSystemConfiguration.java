package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * fs.default.alias (required — the only fs.default.* key)
 * fs.[s3|gcs|ab|ftp|ftps|smb|file].<property>[.<alias>]
 */
@Slf4j
public class FileSystemConfiguration {
    private final LinkedHashMap<String, Map<String, Object>> properties;
    private final Map<String, String> aliasToScheme;

    public FileSystemConfiguration( Map<String, Object> configuration ) {
        this.properties = parse( configuration );
        this.aliasToScheme = buildAliasRegistry( properties );
        logDefaults();
    }

    private FileSystemConfiguration( LinkedHashMap<String, Map<String, Object>> properties ) {
        this.properties = properties;
        this.aliasToScheme = buildAliasRegistry( properties );
        logDefaults();
    }

    private static LinkedHashMap<String, Map<String, Object>> parse( Map<String, Object> configuration ) {
        LinkedHashMap<String, Map<String, Object>> properties = new LinkedHashMap<>();

        LinkedHashMap<String, Object> fsList = toStringList( configuration );
        log.trace( "string fs {}", fsList );

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
     * Discovers the alias -> scheme registry: every {@code fs.<scheme>.container.<alias>} key registers
     * {@code alias -> scheme}. An alias not found here (e.g. the default alias, when it's simply named
     * after its own scheme) resolves instead via the "bare alias == scheme name" convention applied by
     * {@link #findAliasByContainer} and by {@link FileSystem}'s dispatch.
     */
    private static Map<String, String> buildAliasRegistry( Map<String, Map<String, Object>> properties ) {
        Map<String, String> registry = new LinkedHashMap<>();

        for( Map.Entry<String, Map<String, Object>> schemeEntry : properties.entrySet() ) {
            String scheme = schemeEntry.getKey();
            if( "default".equals( scheme ) ) continue;

            for( String property : schemeEntry.getValue().keySet() ) {
                if( !property.startsWith( "container." ) ) continue;

                String alias = property.substring( "container.".length() );
                String existingScheme = registry.put( alias, scheme );
                if( existingScheme != null && !existingScheme.equals( scheme ) ) {
                    throw new CloudException( "fs: alias '" + alias + "' cannot be registered to multiple schemes: "
                        + existingScheme + ", " + scheme );
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
        log.info( "DefaultAlias {}", tryGetDefault( "alias" ) );
        log.info( "fs {}", properties );
    }

    public String getDefaultAlias() {
        return getDefault( "alias" );
    }

    private String getDefault( String parameter ) {
        return Preconditions.checkNotNull( tryGetDefault( parameter ), "fs.default." + parameter + " is required" );
    }

    @Nullable
    private String tryGetDefault( String parameter ) {
        Map<String, Object> defaults = properties.get( "default" );
        if( defaults == null ) return null;
        return ( String ) defaults.get( parameter );
    }

    /**
     * Resolves an alias to its scheme via the discovered registry (fs.&lt;scheme&gt;.container.&lt;alias&gt;
     * entries). Does not know about the implicit self-alias-equals-scheme-name fallback — that requires
     * the set of installed backend schemes, which only {@link FileSystem} knows.
     */
    public Optional<String> findScheme( String alias ) {
        return Optional.ofNullable( aliasToScheme.get( alias ) );
    }

    public String getScheme( String alias ) {
        return findScheme( alias ).orElseThrow( () -> new CloudException(
            "fs: alias '" + alias + "' cannot be resolved to a scheme; declare fs.<scheme>.container." + alias ) );
    }

    /**
     * Finds the alias registered under `scheme` whose resolved `container` property equals `container` —
     * used to map a legacy `scheme://container/path` URI onto an alias. Falls back to the scheme's own
     * name (the "bare alias == scheme name" convention) when `container` matches the scheme-wide container.
     */
    public Optional<String> findAliasByContainer( String scheme, String container ) {
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

    public Object get( String scheme, @Nullable String alias, String property ) {
        Map<String, Object> schemeMap = properties.getOrDefault( scheme, Map.of() );

        if( alias != null ) {
            Object value = schemeMap.get( property + "." + alias );
            if( value != null ) return value;
        }

        Object value = schemeMap.get( property );
        if( value != null ) return value;

        return properties.getOrDefault( "default", Map.of() ).get( property );
    }

    public Object getOrThrow( String scheme, @Nullable String alias, String property ) {
        Object res = get( scheme, alias, property );
        if( res == null ) {
            throw new CloudException( "fs." + scheme + "." + property + ( alias != null ? "." + alias : "" ) + " is required" );
        }
        return res;
    }

    @Override
    public String toString() {

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        properties.forEach( ( k, m ) -> {
            m.forEach( ( k2, v ) -> map.put( "fs." + k + "." + k2, v ) );
        } );

        return Binder.json.marshal( map );
    }
}
