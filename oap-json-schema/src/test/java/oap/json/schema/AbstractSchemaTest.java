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
import org.apache.commons.lang3.NotImplementedException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSchemaTest extends AbstractTest {
   protected static final SchemaStorage NO_STORAGE = new NoStorage();

   protected static SchemaAST schema( String schema ) {
      return JsonValidatorFactory.schemaFromString( schema, NO_STORAGE ).schema;
   }

   protected static void assertOk( String schema, String json ) {
      assertOk( schema, json, NO_STORAGE, false );
   }

   protected static Object assertOk( String schema, String json, SchemaStorage storage, boolean ignoreRequiredDefault ) {
      final Object obj = Binder.json.unmarshal( Object.class, json );
      List<String> result =
         JsonValidatorFactory.schemaFromString( schema, storage )
            .validate( obj, ignoreRequiredDefault );
      if( !result.isEmpty() ) throw new AssertionError( String.join( "\n", result ) );

      return obj;
   }

   protected static void assertFailure( String schema, String json, String error ) {
      assertFailure( schema, json, error, NO_STORAGE );
   }

   protected static void assertFailure( String schema, String json, String error, SchemaStorage storage ) {
      List<String> result =
         JsonValidatorFactory.schemaFromString( schema, storage )
            .validate( Binder.json.unmarshal( Object.class, json ), false );
      if( result.isEmpty() ) Assert.fail( json + " -> " + error );
      assertThat( result ).containsOnly( error );
   }

   @BeforeMethod
   @Override
   public void beforeMethod() {
      super.beforeMethod();

      JsonValidatorFactory.reset();
   }

   private static class NoStorage implements SchemaStorage {
      @Override
      public String get( String name ) {
         throw new NotImplementedException( "" );
      }
   }
}
