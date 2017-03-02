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

import static oap.json.schema.AbstractSchemaTest.NO_STORAGE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 02.03.2017.
 */
public class SchemaPathTest {
    @Test
    public void testTraverse() throws Exception {
        String schema = "{additionalProperties = true, type: object, properties: {a: {type: boolean, required: true}}}";

        final JsonValidatorFactory validator = JsonValidatorFactory.schemaFromString( schema, NO_STORAGE );

        assertThat( SchemaPath.traverse( validator.schema, "properties.b" ).schema ).isEmpty();
        assertThat( SchemaPath.traverse( validator.schema, "b" ).schema ).isEmpty();
        assertThat( SchemaPath.traverse( validator.schema, "a" ).schema ).isPresent();
        assertThat( SchemaPath.traverse( validator.schema, "a" ).additionalProperties ).contains( true );
        assertThat( SchemaPath.traverse( validator.schema, "a" ).schema.get().common.schemaType ).isEqualTo( "boolean" );
    }

    @Test
    public void testTraverseArray() throws Exception {
        String schema = "{type: array, items { type = object, properties: {a: {type: array, items {type = boolean, required: true}}}}}";

        final JsonValidatorFactory validator = JsonValidatorFactory.schemaFromString( schema, NO_STORAGE );

        assertThat( SchemaPath.traverse( validator.schema, "properties.b" ).schema ).isEmpty();
        assertThat( SchemaPath.traverse( validator.schema, "b" ).schema ).isEmpty();
        assertThat( SchemaPath.traverse( validator.schema, "a" ).schema ).isPresent();
        assertThat( SchemaPath.traverse( validator.schema, "a" ).schema.get().common.schemaType ).isEqualTo( "boolean" );
    }

}