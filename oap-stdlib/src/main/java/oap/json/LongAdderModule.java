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
import com.fasterxml.jackson.core.JsonToken;
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
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import oap.util.Numbers;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

class LongAdderModule {
    public static final class LongAdderSerializer extends StdScalarSerializer<LongAdder> {
        private LongAdderSerializer() {
            super( LongAdder.class );
        }

        @Override
        public void serialize( LongAdder value, JsonGenerator gen, SerializerProvider provider ) throws IOException {
            gen.writeObject( value.sum() );
        }
    }

    public static class LongAdderSerializers extends Serializers.Base {
        @Override
        public JsonSerializer<?> findSerializer( SerializationConfig config, JavaType type, BeanDescription beanDesc ) {
            final Class<?> raw = type.getRawClass();
            if( LongAdder.class.isAssignableFrom( raw ) ) {
                return new LongAdderSerializer();
            }

            return super.findSerializer( config, type, beanDesc );
        }
    }

    static final class LongAdderDeserializer extends StdScalarDeserializer<LongAdder> {
        private final NumberDeserializers.LongDeserializer deserializer;

        private LongAdderDeserializer() {
            super( LongAdder.class );
            deserializer = new NumberDeserializers.LongDeserializer( long.class, null );
        }

        @Override
        public LongAdder deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {
            final long l = p.hasToken( JsonToken.VALUE_STRING )
                ? Numbers.parseLongWithUnits( p.getText().trim() )
                : deserializer.deserialize( p, ctxt );
            return new oap.concurrent.LongAdder( l );
        }
    }

    public static class LongAdderDeserializers extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                         BeanDescription beanDesc ) throws JsonMappingException {
            final Class<?> raw = type.getRawClass();
            if( LongAdder.class.isAssignableFrom( raw ) ) {
                return new LongAdderDeserializer();
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}

