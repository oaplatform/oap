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

public class PrimitiveTypeTest extends AbstractSchemaTest {
    @Test
    public void bool() {
        String schema = "{\"type\": \"boolean\"}";

        assertOk( schema, "true" );
        assertOk( schema, "false" );
        assertOk( schema, "null" );
        assertFailure( schema, "\"1\"",
            "instance type is string, but allowed type is boolean" );
        assertFailure( schema, "{}",
            "instance type is object, but allowed type is boolean" );
        assertFailure( schema, "\"true\"",
            "instance type is string, but allowed type is boolean" );
    }

    @Test
    public void string() {
        String schema = "{\"type\": \"string\"}";

        assertOk( schema, "\"test\"" );
        assertOk( schema, "null" );
        assertFailure( schema, "1",
            "instance type is number, but allowed type is string" );
        assertFailure( schema, "{}",
            "instance type is object, but allowed type is string" );
    }

    @Test
    public void stringMinLength() {
        String schema = "{\"type\": \"string\", \"minLength\": 2}";

        assertOk( schema, "\"te\"" );
        assertFailure( schema, "\"t\"", "string t is shorter than minLength 2" );
    }

    @Test
    public void stringMaxLength() {
        String schema = "{\"type\": \"string\", \"maxLength\": 2}";

        assertOk( schema, "\"te\"" );
        assertFailure( schema, "\"tes\"", "string tes is longer than maxLength 2" );
    }

    @Test
    public void stringPattern() {
        String schema = "{\"type\": \"string\", \"pattern\": \"a+\"}";

        assertOk( schema, "\"aa\"" );
        assertFailure( schema, "\"b\"", "string b does not match specified regex a+" );
        assertFailure( schema, "\"aab\"", "string aab does not match specified regex a+" );
        assertFailure( schema, "\"bbaaabb\"", "string bbaaabb does not match specified regex a+" );
    }

    @Test
    public void stringDate() {
        String schema = "{\"type\": \"string\", \"pattern\": \"([12]\\\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\\\d|3[01]))\"}";
        assertOk( schema, "\"2019-09-21\"" );
    }

    @Test
    public void date() {
        String schema = "{\"type\": \"date\"}";

        assertOk( schema, "\"2016-01-01T00:00:00.000Z\"" );
        assertOk( schema, "\"2016-01-01T00:00:00\"" );
        assertOk( schema, "null" );
        assertFailure( schema, "\"2016-01-01TT00:00:00\"",
            "Invalid format: \"2016-01-01TT00:00:00\" is malformed at \"T00:00:00\"" );
        assertFailure( schema, "{}",
            "instance type is object, but allowed type is date" );
    }
}
