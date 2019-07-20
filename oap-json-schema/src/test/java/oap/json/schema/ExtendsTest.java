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

import org.testng.annotations.Test;

public class ExtendsTest extends AbstractSchemaTest {
    @Test
    public void extendsSchema() {
        String schema = "{"
            + "type: object,"
            + "extends: \"schema/test2.json\","
            + "properties: {}"
            + "}";

        String schema2 = "{"
            + "type:object,"
            + "properties: {"
            + "  a: {"
            + "    type: string"
            + "  }"
            + "}"
            + "}";

        assertOk( schema, "{'a': 'test'}", ( url ) -> schema2, false );
        assertFailure( schema, "{'a': 1}",
            "/a: instance type is number, but allowed type is string",
            ( url ) -> schema2
        );
    }

    @Test
    public void mergeInnerObject() {
        String schema = "{"
            + "type: object,"
            + "additionalProperties: false,"
            + "extends: test2,"
            + "properties: {"
            + "  o: {"
            + "    type:object,"
            + "    properties: {"
            + "      a1: {type:string}"
            + "    }"
            + "}"
            + "}"
            + "}";

        String schema2 = "{"
            + "type:object,"
            + "properties: {"
            + "  o: {"
            + "    type:object,"
            + "    properties: {"
            + "      a2: {type:string}"
            + "    }"
            + "}"
            + "}"
            + "}";

        assertOk( schema, "{'o': {'a1':'test'}}", ( url ) -> schema2, false );
        assertOk( schema, "{'o': {'a2':'test'}}", ( url ) -> schema2, false );
    }

    @Test
    public void mergeInnerArray() {
        String schema = "{"
            + "type: object,"
            + "additionalProperties: false,"
            + "extends: test2,"
            + "properties: {"
            + "  o: {"
            + "    type:array,"
            + "    items {"
            + "      type: object,"
            + "      properties {"
            + "        a1: {type:string}"
            + "      }"
            + "    }"
            + "}"
            + "}"
            + "}";

        String schema2 = "{"
            + "type:object,"
            + "properties: {"
            + "  o: {"
            + "    type:array,"
            + "    items {"
            + "      type: object,"
            + "      properties {"
            + "        a2: {type:string}"
            + "      }"
            + "    }"
            + "}"
            + "}"
            + "}";

        assertOk( schema, "{'o': [{'a1':'test'}]}", ( url ) -> schema2, false );
        assertOk( schema, "{'o': [{'a2':'test'}]}", ( url ) -> schema2, false );
    }

    @Test
    public void mergeInnerSchema() {
        String schema = "{"
            + "type: object,"
            + "additionalProperties: false,"
            + "extends: test2,"
            + "properties: {"
            + "  o: {"
            + "    type:array,"
            + "    items {"
            + "      type: object,"
            + "      extends: test11,"
            + "      properties {"
            + "        a1: {type:string}"
            + "      }"
            + "    }"
            + "}"
            + "}"
            + "}";

        String schema2 = "{"
            + "type:object,"
            + "properties: {"
            + "  o: {"
            + "    type:array,"
            + "    items {"
            + "      type: object,"
            + "      extends: test22,"
            + "      properties {"
            + "        a2: {type:string}"
            + "      }"
            + "    }"
            + "}"
            + "}"
            + "}";

        String schema11 = "{"
            + "type:object,"
            + "extends: test22,"
            + "properties: {"
            + "  a11: {type:string}"
            + "}"
            + "}";

        String schema22 = "{"
            + "type:object,"
            + "properties: {"
            + "  a21: {type:string}"
            + "}"
            + "}";

        SchemaStorage func = ( url ) -> {
            switch( url ) {
                case "test2":
                    return schema2;
                case "test11":
                    return schema11;
                case "test22":
                    return schema22;
                default:
                    throw new IllegalAccessError();
            }
        };

        assertOk( schema, "{'o': [{'a1':'test'}]}", func, false );
        assertOk( schema, "{'o': [{'a2':'test'}]}", func, false );
        assertOk( schema, "{'o': [{'a11':'test'}]}", func, false );
        assertOk( schema, "{'o': [{'a21':'test'}]}", func, false );

        assertFailure( schema, "{'o': [{'unknown':'test'}]}", "/o/0: additional properties are not permitted [unknown]", func );
    }
}
