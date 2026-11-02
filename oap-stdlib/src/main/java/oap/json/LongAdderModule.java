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

import oap.util.Numbers;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.deser.jdk.NumberDeserializers;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.util.concurrent.atomic.LongAdder;

class LongAdderModule {
    public static final class LongAdderSerializer extends StdScalarSerializer<LongAdder> {
        private LongAdderSerializer() {
            super( LongAdder.class );
        }

        @Override
        public void serialize( LongAdder value, JsonGenerator gen, SerializationContext provider ) {
            gen.writePOJO( value.sum() );
        }
    }

    public static class LongAdderSerializers extends Serializers.Base {
        @Override
        public ValueSerializer<?> findSerializer( SerializationConfig config, JavaType type, BeanDescription.Supplier beanDesc, com.fasterxml.jackson.annotation.JsonFormat.Value formatOverrides ) {
            final Class<?> raw = type.getRawClass();
            if( LongAdder.class.isAssignableFrom( raw ) ) {
                return new LongAdderSerializer();
            }

            return super.findSerializer( config, type, beanDesc, formatOverrides );
        }
    }

    static final class LongAdderDeserializer extends StdScalarDeserializer<LongAdder> {
        private final NumberDeserializers.LongDeserializer deserializer;

        private LongAdderDeserializer() {
            super( LongAdder.class );
            deserializer = new NumberDeserializers.LongDeserializer( long.class, null );
        }

        @Override
        public LongAdder deserialize( JsonParser p, DeserializationContext ctxt ) {
            final long l = p.hasToken( JsonToken.VALUE_STRING )
                ? Numbers.parseLongWithUnits( p.getString().trim() )
                : deserializer.deserialize( p, ctxt );
            return new oap.concurrent.LongAdder( l );
        }
    }

    public static class LongAdderDeserializers extends Deserializers.Base {
        @Override
        public boolean hasDeserializerFor( DeserializationConfig config, Class<?> valueType ) {
            return LongAdder.class.isAssignableFrom( valueType );
        }

        @Override
        public ValueDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                          BeanDescription.Supplier beanDesc ) throws DatabindException {
            final Class<?> raw = type.getRawClass();
            if( LongAdder.class.isAssignableFrom( raw ) ) {
                return new LongAdderDeserializer();
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}

