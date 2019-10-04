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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.val;

import java.io.IOException;

class CustomValueModule extends SimpleModule {
    static class CustomValueDeserializer extends JsonDeserializer<CustomValue<?>> {
        @Override
        public CustomValue<?> deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException, JsonProcessingException {
            val id = jsonParser.getCurrentName();

            val  valueClass = TypeIdFactory.get( id );

            val  value = valueClass != null
                ? deserializationContext.readValue( jsonParser, valueClass )
                : deserializationContext.readValue( jsonParser, Object.class );

            return new CustomValue<>( value );
        }
    }

    public static class CustomValueDeserializers extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                         BeanDescription beanDesc ) throws JsonMappingException {
            final Class<?> raw = type.getRawClass();
            if( CustomValue.class.equals( raw ) ) {
                return new CustomValueModule.CustomValueDeserializer();
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}
