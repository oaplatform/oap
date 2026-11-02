package oap.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.json.HoconFactoryWithSystemProperties;
import oap.json.OapHoconFactory;
import oap.util.Lists;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public class ConfigurationLoader {
    private static volatile HashMap<Class<?>, ArrayList<Configuration<?>>> configurations;

    private static void init() {
        if( configurations == null ) {
            synchronized( ConfigurationLoader.class ) {
                if( configurations == null ) {
                    configurations = new HashMap<>();
                    List<URL> urls = Lists.concat(
                        Resources.urls( "META-INF/oap-module.oap" ),
                        Resources.urls( "META-INF/oap-module.conf" )
                    );
                    log.trace( "urls {}", urls );

                    ObjectMapper objectMapper = JsonMapper.builder( new HoconFactoryWithSystemProperties( log ) )
                        .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
                        .enable( tools.jackson.databind.MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS )
                        .build();

                    for( URL url : urls ) {
                        try( var reader = OapHoconFactory.open( url ) ) {
                            Configurations conf = objectMapper.readValue( reader, Configurations.class );
                            log.trace( "conf {}", conf );

                            for( ClassConfiguration<?> c : conf.configurations ) {
                                ArrayList<Configuration<?>> list = configurations.computeIfAbsent( c.loader, l -> new ArrayList<>() );
                                list.add( c );
                            }
                        } catch( Exception e ) {
                            log.error( "url {}", url );
                            log.error( e.getMessage(), e );
                            throw new RuntimeException( s( "${url}: ${e.getMessage()}" ), e );
                        }
                    }

                    log.trace( "configurations {}", configurations );
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

        ObjectMapper objectMapper = JsonMapper.builder( new JsonFactory() )
            .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
            .enable( tools.jackson.databind.MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS )
            .build();

        try {
            String data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString( Map.of( "map", conf ) );

            Map<String, List<T>> config = objectMapper.readValue( data, typeReference );
            return config.get( "map" );
        } catch( JacksonException e ) {
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
