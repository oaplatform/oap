package oap.json.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.configuration.ConfigurationLoader;
import oap.json.ClassDeserializer;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class PropertiesDeserializer extends ValueDeserializer<Object> {
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
    public Object deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) {
        init();

        String currentName = jsonParser.currentName();
        TokenStreamContext parsingContext = jsonParser.streamReadContext();
        TokenStreamContext parent = parsingContext.getParent();
        Object currentValue = parent.currentValue();

        if( currentValue == null ) { // not list/map
            currentValue = parsingContext.currentValue();
        }

        Configuration.ClassConfiguration classConfiguration = propertiesMap.get( currentValue.getClass() );

        Class<?> aClass = classConfiguration != null
            ? classConfiguration.properties.getOrDefault( currentName, Object.class )
            : Object.class;

        log.trace( "currentName {} aClass {} currentValue {}", currentName, aClass, currentValue );

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
