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

package oap.ws.openapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.core.util.VersionUtil;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.StringWriter;

public class OpenApiModule extends JacksonModule {

    private static final Version VERSION = VersionUtil.parseVersion(
        "3.0.3", "io.xenoss.platform", "openapi" );

    @Override
    public String getModuleName() {
        return "openapi";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule( SetupContext context ) {
        final SimpleSerializers serializers = new SimpleSerializers();
        serializers.addSerializer( OpenAPI.class, new OpenApiSerializer() );
        context.addSerializers( serializers );

        final SimpleDeserializers deserializers = new SimpleDeserializers();
        deserializers.addDeserializer( OpenAPI.class, new OpenApiDeserializer() );
        context.addDeserializers( deserializers );
    }

    private static class OpenApiSerializer extends StdSerializer<OpenAPI> {

        protected OpenApiSerializer() {
            super( OpenAPI.class );
        }

        @Override
        public void serialize( OpenAPI openAPI, JsonGenerator jsonGenerator, SerializationContext serializerProvider ) {
            StringWriter stringWriter = new StringWriter();
            Json.mapper().writeValue( stringWriter, openAPI );

            jsonGenerator.writeRawValue( stringWriter.toString() );
        }
    }

    private static class OpenApiDeserializer extends StdDeserializer<OpenAPI> {
        protected OpenApiDeserializer() {
            super( OpenAPI.class );
        }

        @Override
        public OpenAPI deserialize( JsonParser jp, DeserializationContext ctxt ) {
            var mapper = Json.mapper();
            ObjectNode obj = ( ObjectNode ) ctxt.readTree( jp );

            return mapper.treeToValue( obj, OpenAPI.class );
        }
    }
}
