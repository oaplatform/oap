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

public class PartialSchemaTest extends AbstractSchemaTest {
    @Test
    public void objectPropertiesEnabledDynamic() {
        String schema = "{"
            + "  additionalProperties: false, "
            + "  type: object, "
            + "  properties {"
            + "    v1.type = string,"
            + "    v2 {"
            + "      type = array,"
            + "      items {"
            + "        type = object,"
            + "        properties {"
            + "          o2 {type = string},"
            + "          v2 {"
            + "            type = string,"
            + "            enabled = {json-path: v1,eq: test}"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

        assertPartialOk( schema, "{'v2':[]}", "{}", "v2.items" );
        assertPartialOk( schema, "{'v1':'10', 'v2':[]}", "{'o2':'10'}", "v2.items" );
        assertPartialOk( schema, "{'v1':'test', 'v2':[]}", "{'o2':'10', 'v2':'ttt'}", "v2.items" );
        assertPartialFailure( schema, "{'v1':'20', 'v2':[]}", "{'v2':'ttt'}", "v2.items",
            "additional properties are not permitted [v2]" );
    }

    @Test
    public void innerObjectPath() {
        String schema = "{"
            + "  additionalProperties: false, "
            + "  type: object, "
            + "  properties {"
            + "    v1.type = string,"
            + "    v2 {"
            + "      type = array,"
            + "      items {"
            + "        type = object,"
            + "        properties {"
            + "          o2 {type = string},"
            + "          v2 {"
            + "            type = string,"
            + "            enabled = {json-path: v2.items.o2,eq: test}"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

        assertPartialOk( schema, "{'v2':[]}", "{}", "v2.items" );
        assertPartialOk( schema, "{'v1':'10', 'v2':[]}", "{'o2':'10'}", "v2.items" );
        assertPartialOk( schema, "{'v1':'10', 'v2':[]}", "{'o2':'test', 'v2':'ttt'}", "v2.items" );
        assertPartialFailure( schema, "{'v1':'20', 'v2':[]}", "{'v2':'ttt'}", "v2.items",
            "additional properties are not permitted [v2]" );
    }
}
