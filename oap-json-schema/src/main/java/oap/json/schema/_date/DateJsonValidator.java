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
package oap.json.schema._date;

import oap.json.schema.DefaultSchemaAST;
import oap.json.schema.DefaultSchemaASTWrapper;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Result;
import org.joda.time.DateTime;

import java.util.List;

public class DateJsonValidator extends JsonSchemaValidator<DefaultSchemaAST> {

   @Override
   public List<String> validate( JsonValidatorProperties properties, DefaultSchemaAST schema, Object value ) {
      if( !( value instanceof String ) ) return typeFailed( properties, schema, value );

      Result<DateTime, List<String>> result = Dates.parseDate( ( String ) value )
         .mapFailure( e -> Lists.of( e.getMessage() ) );
      return result.isSuccess() ? Lists.empty() : result.failureValue;
   }

   @Override
   public DefaultSchemaASTWrapper parse( JsonSchemaParserContext context ) {
      return defaultParse( context );
   }
}
