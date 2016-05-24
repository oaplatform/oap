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

package oap.json.schema._dictionary;

import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Dictionaries;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryLeaf;
import oap.dictionary.DictionaryNotFoundError;
import oap.json.schema.*;
import oap.util.Either;
import oap.json.schema.JsonPath;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.util.Lists;

import java.util.List;
import java.util.Optional;

/**
 * Created by Igor Petrenko on 12.04.2016.
 */
@Slf4j
public class DictionaryJsonValidator extends JsonSchemaValidator<DictionarySchemaAST> {
   @Override
   public List<String> validate( JsonValidatorProperties properties, DictionarySchemaAST schema, Object value ) {
      try {
         Dictionary dictionary = Dictionaries.getCachedDictionary( schema.name );

         if( schema.parent.isPresent() ) {
            Optional<Object> parentValue = Lists.headOpt(
               new JsonPath( schema.parent.get().path, properties.path )
                  .traverse( properties.rootJson )
            );
            if( !parentValue.isPresent() )
               return Lists.of( properties.error( "required property is missing" ) );

            Optional<DictionaryLeaf> child = dictionary.getValue( parentValue.get().toString() );
            if( !child.isPresent() )
               return Lists.of( properties.error( "instance does not match any member of the enumeration " + dictionary.ids() ) );

            dictionary = child.get();
         }

         if( !dictionary.containsValueWithId( String.valueOf( value ) ) )
            return Lists.of( properties.error( "instance does not match any member of the enumeration " + dictionary.ids() ) );

         return Lists.empty();
      } catch( DictionaryNotFoundError e ) {
         return Lists.of( properties.error( "dictionary not found" ) );
      }
   }

   @Override
   public DictionarySchemaASTWrapper parse( JsonSchemaParserContext context ) {
      DictionarySchemaASTWrapper wrapper = context.createWrapper( DictionarySchemaASTWrapper::new );

      wrapper.common = node( context ).asCommon();
      wrapper.name = node( context ).asString( "name" ).optional();
      wrapper.parent = node( context ).asMap( "parent" ).optional()
         .flatMap( m -> Optional.ofNullable( ( String ) m.get( "json-path" ) ) );

      return wrapper;
   }
}
