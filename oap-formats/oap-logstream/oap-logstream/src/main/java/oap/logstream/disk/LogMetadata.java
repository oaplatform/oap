package oap.logstream.disk;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.io.Files;
import oap.json.Binder;
import oap.logstream.LogId;
import oap.util.Maps;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;


@ToString
@EqualsAndHashCode( exclude = "clientHostname" )
public class LogMetadata {
    public static final String EXTENSION_LOG_METADATA = ".metadata.yaml";
    public static final String EXTENSION_LOG_TRANSACTION = ".metadata.transaction";

    public final String type;
    public final String clientHostname;
    @JsonIgnore
    public final Map<String, String> properties;
    public final String[] headers;
    @JsonIgnore
    public final byte[][] types;
    private final String filePrefixPattern;

    @JsonCreator
    public LogMetadata( String filePrefixPattern,
                        String type,
                        String clientHostname,
                        Map<String, String> properties,
                        String[] headers,
                        byte[][] types ) {
        this.filePrefixPattern = filePrefixPattern;
        this.type = type;
        this.clientHostname = clientHostname;
        this.properties = properties != null ? new LinkedHashMap<>( properties ) : new LinkedHashMap<>();
        this.headers = headers;
        this.types = types;
    }

    public LogMetadata( LogId logId ) {
        this( logId.filePrefixPattern, logId.logType,
            logId.clientHostname, logId.properties, logId.headers, logId.types );
    }

    public static LogMetadata readFor( Path file ) {
        return Binder.yaml.unmarshal( LogMetadata.class, pathFor( file ) );
    }

    public static Path pathFor( Path file, String extension ) {
        return Path.of( file.toString() + extension );
    }

    public static Path pathFor( Path file ) {
        return pathFor( file, EXTENSION_LOG_METADATA );
    }

    public static Path pathFor( String file, String extension ) {
        return Path.of( file + extension );
    }

    public static Path pathFor( String file ) {
        return pathFor( file, EXTENSION_LOG_METADATA );
    }

    public static Path pathForDataFromMetadata( Path metadataPath ) {
        Preconditions.checkArgument( isMetadata( metadataPath ) );

        String metadataPathString = metadataPath.toString();
        return Paths.get( metadataPathString.substring( 0, metadataPathString.indexOf( EXTENSION_LOG_METADATA ) ) );
    }

    public static boolean isMetadata( Path filename ) {
        return filename.toString().endsWith( EXTENSION_LOG_METADATA );
    }

    public static void rename( Path filename, Path newFile ) {
        Path from = pathFor( filename );
        if( Files.exists( from ) )
            Files.rename( from, pathFor( newFile ) );
    }

    public static void addProperty( Path path, String name, String value ) {
        LogMetadata metadata = LogMetadata.readFor( path );
        metadata.setProperty( name, value );
        metadata.writeFor( path );
    }

    public static long beginTransaction( Path file ) throws IOException {
        Path path = pathFor( file, EXTENSION_LOG_TRANSACTION );

        int dataSize;
        if( java.nio.file.Files.exists( path ) ) {
            dataSize = Integer.parseInt( java.nio.file.Files.readString( path, StandardCharsets.UTF_8 ) );
        } else {
            dataSize = 0;
        }

        return dataSize;
    }

    public static void commitTransaction( Path file, int length ) throws IOException {
        Path path = pathFor( file, EXTENSION_LOG_TRANSACTION );

        int dataSize;
        if( java.nio.file.Files.exists( path ) ) {
            dataSize = Integer.parseInt( java.nio.file.Files.readString( path, StandardCharsets.UTF_8 ) );
        } else {
            dataSize = 0;
        }


        dataSize += length;

        java.nio.file.Files.writeString( path, String.valueOf( dataSize ), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING );
    }

    @JsonAnyGetter
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty( String name, String value ) {
        properties.put( name, value );
    }

    public void writeFor( Path file ) {
        Path path = pathFor( file );
        Path tmpFile = Path.of( path + ".tmp" );
        Binder.yaml.marshal( tmpFile, this );

        Files.rename( tmpFile, path );
    }

    public DateTime getDateTime( String name ) {
        return Maps.get( properties, name )
            .map( v -> new DateTime( v, UTC ) )
            .orElse( null );
    }

    @JsonGetter
    public List<Byte[]> types() {
        ArrayList<Byte[]> ret = new ArrayList<>();
        for( byte[] t : types ) {
            Byte[] bb = new Byte[t.length];
            for( int i = 0; i < t.length; i++ ) {
                bb[i] = t[i];
            }
            ret.add( bb );
        }

        return ret;
    }

    public String getString( String name ) {
        return properties.get( name );
    }

    public LogMetadata withProperty( String propertyName, String value ) {
        LinkedHashMap<String, String> newProperties = new LinkedHashMap<>( properties );
        newProperties.put( propertyName, value );
        return new LogMetadata( filePrefixPattern, type, clientHostname, newProperties, headers, types );
    }
}
