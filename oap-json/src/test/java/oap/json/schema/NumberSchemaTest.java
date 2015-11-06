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

public class NumberSchemaTest extends AbstractSchemaTest {
    @Test
    public void testInt() {
        String schema = "{\"type\": \"integer\"}}";
        vOk( schema, "10" );
        vOk( schema, "-10" );
        vOk( schema, "null" );

        vFail( schema, "10.0", "instance is of type number, which is none of the allowed primitive types ([integer])" );
        vFail( schema, "\"10\"",
                "instance is of type string, which is none of the allowed primitive types ([integer])" );
    }

    @Test
    public void testLong() {
        String schema = "{\"type\": \"long\"}}";
        vOk( schema, "10" );
        vOk( schema, "-10" );
        vOk( schema, "null" );

        vFail( schema, "10.0", "instance is of type number, which is none of the allowed primitive types ([long])" );
        vFail( schema, "\"10\"",
                "instance is of type string, which is none of the allowed primitive types ([long])" );
    }

    @Test
    public void testDouble() {
        String schema = "{\"type\": \"double\"}}";
        vOk( schema, "-10.0" );
        vOk( schema, "324.23" );
        vOk( schema, "null" );

        vFail( schema, "\"10\"",
                "instance is of type string, which is none of the allowed primitive types ([double])" );
    }

    @Test
    public void test_minimum() {
        String schema = "{\"type\": \"integer\", \"minimum\": 2}}";

        vOk( schema, "2" );
        vOk( schema, "20" );

        vFail( schema, "1", "number is lower than the required minimum 2.0" );
        vFail( schema, "-3", "number is lower than the required minimum 2.0" );
    }

    @Test
    public void test_exclusiveMinimum() {
        String schema = "{\"type\": \"integer\", \"minimum\": 2, \"exclusiveMinimum\": true}}";

        vOk( schema, "3" );

        vFail( schema, "2", "number is not strictly greater than the required minimum 2.0" );
    }

    @Test
    public void test_maximum() {
        String schema = "{\"type\": \"integer\", \"maximum\": 3}}";

        vOk( schema, "2" );
        vOk( schema, "-100" );

        vFail( schema, "4", "number is greater than the required maximum 3.0" );
    }

    @Test
    public void test_exclusiveMaximum() {
        String schema = "{\"type\": \"integer\", \"maximum\": 3, \"exclusiveMaximum\": true}}";

        vOk( schema, "2" );

        vFail( schema, "3", "number is not strictly lower than the required maximum 3.0" );
    }
}
