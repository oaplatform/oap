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

public class EnumSchemaTest extends AbstractSchemaTest {
    @Test
    public void testStaticEnum() {
        String schema = "{type: string, enum: [test, test1]}";

        assertOk( schema, "null" );
        assertOk( schema, "'test'" );
        assertOk( schema, "'test1'" );

        assertFailure( schema, "'test2'", "instance does not match any member resolve the enumeration [test, test1]" );
    }

    @Test
    public void testDynamicEnumPathSingleton() {
        String schema = "{" +
            "type:object," +
            "properties:{" +
            "  a:{" +
            "    type:string," +
            "  }," +
            "  b:{" +
            "    type: string, " +
            "    enum: {json-path:a}" +
            "  }" +
            "}" +
            "}";

        assertOk( schema, "{'b':null}" );
        assertOk( schema, "{'a':'test', 'b':'test'}" );

        assertFailure( schema, "{'a':'test', 'b':'test2'}", "/b: instance does not match any member resolve the enumeration [test]" );
    }

    @Test
    public void testDynamicEnumPathListObjects() {
        String schema = "{" +
            "type:object," +
            "properties:{" +
            "  a:{" +
            "    type:array," +
            "    items: {" +
            "      type:object," +
            "      properties:{" +
            "        c: {type:string}" +
            "      }" +
            "    }" +
            "  }," +
            "  b:{" +
            "    type: string, " +
            "    enum: {json-path:a.c}" +
            "  }" +
            "}" +
            "}";

        assertOk( schema, "{'b':null}" );
        assertOk( schema, "{'a':[{'c':'test'}], 'b':'test'}" );

        assertFailure( schema, "{'a':[{'c':'test'}], 'b':'test2'}", "/b: instance does not match any member resolve the enumeration [test]" );
    }

    @Test
    public void testDynamicEnumPathList() {
        String schema = "{" +
            "type:object," +
            "properties:{" +
            "  a:{" +
            "    type:array," +
            "    items: {" +
            "      type:string" +
            "    }" +
            "  }," +
            "  b:{" +
            "    type: string, " +
            "    enum: {json-path:a}" +
            "  }" +
            "}" +
            "}";

        assertOk( schema, "{'b':null}" );
        assertOk( schema, "{'a':['test'], 'b':'test'}" );

        assertFailure( schema, "{'a':['test'], 'b':'test2'}", "/b: instance does not match any member resolve the enumeration [test]" );
    }
}
