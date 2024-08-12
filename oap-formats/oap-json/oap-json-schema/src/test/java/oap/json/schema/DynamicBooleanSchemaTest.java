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

public class DynamicBooleanSchemaTest extends AbstractSchemaTest {
    @Test
    public void eq() {
        var schema = """
            {
                additionalProperties = false
                type = object
                properties = {
                    a {
                        type = string
                        enabled = true
                    }
                    settings {
                        additionalProperties = false
                        type = object
                        properties = {
                            c {
                                type = string
                                enabled = {
                                    json-path = a
                                    eq = b
                                }
                            }
                        }
                    }
                    b {
                        type = string
                        enabled = {
                            json-path = a
                            eq = b
                        }
                    }
                }
            }
                """;

        assertOk( schema, "{'a':'1'}" );
        assertOk( schema, "{'a':'b'}" );
        assertOk( schema, "{'a':'b', 'b':'b'}" );
        assertOk( schema, "{'a':'b', 'settings':{'c':'2'} }" );

        assertFailure( schema, "{'settings':{'c':'2'} }", "/settings: additional properties are not permitted [c]" );
        assertFailure( schema, "{'b':'2' }", "additional properties are not permitted [b]" );
        assertFailure( schema, "{'a':'1', 'b':'b'}", "additional properties are not permitted [b]" );
    }

    @Test
    public void ne() {
        var schema = "{additionalProperties: false, type: object, properties: {"
            + "a.type=string,b:{type = string, enabled: {json-path:a, ne=b}}"
            + "}}";

        assertOk( schema, "{'a':'1'}" );
        assertOk( schema, "{'a':'b'}" );
        assertOk( schema, "{'a':'1', 'b':'b'}" );

        assertFailure( schema, "{'a':'b', 'b':'b'}", "additional properties are not permitted [b]" );
    }

    @Test
    public void in() {
        var schema = "{additionalProperties: false, type: object, properties: {"
            + "a.type=string,b:{type = string, enabled: {json-path:a, in=[a,b]}}"
            + "}}";

        assertOk( schema, "{'a':'1'}" );
        assertOk( schema, "{'a':'b'}" );
        assertOk( schema, "{'a':'a', 'b':'b'}" );
        assertOk( schema, "{'a':'b', 'b':'b'}" );

        assertFailure( schema, "{'a':'c', 'b':'b'}", "additional properties are not permitted [b]" );
    }

    @Test
    public void testNin() {
        var schema = "{additionalProperties: false, type: object, properties: {"
            + "a.type=string,b:{type = string, enabled: {json-path:a, nin=[a,b]}}"
            + "}}";

        assertOk( schema, "{'a':'a'}" );
        assertOk( schema, "{'a':'b'}" );
        assertOk( schema, "{'a':'1', 'b':'b'}" );

        assertFailure( schema, "{'a':'a', 'b':'b'}", "additional properties are not permitted [b]" );
    }
}
