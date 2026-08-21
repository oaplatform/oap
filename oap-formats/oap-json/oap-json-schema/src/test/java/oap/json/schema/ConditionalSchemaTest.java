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

public class ConditionalSchemaTest extends AbstractSchemaTest {
    @Test
    public void ifThenOnStringType() {
        String schema = "{"
            + "type: string, "
            + "if: {maxLength: 2}, "
            + "then: {enum: [US, CA]}"
            + "}";

        assertOk( schema, "'US'" );
        assertOk( schema, "'HELLO'" );
        assertFailure( schema, "'XX'", "instance of 'XX' does not match any member resolve the enumeration [US, CA]" );
    }

    @Test
    public void allOfOnNumberType() {
        String schema = "{"
            + "type: double, "
            + "allOf: [ {minimum: 1}, {maximum: 10} ]"
            + "}";

        assertOk( schema, "5" );
        assertFailure( schema, "20", "number 20.0 is greater than the required maximum 10.0" );
    }

    @Test
    public void typelessConditionalDoesNotInferObjectType() {
        String schema = "{"
            + "minLength: 2, "
            + "if: {maxLength: 4}, "
            + "then: {enum: [aa, bb]}"
            + "}";

        assertOk( schema, "'aa'" );
        assertFailure( schema, "'cc'", "instance of 'cc' does not match any member resolve the enumeration [aa, bb]" );
    }
}
