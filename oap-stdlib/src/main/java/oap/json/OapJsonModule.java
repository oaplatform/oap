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

import oap.reflect.Coercions;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Numbers;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.Version;
import tools.jackson.core.util.VersionUtil;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.NumberDeserializers;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;


public class OapJsonModule extends JacksonModule {
    public static final Version VERSION = VersionUtil.parseVersion(
        "1.0.0", "oap", "oap-json" );

    @Override
    public String getModuleName() {
        return "OapJson";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule( SetupContext context ) {
        context.addSerializers( new PathModule.PathSerializers() );
        context.addDeserializers( new PathModule.PathDeserializers() );

        context.addSerializers( new MutableObjectModule.MutableObjectSerializers() );
        context.addDeserializers( new MutableObjectModule.MutableObjectDeserializers() );

        context.addSerializers( new LongAdderModule.LongAdderSerializers() );
        context.addDeserializers( new LongAdderModule.LongAdderDeserializers() );

        context.addDeserializers( new CustomValueModule.CustomValueDeserializers() );

        SimpleDeserializers deserializers = new SimpleDeserializers();
        deserializers.addDeserializer( Long.TYPE, new LongDeserializer( Long.TYPE, 0L ) );
        deserializers.addDeserializer( Long.class, new LongDeserializer( Long.class, null ) );

        context.addDeserializers( deserializers );
    }

    static class LongDeserializer extends StdScalarDeserializer<Long> {

        private final NumberDeserializers.LongDeserializer deserializer;

        LongDeserializer( Class<Long> cls, Long nullValue ) {
            super( cls );
            deserializer = new NumberDeserializers.LongDeserializer( cls, nullValue );
        }

        @Override
        public Long deserialize( JsonParser p, DeserializationContext ctxt ) {
            return p.hasToken( JsonToken.VALUE_STRING )
                ? Numbers.parseLongWithUnits( p.getString().trim() )
                : deserializer.deserialize( p, ctxt );
        }
    }

    public static class StringDeserializer extends StdScalarDeserializer<String> {
        static final Reflection STRING_REFLECTION = Reflect.reflect( String.class );

        protected StringDeserializer() {
            super( String.class );
        }

        @Override
        public String deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) {
            String s = tools.jackson.databind.deser.jdk.StringDeserializer.instance.deserialize( jsonParser, deserializationContext );

            return ( String ) Coercions.castFunction( STRING_REFLECTION, s );
        }
    }
}
