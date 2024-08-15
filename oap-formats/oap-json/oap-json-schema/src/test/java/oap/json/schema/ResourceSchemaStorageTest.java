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

package oap.json.schema;

import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.json.testng.JsonAsserts.assertJson;

public class ResourceSchemaStorageTest {
    @Test
    public void testGet() {
        String schema = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema.conf" );
        String schema2 = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema" );
        String schema3 = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema2" );

        String expected = """
            {
            "type": "object",
              "properties": {
                 "a": {
                   "type": "string"
                }
              }
            }""";

        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema ) ) ).isEqualTo( expected );
        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema2 ) ) ).isEqualTo( expected );
        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema3 ) ) ).isEqualTo( expected );
    }

    @Test
    public void testGetWithExtends() {
        String schema = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema-1" );

        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema ) ) )
            .isEqualTo( """
                {
                  "type": "object",
                  "properties": {
                    "a": {
                      "type": "string"
                    },
                    "b": {
                      "type": "integer"
                    },
                    "j": {
                      "type": "integer"
                    },
                    "y": {
                      "type": "integer"
                    }
                  }
                }""" );
    }
}
