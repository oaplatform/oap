/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
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
    public void testBoolean() {
        String schema = "{\"type\": \"boolean\"}";

        vOk( schema, "true" );
        vOk( schema, "false" );
        vOk( schema, "null" );
        vFail( schema, "\"1\"",
            "instance is of type string, which is none of the allowed primitive types ([boolean])" );
        vFail( schema, "{}",
            "instance is of type object, which is none of the allowed primitive types ([boolean])" );
        vFail( schema, "\"true\"",
            "instance is of type string, which is none of the allowed primitive types ([boolean])" );
    }

    @Test
    public void testString() {
        String schema = "{\"type\": \"string\"}";

        vOk( schema, "\"test\"" );
        vOk( schema, "null" );
        vFail( schema, "1",
            "instance is of type number, which is none of the allowed primitive types ([string])" );
        vFail( schema, "{}",
            "instance is of type object, which is none of the allowed primitive types ([string])" );
    }

    @Test
    public void testString_minLength() {
        String schema = "{\"type\": \"string\", \"minLength\": 2}";

        vOk( schema, "\"te\"" );
        vFail( schema, "\"t\"", "string is shorter than minLength 2" );
    }

    @Test
    public void testString_maxLength() {
        String schema = "{\"type\": \"string\", \"maxLength\": 2}";

        vOk( schema, "\"te\"" );
        vFail( schema, "\"tes\"", "string is longer than maxLength 2" );
    }

    @Test
    public void testString_pattern() {
        String schema = "{\"type\": \"string\", \"pattern\": \"a+\"}";

        vOk( schema, "\"aa\"" );
        vFail( schema, "\"b\"", "string does not match specified regex a+" );
        vFail( schema, "\"aab\"", "string does not match specified regex a+" );
        vFail( schema, "\"bbaaabb\"", "string does not match specified regex a+" );
    }

    @Test
    public void test_date() {
        String schema = "{\"type\": \"date\"}";

        vOk( schema, "\"2016-01-01T00:00:00\"" );
        vOk( schema, "null" );
        vFail( schema, "\"2016-01-01TT00:00:00\"",
            "Invalid format: \"2016-01-01TT00:00:00\" is malformed at \"T00:00:00\"" );
        vFail( schema, "{}",
            "instance is of type object, which is none of the allowed primitive types ([date])" );
    }
}
