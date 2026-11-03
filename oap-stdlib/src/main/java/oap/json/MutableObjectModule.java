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

import org.apache.commons.lang3.mutable.MutableObject;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
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
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.ser.std.StdSerializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MutableObjectModule {
    public static class MutableObjectSerializer extends StdSerializer<MutableObject<?>> {
        public MutableObjectSerializer( JavaType type ) {
            super( type );
        }

        @Override
        public void serialize( MutableObject value, JsonGenerator gen, SerializationContext serializers ) {
            gen.writePOJO( value.getValue() );
        }
    }

    public static class MutableObjectSerializers extends Serializers.Base {
        @Override
        public ValueSerializer<?> findSerializer( SerializationConfig config, JavaType type, BeanDescription.Supplier beanDesc, com.fasterxml.jackson.annotation.JsonFormat.Value formatOverrides ) {
            Class<?> raw = type.getRawClass();
            if( MutableObject.class.isAssignableFrom( raw ) ) {
                return new MutableObjectSerializer( type );
            }

            return super.findSerializer( config, type, beanDesc, formatOverrides );
        }
    }

    public static class MutableObjectDeserializer extends StdDeserializer<MutableObject<?>> {
        private final JavaType refType;

        protected MutableObjectDeserializer( JavaType valueType, JavaType refType ) {
            super( valueType );
            this.refType = refType;
        }

        @Override
        public MutableObject<?> deserialize( JsonParser p, DeserializationContext ctxt ) {

            try {
                MutableObject<?> mutableObject = ( MutableObject<?> ) _valueClass.getDeclaredConstructor().newInstance();

                mutableObject.setValue( ctxt.readValue( p, refType ) );

                return mutableObject;
            } catch( InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e ) {
                throw ctxt.instantiationException( _valueClass, e );
            }
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public MutableObject<?> deserialize( JsonParser p, DeserializationContext ctxt, MutableObject intoValue ) {
            intoValue.setValue( ctxt.readValue( p, refType ) );

            return intoValue;
        }
    }

    public static class MutableObjectDeserializers extends Deserializers.Base {
        @Override
        public boolean hasDeserializerFor( DeserializationConfig config, Class<?> valueType ) {
            return MutableObject.class.isAssignableFrom( valueType );
        }

        @Override
        public ValueDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                          BeanDescription.Supplier beanDesc ) throws DatabindException {
            Class<?> raw = type.getRawClass();
            if( MutableObject.class.isAssignableFrom( raw ) ) {
                Type[] actualTypeArguments = ( ( ParameterizedType ) raw.getGenericSuperclass() ).getActualTypeArguments();
                JavaType refType = config.constructType( ( Class<?> ) actualTypeArguments[0] );
                return new MutableObjectDeserializer( type, refType );
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}

