package oap.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import oap.json.properties.PropertiesDeserializer;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractProperties {
    @JsonIgnore
    private final LinkedHashMap<String, Object> properties = new LinkedHashMap<>();

    @JsonAnySetter
    @JsonDeserialize( contentUsing = PropertiesDeserializer.class )
    public void putProperty( String name, Object value ) {
        properties.put( name, value );
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings( "unchecked" )
    @Nullable
    public <T> T getProperty( String property ) {
        return ( T ) properties.get( property );
    }
}
