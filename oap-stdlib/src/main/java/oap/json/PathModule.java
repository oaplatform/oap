/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathModule {
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
            return Paths.get( p.getValueAsString() );
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
}

