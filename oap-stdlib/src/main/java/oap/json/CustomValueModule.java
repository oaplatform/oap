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

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.module.SimpleModule;


class CustomValueModule extends SimpleModule {
    static class CustomValueDeserializer extends ValueDeserializer<CustomValue<?>> {
        @Override
        public CustomValue<?> deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) {
            var id = jsonParser.currentName();

            var valueClass = TypeIdFactory.get( id );

            var value = valueClass != null
                ? deserializationContext.readValue( jsonParser, valueClass )
                : deserializationContext.readValue( jsonParser, Object.class );

            return new CustomValue<>( value );
        }
    }

    public static class CustomValueDeserializers extends Deserializers.Base {
        @Override
        public boolean hasDeserializerFor( DeserializationConfig config, Class<?> valueType ) {
            return CustomValue.class.equals( valueType );
        }

        @Override
        public ValueDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                          BeanDescription.Supplier beanDesc ) throws DatabindException {
            final Class<?> raw = type.getRawClass();
            if( CustomValue.class.equals( raw ) ) {
                return new CustomValueModule.CustomValueDeserializer();
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}
