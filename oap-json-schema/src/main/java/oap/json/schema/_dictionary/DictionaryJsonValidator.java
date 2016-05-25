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
import oap.json.schema.JsonPath;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaPath;
import oap.util.Lists;
import oap.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static oap.json.schema.SchemaPath.rightTrimItems;

/**
 * Created by Igor Petrenko on 12.04.2016.
 */
@Slf4j
public class DictionaryJsonValidator extends JsonSchemaValidator<DictionarySchemaAST> {
   private Result<List<Dictionary>, List<String>> validate( JsonValidatorProperties properties, Optional<DictionarySchemaAST> schemaOpt, List<Dictionary> dictionaries ) {
      if(!schemaOpt.isPresent()) return Result.success( dictionaries );

      final DictionarySchemaAST schema = schemaOpt.get();

      final Result<List<Dictionary>, List<String>> cd = validate( properties, schema.parent, dictionaries );

      if( !cd.isSuccess() ) return cd;

      List<Object> parentValues = new JsonPath( rightTrimItems( schema.path ), properties.path )
         .traverse( properties.rootJson );

      if( parentValues.isEmpty() )
         return Result.failure( Lists.of( properties.error( "required property is missing" ) ) );

      final ArrayList<Dictionary> cDict = new ArrayList<>();

      for( Object parentValue : parentValues ) {
         List<DictionaryLeaf> children = cd.successValue
            .stream()
            .map( d -> d.getValue( parentValue.toString() ) )
            .filter( Optional::isPresent )
            .map( Optional::get ).collect( toList() );
         if( children.isEmpty() )
            return Result.failure( Lists.of(
               properties.error( "instance does not match any member resolve the enumeration " + printIds( cd.successValue ) )
            ) );

         cDict.addAll( children );
      }

      return Result.success( cDict );
   }

   @Override
   public List<String> validate( JsonValidatorProperties properties, DictionarySchemaAST schema, Object value ) {
      try {
         List<Dictionary> dictionaries = Lists.of( Dictionaries.getCachedDictionary( schema.name ) );

         final Result<List<Dictionary>, List<String>> result = validate( properties, schema.parent, dictionaries );

         if( !result.isSuccess() ) return result.failureValue;

         if( !result.successValue.stream().filter( d -> d.containsValueWithId( String.valueOf( value ) ) ).findAny().isPresent() )
            return Lists.of( properties.error( "instance does not match any member resolve the enumeration " + printIds( result.successValue ) ) );

         return Lists.empty();
      } catch( DictionaryNotFoundError e ) {
         return Lists.of( properties.error( "dictionary not found" ) );
      }
   }

   private String printIds( List<Dictionary> dictionaries ) {
      return dictionaries
         .stream()
         .flatMap( d -> d.ids().stream() )
         .distinct()
         .collect( toList() )
         .toString();
   }

   @Override
   public DictionarySchemaASTWrapper parse( JsonSchemaParserContext context ) {
      DictionarySchemaASTWrapper wrapper = context.createWrapper( DictionarySchemaASTWrapper::new );

      wrapper.common = node( context ).asCommon();
      wrapper.name = node( context ).asString( "name" ).optional();
      wrapper.parent = node( context ).asMap( "parent" ).optional()
         .flatMap( m -> Optional.ofNullable( ( String ) m.get( "json-path" ) ).map( jp -> SchemaPath.resolve( context.rootPath, jp ) ) );

      return wrapper;
   }
}
