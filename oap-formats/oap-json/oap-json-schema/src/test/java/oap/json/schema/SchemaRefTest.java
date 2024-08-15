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

public class SchemaRefTest extends AbstractSchemaTest {
    @Test
    public void extendsSchema() {
        String schema = """
            {
              type = object
              additionalProperties = false
              properties {
                field1 = {"$ref" = "/schema/test2" }
              }
            }""";

        String schema2 = """
            {
              type = object
              additionalProperties = false
              properties {
                a {
                  type = string
                }
              }
            }""";

        assertOk( schema, "{'field1': {'a': 'test'}}", _ -> schema2, false );
        assertFailure( schema, "{'field1': {'a': 1}}",
            _ -> schema2, "/field1/a: instance type is number, but allowed type is string"
        );
    }

    @Test
    public void extendsSchemaArray() {
        String schema = """
            {
              type = object
              additionalProperties = false
              properties {
                list {
                  type = array
                  additionalProperties = false
                  items { "$ref" = "/schema/test2" }
                }
              }
            }""";

        String schema2 = """
            {
              type = object
              additionalProperties = false
              properties {
                a {
                  type = string
                }
              }
            }""";

        assertOk( schema, "{'list': [{'a': 'test'}]}", _ -> schema2, false );
        assertFailure( schema, "{'list': [{'a': 1}]}",
            _ -> schema2, "/list/0/a: instance type is number, but allowed type is string"
        );
    }
}
