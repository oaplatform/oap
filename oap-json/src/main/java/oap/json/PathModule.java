package oap.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oap.io.Files;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class PathModule extends Module {
    public static class PathSerializer extends StdSerializer<Path> {
        public PathSerializer( JavaType type ) {
            super( type );
        }

        @Override
        public void serialize( Path value, JsonGenerator gen, SerializerProvider provider ) throws IOException {
            gen.writeString( value.toString() );
        }
    }

    public static class PathSerializers extends Serializers.Base {
        @Override
        public JsonSerializer<?> findSerializer( SerializationConfig config, JavaType type, BeanDescription beanDesc ) {
            final Class<?> raw = type.getRawClass();
            if( Path.class.isAssignableFrom( raw ) ) {
                return new PathSerializer( type );
            }

            return super.findSerializer( config, type, beanDesc );
        }
    }

    public static class PathDeserializer extends StdDeserializer<Path> {

        protected PathDeserializer( JavaType valueType ) {
            super( valueType );
        }

        @Override
        public Path deserialize( JsonParser p,
            DeserializationContext ctxt ) throws IOException {
            return Files.path( p.getValueAsString() );
        }
    }

    public static class PathDeserializers extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
            BeanDescription beanDesc ) throws JsonMappingException {
            final Class<?> raw = type.getRawClass();
            if( raw == Path.class ) {
                return new PathDeserializer( type );
            }


            return super.findBeanDeserializer( type, config, beanDesc );
        }
    }

    @Override
    public String getModuleName() {
        return "PathModule";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule( SetupContext context ) {
        context.addSerializers( new PathSerializers() );
        context.addDeserializers( new PathDeserializers() );
    }

    public final static Version VERSION = VersionUtil.parseVersion(
        "1.0.0", "oap", "json" );
}
