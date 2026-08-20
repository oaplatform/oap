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

package oap.json.schema.validator.object;

import oap.json.schema.AbstractSchemaTest;
import org.testng.annotations.Test;

import static oap.json.schema.ResourceSchemaStorage.INSTANCE;

public class ObjectSchemaTest extends AbstractSchemaTest {
    @Test
    public void object() {
        String schema = "{type: object, properties: {}}";

        assertOk( schema, "{}" );
        assertOk( schema, "null" );
        assertFailure( schema, "[]",
            "instance type is array, but allowed type is object" );
    }

    @Test
    public void objectWithField() {
        String schema = "{type: object, properties: {a: {type: string}}}";

        assertOk( schema, "{}" );
        assertOk( schema, "{'a': 'test'}" );
        assertFailure( schema, "{'a': 10}",
            "/a: instance type is number, but allowed type is string" );
    }

    @Test
    public void objectObjectWithField() {
        String schema = "{"
            + "type: object, "
            + "properties: {"
            + "  a: {"
            + "    type: object, "
            + "    properties: {"
            + "      a: {"
            + "        type: string"
            + "      }"
            + "    }"
            + "  }"
            + "}"
            + "}";

        assertOk( schema, "{}" );
        assertOk( schema, "{'a': {'a': 'test'}}" );
        assertFailure( schema, "{'a': {'a': true}}",
            "/a/a: instance type is boolean, but allowed type is string" );
    }

    @Test
    public void additionalPropertiesTrue() {
        String schema = "{type: object, properties: {a: {type: string}}}";

        assertOk( schema, "{}" );
        assertOk( schema, "{'b': 'test'}" );
    }

    @Test
    public void additionalPropertiesFalse() {
        String schema = "{additionalProperties: false, type: object, properties: {a: {type: string}}}";

        assertOk( schema, "{}" );
        assertFailure( schema, "{'b': 'test', 'c': 10}", "additional properties are not permitted [b, c]" );
    }

    @Test
    public void additionalPropertiesFalseInheritance() {
        String schema = "{"
            + "additionalProperties: false, "
            + "type: object, "
            + "properties: {"
            + " a: {"
            + "  type: object,"
            + "  properties: {"
            + "   b: {"
            + "    type: string"
            + "   }"
            + "  }"
            + " }"
            + "}"
            + "}";

        assertOk( schema, "{}" );
        assertFailure( schema, "{'a': {'b': 'test', 'c': 10}}", "/a: additional properties are not permitted [c]" );
    }

    @Test
    public void additionalPropertiesFalseDoesNotLeakIntoAllOfBranch() {
        String schema = "{"
            + "additionalProperties: false, "
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "allOf: [ { required = [a] } ]"
            + "}";

        assertOk( schema, "{'a': 'x'}" );
    }

    @Test
    public void requiredArrayOk() {
        String schema = "{type: object, properties: {a: {type: string}}, required: [a]}";

        assertOk( schema, "{'a': 'x'}" );
    }

    @Test
    public void requiredArrayMissing() {
        String schema = "{type: object, properties: {a: {type: string}}, required: [a]}";

        assertFailure( schema, "{}", "/a: required property is missing" );
    }

    @Test
    public void requiredArrayNullValue() {
        String schema = "{type: object, properties: {a: {type: string}}, required: [a]}";

        assertFailure( schema, "{'a': null}", "/a: required property is missing" );
    }

    @Test
    public void requiredArrayMultiple() {
        String schema = "{type: object, properties: {a: {type: string}, b: {type: string}}, required: [a, b]}";

        assertFailure( schema, "{'a': 'x'}", "/b: required property is missing" );
    }

    @Test
    public void requiredArrayIgnoreRequiredDefault() {
        String schema = "{type: object, properties: {a: {type: string}}, required: [a]}";

        assertOk( schema, "{}", true );
    }

    @Test
    public void requiredArrayDoesNotCollideWithPerFieldRequired() {
        String schema = "{type: object, properties: {a: {type: string, required: true}, b: {type: string}}, required: [b]}";

        assertFailure( schema, "{}", INSTANCE, "/a: required property is missing", "/b: required property is missing" );
    }

    @Test
    public void ifThenOk() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}, b: {type: string}}, "
            + "if: {type: object, properties: {a: {type: string}}, required: [a]}, "
            + "then: {type: object, properties: {b: {type: string}}, required: [b]}"
            + "}";

        assertOk( schema, "{'a': 'x', 'b': 'y'}" );
    }

    @Test
    public void ifThenElseThenBranch() {
        String schema = "{"
            + "type: object, "
            + "properties: {country: {type: string}, postalCode: {type: string}}, "
            + "if: {type: object, properties: {country: {type: string, enum: [US]}}, required: [country]}, "
            + "then: {type: object, properties: {postalCode: {type: string}}, required: [postalCode]}, "
            + "else: {type: object, properties: {postalCode: {type: string}}}"
            + "}";

        assertFailure( schema, "{'country': 'US'}", "/postalCode: required property is missing" );
    }

    @Test
    public void ifThenElseElseBranch() {
        String schema = "{"
            + "type: object, "
            + "properties: {country: {type: string}, postalCode: {type: string}}, "
            + "if: {type: object, properties: {country: {type: string, enum: [US]}}, required: [country]}, "
            + "then: {type: object, properties: {postalCode: {type: string}}, required: [postalCode]}, "
            + "else: {type: object, properties: {postalCode: {type: string}}, required: [postalCode]}"
            + "}";

        assertFailure( schema, "{'country': 'CA'}", "/postalCode: required property is missing" );
    }

    @Test
    public void ifWithoutThenOrElse() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "if: {type: object, properties: {a: {type: string}}, required: [a]}"
            + "}";

        assertOk( schema, "{'a': 'x'}" );
        assertOk( schema, "{}" );
    }

    @Test
    public void ifFailsNoElseBranch() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}, b: {type: string}}, "
            + "if: {type: object, properties: {a: {type: string}}, required: [a]}, "
            + "then: {type: object, properties: {b: {type: string}}, required: [b]}"
            + "}";

        assertOk( schema, "{}" );
    }

    @Test
    public void allOfOk() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}, b: {type: string}}, "
            + "allOf: [ {type: object, properties: {a: {type: string}}, required: [a]}, {type: object, properties: {b: {type: string}}, required: [b]} ]"
            + "}";

        assertOk( schema, "{'a': 'x', 'b': 'y'}" );
    }

    @Test
    public void allOfFailure() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}, b: {type: string}}, "
            + "allOf: [ {type: object, properties: {a: {type: string}}, required: [a]}, {type: object, properties: {b: {type: string}}, required: [b]} ]"
            + "}";

        assertFailure( schema, "{'a': 'x'}", "/b: required property is missing" );
    }

    @Test
    public void anyOfOk() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "anyOf: [ {type: object, properties: {a: {type: string, enum: [foo]}}, required: [a]}, {type: object, properties: {a: {type: string, enum: [bar]}}, required: [a]} ]"
            + "}";

        assertOk( schema, "{'a': 'bar'}" );
    }

    @Test
    public void anyOfFailure() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "anyOf: [ {type: object, properties: {a: {type: string, enum: [foo]}}, required: [a]}, {type: object, properties: {a: {type: string, enum: [bar]}}, required: [a]} ]"
            + "}";

        assertFailure( schema, "{'a': 'baz'}", "instance does not match any schema in anyOf" );
    }

    @Test
    public void oneOfOk() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "oneOf: [ {type: object, properties: {a: {type: string, enum: [foo]}}, required: [a]}, {type: object, properties: {a: {type: string, enum: [bar]}}, required: [a]} ]"
            + "}";

        assertOk( schema, "{'a': 'bar'}" );
    }

    @Test
    public void oneOfFailureNoneMatch() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "oneOf: [ {type: object, properties: {a: {type: string, enum: [foo]}}, required: [a]}, {type: object, properties: {a: {type: string, enum: [bar]}}, required: [a]} ]"
            + "}";

        assertFailure( schema, "{'a': 'baz'}", "instance must match exactly one schema in oneOf, matched 0" );
    }

    @Test
    public void oneOfFailureMultipleMatch() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "oneOf: [ {type: object, properties: {a: {type: string}}}, {type: object, properties: {a: {type: string}}} ]"
            + "}";

        assertFailure( schema, "{'a': 'x'}", "instance must match exactly one schema in oneOf, matched 2" );
    }

    @Test
    public void notOk() {
        String schema = """
            {
                type: object
                properties: {a: {type: string}}
                not: {type: object, properties: {a: {type: string, enum: [forbidden]}}, required: [a]}
            }""";

        assertOk( schema, "{'a': 'allowed'}" );
    }

    @Test
    public void notFailure() {
        String schema = """
            {
                type: object
                properties: {a: {type: string}}
                not: {type: object, properties: {a: {type: string, enum: [forbidden]}}, required: [a]}
            }""";

        assertFailure( schema, "{'a': 'forbidden'}", "instance must not be valid against the schema in not" );
    }

    @Test
    public void testAllOf() {
        String schema1 = """
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
              properties {
                enabled.type = boolean
                a.type = integer
                b.type = integer
                type {
                  type = string
                  enum = [B1, A2]
                }
                c.type = integer
                trafficLeak {
                  type = double
                  minimum = 0.0
                  maximum = 1.0
                }
              }

              required = [enabled, type]

              allOf:
                - if {properties {enabled.const: true, type.const: A2}}
                  then {required = [a]}
                - if {properties {enabled.const: true, type.const: B1}}
                  then {required = [b]}
                - if {properties {enabled.const: true}}
                  then {required = [c]}
            }""";

        assertOk( schema1, "{'field1': {'enabled': true, 'type': 'A2', 'c': 1, 'a': 2 }}", _ -> schema2, false );
        assertFailure( schema1, "{'field1': {'type': 'A2', 'c': 1, 'a': 2 }}", _ -> schema2, "/field1/enabled: required property is missing" );
        assertFailure( schema1, "{'field1': {'enabled': true, 'type': 'A2', 'c': 1 }}", _ -> schema2, "/field1/a: required property is missing" );
        assertFailure( schema1, "{'field1': {'enabled': true, 'type': 'A2', 'a': 2 }}", _ -> schema2, "/field1/c: required property is missing" );
    }

    @Test
    public void typelessObjectInference() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {type: string}}, "
            + "not: {properties: {a: {type: string, enum: [forbidden]}}, required: [a]}"
            + "}";

        assertOk( schema, "{'a': 'allowed'}" );
        assertFailure( schema, "{'a': 'forbidden'}", "instance must not be valid against the schema in not" );
    }

    @Test
    public void typelessAnyInference() {
        String schema = "{"
            + "type: object, "
            + "properties: {a: {const: true}}"
            + "}";

        assertOk( schema, "{'a': true}" );
        assertFailure( schema, "{'a': 'nope'}", "/a: instance does not equal const value 'true'" );
    }
}
