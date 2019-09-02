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

import oap.json.Binder;
import org.testng.Assert;

import java.util.List;

import static oap.json.schema.ResourceSchemaStorage.INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSchemaTest {
    protected static SchemaAST schema( String schema ) {
        return JsonSchema.schemaFromString( schema ).schema;
    }

    protected static void assertOk( String schema, String json ) {
        assertOk( schema, json, false );
    }

    protected static Object assertOk( String schema, String json, boolean ignoreRequiredDefault ) {
        return assertOk( schema, json, INSTANCE, ignoreRequiredDefault );
    }

    protected static Object assertOk( String schema, String json, SchemaStorage storage, boolean ignoreRequiredDefault ) {
        final Object obj = Binder.json.unmarshal( Object.class, json );
        List<String> result = JsonSchema.schemaFromString( schema, storage )
            .validate( obj, ignoreRequiredDefault );
        if( !result.isEmpty() ) throw new AssertionError( String.join( "\n", result ) );

        return obj;
    }

    protected static void assertPartialOk( String schema, String json, String partialJson, String path ) {
        final Object obj = Binder.json.unmarshal( Object.class, json );
        final Object partial = Binder.json.unmarshal( Object.class, partialJson );
        List<String> result = JsonSchema.schemaFromString( schema )
            .partialValidate( obj, partial, path, false );
        if( !result.isEmpty() ) throw new AssertionError( String.join( "\n", result ) );
    }

    protected static void assertFailure( String schema, String json, String error ) {
        assertFailure( schema, json, INSTANCE, error );
    }

    protected static void assertFailure( String schema, String json, SchemaStorage storage, String... error ) {
        List<String> result = JsonSchema.schemaFromString( schema, storage )
            .validate( Binder.json.unmarshal( Object.class, json ), false );
        if( result.isEmpty() ) Assert.fail( json + " -> " + error );
        assertThat( result ).containsOnly( error );
    }

    protected static void assertPartialFailure( String schema, String json, String partialJson,
                                                String path, String error ) {
        final Object root = Binder.json.unmarshal( Object.class, json );
        final Object partial = Binder.json.unmarshal( Object.class, partialJson );
        List<String> result = JsonSchema.schemaFromString( schema )
            .partialValidate( root, partial, path, false );
        if( result.isEmpty() ) Assert.fail( json + " -> " + error );
        assertThat( result ).containsOnly( error );
    }
}
