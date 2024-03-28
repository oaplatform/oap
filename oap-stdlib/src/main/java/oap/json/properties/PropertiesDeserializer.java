package oap.json.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.configuration.ConfigurationLoader;
import oap.json.ClassDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PropertiesDeserializer extends JsonDeserializer<Object> {
    private static volatile HashMap<Class<?>, Configuration.ClassConfiguration> propertiesMap;

    private static void init() {
        if( propertiesMap == null ) {
            synchronized( PropertiesDeserializer.class ) {
                if( propertiesMap == null ) {
                    propertiesMap = new HashMap<>();
                    List<Configuration> conf = ConfigurationLoader.read( PropertiesDeserializer.class, new TypeReference<>() {} );
                    for( Configuration c : conf ) {
                        for( Configuration.ClassConfiguration cc : c.config ) {
                            var clazz = cc.clazz;

                            Configuration.ClassConfiguration classConfiguration = propertiesMap.computeIfAbsent( clazz,
                                v -> new Configuration.ClassConfiguration() );
                            classConfiguration.clazz = clazz;
                            classConfiguration.properties.putAll( cc.properties );
                        }
                    }
                    log.trace( "mapped extensions: {}", propertiesMap );
                }
            }
        }
    }

    @Override
    public Object deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException, JacksonException {
        init();

        String currentName = jsonParser.getCurrentName();
        JsonStreamContext parsingContext = jsonParser.getParsingContext();
        JsonStreamContext parent = parsingContext.getParent();
        Object currentValue = parent.getCurrentValue();

        Configuration.ClassConfiguration classConfiguration = propertiesMap.get( currentValue.getClass() );

        Class<?> aClass = classConfiguration.properties.getOrDefault( currentName, Map.class );

        return deserializationContext.readValue( jsonParser, aClass );
    }

    @ToString
    public static class Configuration extends ConfigurationLoader.Configuration<ArrayList<Configuration.ClassConfiguration>> {
        @ToString
        public static class ClassConfiguration {
            @JsonDeserialize( contentUsing = ClassDeserializer.class )
            public final LinkedHashMap<String, Class<?>> properties = new LinkedHashMap<>();
            @JsonProperty( "class" )
            @JsonDeserialize( using = ClassDeserializer.class )
            public Class<?> clazz;
        }
    }
}
