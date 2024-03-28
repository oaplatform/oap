package oap.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigurationLoader {
    private static volatile HashMap<Class<?>, ArrayList<Configuration<?>>> configurations;

    private static void init() {
        if( configurations == null ) {
            synchronized( ConfigurationLoader.class ) {
                if( configurations == null ) {
                    configurations = new HashMap<>();
                    try {
                        List<URL> urls = Resources.urls( "META-INF/oap-module.conf" );
                        log.trace( "urls {}", urls );

                        var objectMapper = new ObjectMapper( new HoconFactory() );
                        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
                        objectMapper.getDeserializationConfig().with( new JacksonAnnotationIntrospector() );
                        objectMapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );

                        for( var url : urls ) {
                            var conf = objectMapper.readValue( url, Configurations.class );
                            log.trace( "conf {}", conf );

                            for( var c : conf.configurations ) {
                                ArrayList<Configuration<?>> list = configurations.computeIfAbsent( c.loader, l -> new ArrayList<>() );
                                list.add( c );
                            }
                        }

                        log.trace( "configurations {}", configurations );
                    } catch( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
            }
        }
    }

    public static <C, T extends Configuration<C>> List<T> read( Class<?> loader, TypeReference<Map<String, List<T>>> typeReference ) {
        init();

        ArrayList<Configuration<?>> conf = configurations.get( loader );

        if( conf == null ) {
            return List.of();
        }

        var objectMapper = new ObjectMapper( new JsonFactory() );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.getDeserializationConfig().with( new JacksonAnnotationIntrospector() );
        objectMapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );

        try {
            String data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString( Map.of( "map", conf ) );

            Map<String, List<T>> config = objectMapper.readValue( data, typeReference );
            return config.get( "map" );
        } catch( JsonProcessingException e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T get( Class<T> clazz ) {
        init();

        return ( T ) configurations.get( clazz );
    }

    @ToString
    public static class Configurations {
        public final ArrayList<ClassConfiguration<?>> configurations = new ArrayList<>();
    }

    @ToString
    public static class Configuration<T> {
        public T config;
    }

    @ToString
    public static class ClassConfiguration<T> extends Configuration<T> {
        public final Class<?> loader;

        @JsonCreator
        public ClassConfiguration( Class<?> loader ) {
            this.loader = loader;
        }
    }
}
