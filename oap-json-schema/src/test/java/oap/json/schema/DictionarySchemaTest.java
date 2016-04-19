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

public class DictionarySchemaTest extends AbstractSchemaTest {
    @Test
    public void testDictionary() {
        String schema = "{type: dictionary, name: dict}";

        vOk( schema, "null" );
        vOk( schema, "'test1'" );
        vOk( schema, "'test2'" );

        vFail( schema, "'test4'", "instance does not match any member of the enumeration [test1,test2,test3]" );
    }

    @Test
    public void testUnknownDictionary() {
        String schema = "{type: dictionary, name: unknown}";

        vFail( schema, "'test4'", "dictionary not found" );
    }

    @Test(enabled = false)
    public void testHierarchical() {
        String schema = "{type: object, properties: {" +
            "parent: {type: dictionary, name: dict-h}, " +
            "child: {type: dictionary, parent: {json-path: parent}}" +
            "}}";

        vOk( schema, "{'parent': 'p1'}" );
        vOk( schema, "{'parent': 'p2'}" );
        vOk( schema, "{'parent': 'p1', 'child':'c11'}" );
        vOk( schema, "{'parent': 'p1', 'child':'c12'}" );
        vOk( schema, "{'parent': 'p2', 'child':'c21'}" );

        vFail( schema, "{'parent': 'p1', 'child':'oops'}", "instance does not match any member of the enumeration [c11,c12]" );
    }
}
