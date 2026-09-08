package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final Pattern PART_PATTERN = Pattern.compile( "[A-Za-z0-9]+(_[A-Za-z0-9]+)*" );

    private static void validateKeyParts( String key ) {
        for( String part : key.split( "\\." ) ) {
            Preconditions.checkArgument( PART_PATTERN.matcher( part ).matches(),
                s( "invalid fs configuration key part '${part}' in key '${key}': only letters, digits, and single underscores are allowed" ) );
        }
    }

    /**
     * Decodes an OS environment variable name into the dotted `fs.*` key it represents, or {@code null} if
     * it isn't one (no `FS_` prefix). `.` in the key becomes a single `_`; a literal `_` already in the key
     * becomes `__`. E.g. {@code FS_A_B_D} -&gt; {@code fs.a.b.d}, {@code FS_A_B_D__F} -&gt; {@code fs.a.b.d_f}.
     */
    private static String decodeEnvKey( String envName ) {
        if( !envName.startsWith( "FS_" ) ) return null;

        StringBuilder sentinelized = new StringBuilder();
        int i = 0;
        while( i < envName.length() ) {
            if( envName.charAt( i ) == '_' && i + 1 < envName.length() && envName.charAt( i + 1 ) == '_' ) {
                sentinelized.append( ' ' );
                i += 2;
            } else {
                sentinelized.append( envName.charAt( i ) );
                i++;
            }
        }

        String[] parts = sentinelized.toString().split( "_", -1 );
        StringBuilder key = new StringBuilder();
        for( int j = 0; j < parts.length; j++ ) {
            if( j > 0 ) key.append( '.' );
            key.append( parts[j].replace( ' ', '_' ).toLowerCase() );
        }
        return key.toString();
    }

    /**
     * Builds the id-&gt;property-&gt;value structure from `configuration`, overlaid with `fs.*` JVM system
     * properties (used as-is) and OS environment variables (matched by `FS_` prefix and decoded back into a
     * dotted key — see {@link #decodeEnvKey}). Priority, highest first: env, system properties, `configuration`.
     * Every dot-separated part of every key may contain only letters, digits, and single underscores
     * (see {@link #validateKeyParts}).
     */
    private static LinkedHashMap<String, Map<String, Object>> parse( Map<String, Object> configuration ) {
        LinkedHashMap<String, Map<String, Object>> properties = new LinkedHashMap<>();

        LinkedHashMap<String, Object> fsList = toStringList( configuration );
        log.trace( "string fs {}", fsList );

        for( String key : System.getProperties().stringPropertyNames() ) {
            if( key.startsWith( "fs." ) ) fsList.put( key, System.getProperty( key ) );
        }

        for( Map.Entry<String, String> entry : System.getenv().entrySet() ) {
            String key = decodeEnvKey( entry.getKey() );
            if( key != null ) fsList.put( key, entry.getValue() );
        }

        for( Map.Entry<String, Object> entry : fsList.entrySet() ) {
            String[] toks = entry.getKey().split( "\\.", 3 );

            Preconditions.checkArgument( toks.length == 3 && "fs".equals( toks[0] ),
                "invalid fs configuration key: " + entry.getKey() );

            validateKeyParts( entry.getKey() );

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
     * {@link FileSystem}'s dispatch.
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
     * (fs.&lt;scheme&gt;.container.&lt;configurationId&gt; entries), or {@code null} if unresolved. Does not know
     * about the implicit self-configurationId-equals-scheme-name fallback — that requires the set of installed
     * backend schemes, which only {@link FileSystem} knows.
     */
    @Nullable
    public String getScheme( String configurationId ) {
        return configurationIdToScheme.get( configurationId );
    }

    public String getSchemeOrThrow( String configurationId ) {
        String scheme = getScheme( configurationId );
        if( scheme == null ) {
            throw new CloudException( s( "fs: configurationId '${configurationId}' cannot be resolved to a scheme; declare fs.<scheme>.container.${configurationId}" ) );
        }
        return scheme;
    }

    public String getSchemeOrThrow( CloudURI cloudURI ) {
        return getSchemeOrThrow( cloudURI.configurationId );
    }

    /**
     * Throws if `configurationId` isn't registered to any scheme (i.e. no `fs.<scheme>.container.<configurationId>`
     * declares it). Use to validate a configurationId up front, without needing its resolved scheme.
     */
    public FileSystemConfiguration required( String configurationId ) {
        getSchemeOrThrow( configurationId );

        return this;
    }

    public Object get( String scheme, String configurationId, String property ) {
        Preconditions.checkNotNull( configurationId, "configurationId is required" );

        Map<String, Object> schemeMap = properties.getOrDefault( scheme, Map.of() );

        Object value = schemeMap.get( s( "${property}.${configurationId}" ) );
        if( value != null ) return value;

        value = schemeMap.get( property );
        if( value != null ) return value;

        return properties.getOrDefault( "default", Map.of() ).get( property );
    }

    public Object getOrThrow( String scheme, String configurationId, String property ) {
        Object res = get( scheme, configurationId, property );
        if( res == null ) {
            throw new CloudException( s( "fs.${scheme}.${property}.${configurationId} is required" ) );
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
