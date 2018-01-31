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

import java.util.Optional;

import static oap.json.schema.ResourceStorage.INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Igor Petrenko on 19.04.2016.
 */
public class SchemaWrapperPathTest extends AbstractSchemaTest {
    private SchemaASTWrapper wrapper( String schema, SchemaStorage storage ) {
        return JsonSchema.schemaFromString( schema ).parse( schema, storage );
    }

    @Test
    public void traverseObject() {
        String schema = "{type: object, properties: {a: {type: object, properties: {b: {type: string}}}}}";

        Optional<SchemaASTWrapper> traverse = new SchemaWrapperPath( "a.b" ).traverse( wrapper( schema, INSTANCE ) );
        assertThat( traverse ).hasValueSatisfying( s -> assertThat( s.common.schemaType ).isEqualTo( "string" ) );
    }

    @Test
    public void traverseArray() throws Exception {
        String schema = "{type: object, properties: {a: {type: array, items: {type: object, properties: {b: {type: string}}}}}}";

        Optional<SchemaASTWrapper> traverse = new SchemaWrapperPath( "a.items.b" ).traverse( wrapper( schema, INSTANCE ) );
        assertThat( traverse ).hasValueSatisfying( s -> assertThat( s.common.schemaType ).isEqualTo( "string" ) );
    }

    @Test
    public void traverseNotFound() throws Exception {
        String schema = "{type: object, properties: {a: {type: string}}}";

        Optional<SchemaASTWrapper> traverse = new SchemaWrapperPath( "a.b" ).traverse( wrapper( schema, INSTANCE ) );
        assertThat( traverse ).isEmpty();
    }
}