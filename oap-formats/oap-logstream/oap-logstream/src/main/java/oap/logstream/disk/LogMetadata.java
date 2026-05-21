package oap.logstream.disk;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.logstream.LogId;
import oap.util.Maps;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;


@ToString
@EqualsAndHashCode( exclude = "clientHostname" )
public class LogMetadata {
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

    @JsonAnyGetter
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty( String name, String value ) {
        properties.put( name, value );
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
