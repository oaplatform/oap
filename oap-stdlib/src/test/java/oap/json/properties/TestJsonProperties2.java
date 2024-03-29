package oap.json.properties;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestJsonProperties2 {
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
}
