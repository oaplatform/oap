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

public class ErrorMessageSchemaTest extends AbstractSchemaTest {
    @Test
    public void typeOverride() {
        String schema = "{"
            + "type: object, "
            + "properties: { foo: {type: string} }, "
            + "errorMessage: { type: \"Invalid type\" }"
            + "}";

        assertFailure( schema, "123", "Invalid type" );
    }

    @Test
    public void maxItemsOverride() {
        String schema = "{"
            + "type: object, "
            + "properties: { "
            + "  foo: { type: array, items: {type: string}, maxItems: 3, errorMessage: { maxItems: \"MaxItem must be 3 only\" } } "
            + "}"
            + "}";

        assertFailure( schema, "{'foo': ['a','b','c','d']}", "MaxItem must be 3 only" );
    }

    @Test
    public void requiredPerPropertyOverride() {
        String schema = "{"
            + "type: object, "
            + "properties: { foo: {type: double}, bar: {type: string} }, "
            + "required: [foo, bar], "
            + "errorMessage: { required: { foo: \"{0}: ''foo'' is required\" } }"
            + "}";

        assertFailure( schema, "{'bar': 'x'}", "foo: 'foo' is required" );
    }

    @Test
    public void requiredPerPropertyFallsBackToDefaultWhenNotListed() {
        String schema = "{"
            + "type: object, "
            + "properties: { foo: {type: double}, bar: {type: string} }, "
            + "required: [foo, bar], "
            + "errorMessage: { required: { foo: \"{0}: ''foo'' is required\" } }"
            + "}";

        assertFailure( schema, "{'foo': 1}", "/bar: required property is missing" );
    }

    @Test
    public void stringKeywordOverride() {
        String schema = "{"
            + "type: object, "
            + "properties: { "
            + "  code: { type: string, minLength: 5, errorMessage: { minLength: \"Code too short\" } } "
            + "}"
            + "}";

        assertFailure( schema, "{'code': 'ab'}", "Code too short" );
    }

    @Test
    public void noErrorMessageKeepsDefaultText() {
        String schema = "{"
            + "type: string, "
            + "minLength: 5"
            + "}";

        assertFailure( schema, "'ab'", "string ab is shorter than minLength 5" );
    }

    @Test
    public void typeOverrideInsideThenBranch() {
        String schema = "{"
            + "type: object, "
            + "properties: { "
            + "  a: {type: string}, "
            + "  b: {type: string} "
            + "}, "
            + "if: { properties: { a: {const: \"test\"} } }, "
            + "then: { properties: { b: {type: integer, errorMessage: { type: \"{0}: a == test\" }} } }"
            + "}";

        assertFailure( schema, "{'a': 'test', 'b': 'somestring'}", "b: a == test" );
    }
}
