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

package oap.json.schema.validator.dictionary;

import oap.json.schema.AbstractSchemaTest;
import oap.json.schema.SchemaStorage;
import org.testng.annotations.Test;

public class DictionarySchemaTest extends AbstractSchemaTest {
    @Test
    public void dictionary() {
        String schema = "{type: dictionary, name: dict}";

        assertOk( schema, "null" );
        assertOk( schema, "'test1'" );
        assertOk( schema, "'test2'" );

        assertFailure( schema, "'test4'", "instance of 'test4' does not match any member resolve the enumeration [test1, test2, test3]" );
    }

    @Test
    public void unknownDictionary() {
        String schema = "{type: dictionary, name: unknown}";

        assertFailure( schema, "'test4'", "dictionary unknown not found" );
    }

    @Test
    public void hierarchical() {
        String schema = "{type: object, properties: {"
            + "parent: {type: dictionary, name: dict-h}, "
            + "child: {type: dictionary, parent: {json-path: parent}}"
            + "}}";

        assertOk( schema, "{'parent': 'p1'}" );
        assertOk( schema, "{'parent': 'p2'}" );
        assertOk( schema, "{'parent': 'p1', 'child':'c11'}" );
        assertOk( schema, "{'parent': 'p1', 'child':'c12'}" );
        assertOk( schema, "{'parent': 'p2', 'child':'c21'}" );

        assertFailure( schema, "{'parent': 'p1', 'child':'oops'}", "/child: instance of 'oops' does not match any member resolve the enumeration [c11, c12]" );
    }

    @Test
    public void parentArray() {
        String schema = "{type: object, properties: {"
            + "parent: {type:array, items:{type: dictionary, name: dict-h}}, "
            + "child: {type:array, items:{type: dictionary, parent: {json-path: parent.items}}}"
            + "}}";

        assertOk( schema, "{'parent': ['p1','p2']}" );
        assertOk( schema, "{'parent': ['p1'], 'child':['c11']}" );
        assertOk( schema, "{'parent': ['p1', 'p2'], 'child':['c11','c21']}" );
    }

    @Test
    public void hierarchicalArray() {
        String schema = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertOk( schema, "{'a':[{'parent': 'p1'}]}" );
        assertOk( schema, "{'a':[{'parent': 'p2'}]}" );
        assertOk( schema, "{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p1', 'child':'c12'}]}" );

        assertFailure( schema, "{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p2', 'child':'c12'}]}", "/a/1/child: instance of 'c12' does not match any member resolve the enumeration [c21, c22, c23]" );
    }

    @Test
    public void hierarchicalArray2() {
        String schema = "{type: object, properties: {"
            + "a:{"
            + "  type: object,"
            + "  properties: {"
            + "    parent: {"
            + "      type: array,"
            + "      items: {type: dictionary, name: dict-h}"
            + "    },"
            + "    child: {"
            + "      type: array,"
            + "      items: {type: dictionary, parent: {json-path: a.parent.items}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertOk( schema, "{'a':{'parent': ['p1']}}" );
        assertOk( schema, "{'a':{'parent': ['p2']}}" );
        assertOk( schema, "{'a':{'parent': ['p1','p2'], 'child':['c21', 'c12']}}" );
    }

    @Test
    public void hierarchicalArray3() {
        String schema = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}},"
            + "      child2: {type: dictionary, parent: {json-path: a.items.child}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertOk( schema, "{'a':[{'parent': 'p1', 'child':'c11', 'child2':'c111'}]}" );
    }

    @Test
    public void hierarchicalArrayRequiredFalse() {
        String schema = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}},"
            + "      child2: {type: dictionary, parent: {json-path: a.items.child}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertOk( schema, "{'a':[{'child2':'c111'}]}" );
    }

    @Test
    public void hierarchicalArrayRequiredTrue() {
        String schema = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}, required: true},"
            + "      child2: {type: dictionary, parent: {json-path: a.items.child}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertFailure( schema, "{'a':[{'child2':'c111'}]}", "/a/0/child: required property is missing" );
    }

    @Test
    public void extendsHierarchicalArray() {
        String schema = "{"
            + "type: object,"
            + "properties: {"
            + "  p: {"
            + "    type: object,"
            + "    extends: \"schema/test2.json\","
            + "    properties: {}"
            + "  }"
            + "}"
            + "}";

        String schema2 = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        assertOk( schema, "{'p':{'a':[{'parent': 'p1'}]}}", url -> schema2, false );
        assertOk( schema, "{'p':{'a':[{'parent': 'p2'}]}}", url -> schema2, false );
        assertOk( schema, "{'p':{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p1', 'child':'c12'}]}}",
            url -> schema2, false );

        assertFailure( schema, "{'p':{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p2', 'child':'c12'}]}}",
            url -> schema2, "/p/a/1/child: instance of 'c12' does not match any member resolve the enumeration [c21, c22, c23]" );
    }

    @Test
    public void extendsHierarchicalArray2() {
        String schema = "{"
            + "type: object,"
            + "properties: {"
            + "  p: {"
            + "    type: object,"
            + "    extends: \"schema2\","
            + "    properties: {}"
            + "  }"
            + "}"
            + "}";

        String schema2 = "{"
            + "type: object,"
            + "properties: {"
            + "  p2: {"
            + "    type: object,"
            + "    extends: \"schema3\","
            + "    properties: {}"
            + "  }"
            + "}"
            + "}";

        String schema3 = "{type: object, properties: {"
            + "a:{"
            + "  type: array,"
            + "  items: {"
            + "    type: object,"
            + "    properties: {"
            + "      parent: {type: dictionary, name: dict-h}, "
            + "      child: {type: dictionary, parent: {json-path: a.items.parent}}"
            + "    }"
            + "  }"
            + "}"
            + "}}";

        final SchemaStorage schemaF = url -> switch( url ) {
            case "schema2" -> schema2;
            case "schema3" -> schema3;
            default -> throw new IllegalArgumentException( url );
        };

        assertOk( schema, "{'p':{'p2':{'a':[{'parent': 'p1'}]}}}", schemaF, false );
        assertOk( schema, "{'p':{'p2':{'a':[{'parent': 'p2'}]}}}", schemaF, false );
        assertOk( schema, "{'p':{'p2':{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p1', 'child':'c12'}]}}}",
            schemaF, false );

        assertFailure( schema, "{'p':{'p2':{'a':[{'parent': 'p1', 'child':'c11'},{'parent': 'p2', 'child':'c12'}]}}}",
            schemaF, "/p/p2/a/1/child: instance of 'c12' does not match any member resolve the enumeration [c21, c22, c23]" );
    }
}
