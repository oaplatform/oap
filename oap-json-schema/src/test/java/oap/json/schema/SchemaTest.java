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

import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class SchemaTest extends AbstractSchemaTest {
    @Test
    public void requiredNull() {
        String schema = "{type: boolean, required: true}";

        assertFailure( schema, "null", "required property is missing" );
    }

    @Test
    public void requiredNullIgnoreRequiredDefault() {
        String schema = "{type: boolean, required: true}";

        assertOk( schema, "null", true );
    }

    @Test
    public void requiredPropertyNull() {
        String schema = "{type: object, properties: {a: {type: boolean, required: true}}}";

        assertFailure( schema, "{'a':null}", "/a: required property is missing" );
    }

    @Test
    public void requiredPropertyEmpty() {
        String schema = "{type: object, properties: {a: {type: boolean, required: true}}}";

        assertFailure( schema, "{}", "/a: required property is missing" );
    }

    @Test
    public void defaultValue() {
        String schema = "{type: boolean, default: true}";

        assertOk( schema, "null" );
    }

    @Test
    public void defaultIgnoreRequiredDefault() {
        String schema = "{type: boolean, default: true}";

        assertOk( schema, "null", true );
    }

    @Test
    public void fixDefault() {
        String schema = "{type: object, properties: {a: {type: boolean, default: true}}}";

        assertThat( assertOk( schema, "{}", true ) ).isEqualTo( Maps.of( __( "a", true ) ) );
    }

    @Test
    public void anyType() {
        String schema = "{type: any}";

        assertOk( schema, "null" );
        assertOk( schema, "1" );
        assertOk( schema, "{}" );
        assertOk( schema, "[]" );
    }

}
