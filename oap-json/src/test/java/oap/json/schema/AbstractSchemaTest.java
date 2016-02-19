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
import oap.testng.AbstractTest;
import oap.util.Either;
import org.apache.commons.lang3.NotImplementedException;
import org.testng.Assert;

import java.util.List;

public abstract class AbstractSchemaTest extends AbstractTest {
    protected static final SchemaStorage NO_STORAGE = new NoStorage();

    protected static Object vOk( String schema, String json ) {
        return vOk( schema, json, NO_STORAGE, false );
    }

    protected static Object vOk( String schema, String json, SchemaStorage storage, boolean ignore_required_default ) {
        Either<List<String>, ?> result =
            JsonValidatorFactory.schemaFromString( schema, storage ).validate( Binder.json.unmarshal( Object.class, json ),
                ignore_required_default );
        if( result.isLeft() ) throw new AssertionError( String.join( "\n", result.left().get() ) );
        else return result.right().get();
    }

    protected static void vFail( String schema, String json, String error ) {
        vFail( schema, json, error, NO_STORAGE );
    }

    protected static void vFail( String schema, String json, String error, SchemaStorage storage ) {
        Either<List<String>, ?> result =
            JsonValidatorFactory.schemaFromString( schema, storage ).validate( Binder.json.unmarshal( Object.class, json ), false );
        if( result.isRight() ) Assert.fail( json + " -> " + error );
        List<String> errors = result.left().get();
        Assert.assertEquals( errors.size(), 1 );
        Assert.assertEquals( errors.get( 0 ), error );
    }

    private static class NoStorage implements SchemaStorage {
        @Override
        public String get( String name ) {
            throw new NotImplementedException( "" );
        }
    }
}
