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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import oap.util.Numbers;

import java.io.IOException;

public class OapJsonModule extends Module {
    public final static Version VERSION = VersionUtil.parseVersion(
        "1.0.0", "oap", "json" );

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

        SimpleDeserializers deserializers = new SimpleDeserializers();
        deserializers.addDeserializer( Long.TYPE, new LongDeserializer( Long.TYPE, 0L ) );
        deserializers.addDeserializer( Long.class, new LongDeserializer( Long.class, null ) );
        context.addDeserializers( deserializers );
    }

    static class LongDeserializer extends StdScalarDeserializer<Long> {

        private NumberDeserializers.LongDeserializer deserializer;

        public LongDeserializer( Class<Long> cls, Long nullValue ) {
            super( cls );
            deserializer = new NumberDeserializers.LongDeserializer( cls, nullValue );
        }

        @Override
        public Long deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {
            return p.hasToken( JsonToken.VALUE_STRING ) ?
                Numbers.parseLongWithUnits( p.getText().trim() )
                : deserializer.deserialize( p, ctxt );
        }
    }
}
