package oap.json.properties;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestJsonProperties {
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

    @ToString
    @EqualsAndHashCode
    public static class TestProperty1 {
        public String name1;
        public String name2;

        public TestProperty1() {
        }

        public TestProperty1( String name1, String name2 ) {
            this.name1 = name1;
            this.name2 = name2;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestProperty2 {
        public int name1;
        public int name2;

        public TestProperty2() {
        }

        public TestProperty2( int name1, int name2 ) {
            this.name1 = name1;
            this.name2 = name2;
        }
    }
}
