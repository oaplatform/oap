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
package oap.json.schema.validator.array;

import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;

import java.util.ArrayList;
import java.util.List;

public class ArrayJsonValidator extends JsonSchemaValidator<ArraySchemaAST> {
   public ArrayJsonValidator() {
      super( "array" );
   }

   @Override
   public List<String> validate( JsonValidatorProperties properties, ArraySchemaAST schema,
                                 Object value ) {
      if( !( value instanceof List<?> ) ) return typeFailed( properties, schema, value );

      List<?> arrayValue = ( List<?> ) value;
      List<String> errors = new ArrayList<>();

      schema.minItems.filter( minItems -> arrayValue.size() < minItems )
         .ifPresent( minItems -> errors.add( properties.error( "array " + arrayValue + " has less than minItems elements " + minItems ) ) );

      schema.maxItems.filter( maxItems -> arrayValue.size() > maxItems )
         .ifPresent( maxItems -> errors.add( properties.error( "array " + arrayValue + " has more than maxItems elements " + maxItems ) ) );

      for( int i = 0; i < arrayValue.size(); i++ )
         errors.addAll( properties.validator.apply( properties.withAdditionalProperties( schema.additionalProperties ).withPath( String.valueOf( i ) ),
            schema.items, arrayValue.get( i ) ) );

      return errors;
   }

   @Override
   public ArraySchemaASTWrapper parse( JsonSchemaParserContext context ) {
      final ArraySchemaASTWrapper wrapper = context.createWrapper( ArraySchemaASTWrapper::new );

      wrapper.common = node( context ).asCommon();
      wrapper.additionalProperties = node( context ).asBoolean( ADDITIONAL_PROPERTIES ).optional();
      wrapper.minItems = node( context ).asInt( "minItems" ).optional();
      wrapper.maxItems = node( context ).asInt( "maxItems" ).optional();
      wrapper.idField = node( context ).asString( "id" ).optional();
      wrapper.items = node( context ).asAST( "items", context ).required();

      return wrapper;
   }
}
