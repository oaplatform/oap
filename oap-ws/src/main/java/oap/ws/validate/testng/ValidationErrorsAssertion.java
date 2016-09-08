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
package oap.ws.validate.testng;

import oap.ws.validate.ValidationErrors;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationErrorsAssertion extends AbstractAssert<ValidationErrorsAssertion, ValidationErrors> {

   protected ValidationErrorsAssertion( ValidationErrors actual ) {
      super( actual, ValidationErrorsAssertion.class );
   }


   public ValidationErrorsAssertion hasCode( int code ) {
      assertThat( this.actual.code ).isEqualTo( code );
      return this;
   }

   public ValidationErrorsAssertion containsErrors( String... errors ) {
      assertThat( this.actual.errors ).contains( errors );
      return this;
   }

   public ValidationErrorsAssertion isError( int code, String error ) {
      return hasCode( code ).containsErrors( error );
   }

   public ValidationErrorsAssertion isFailed() {
      assertThat( this.actual.isFailed() ).isTrue();
      return this;
   }

   public ValidationErrorsAssertion isNotFailed() {
      assertThat( this.actual.isFailed() ).isFalse();
      return this;
   }
}
