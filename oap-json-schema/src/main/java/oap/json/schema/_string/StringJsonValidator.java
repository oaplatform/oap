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
package oap.json.schema._string;

import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;

import java.util.ArrayList;
import java.util.List;

public class StringJsonValidator extends JsonSchemaValidator<StringSchemaAST> {
   @Override
   public List<String> validate( JsonValidatorProperties properties, StringSchemaAST schema, Object value ) {
      if( !( value instanceof String ) ) return typeFailed( properties, schema, value );

      String strValue = ( String ) value;

      List<String> errors = new ArrayList<>();

      schema.minLength
         .filter( minLength -> strValue.length() < minLength )
         .ifPresent( minLength -> errors.add( properties.error( "string " + strValue + " is shorter than minLength " + minLength ) ) );

      schema.maxLength
         .filter( maxLength -> strValue.length() > maxLength )
         .ifPresent( maxLength -> errors.add( properties.error( "string " + strValue + " is longer than maxLength " + maxLength ) ) );

      schema.pattern
         .filter( pattern -> !pattern.matcher( strValue ).matches() )
         .ifPresent( pattern -> errors.add( properties.error( "string " + strValue + " does not match specified regex " + pattern ) ) );

      return errors;
   }

   @Override
   public StringSchemaASTWrapper parse( JsonSchemaParserContext context ) {
      final StringSchemaASTWrapper wrapper = context.createWrapper( StringSchemaASTWrapper::new );
      wrapper.common = node( context ).asCommon();
      wrapper.minLength = node( context ).asInt( "minLength" ).optional();
      wrapper.maxLength = node( context ).asInt( "maxLength" ).optional();
      wrapper.pattern = node( context ).asPattern( "pattern" ).optional();

      return wrapper;
   }
}
