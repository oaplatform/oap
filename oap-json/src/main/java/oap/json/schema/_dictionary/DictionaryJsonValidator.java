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
import oap.json.Binder;
import oap.json.schema.JsonSchemaParserProperties;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaAST;
import oap.util.Either;
import oap.util.Lists;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Igor Petrenko on 12.04.2016.
 */
@Slf4j
public class DictionaryJsonValidator implements JsonSchemaValidator<DictionarySchemaAST> {
   private final static HashMap<String, Dictionary> dictionaries = new HashMap<>();

   public DictionaryJsonValidator() {
   }

   public DictionaryJsonValidator( Path path ) {
      log.debug( "dictionary path = {}", path );
      try( DirectoryStream<Path> dirStream = java.nio.file.Files.newDirectoryStream( path ) ) {
         dirStream.forEach( p -> {
            log.trace( "found {}", p );
            final Dictionary dictionary = Binder.hoconWithoutSystemProperties.unmarshal( Dictionary.class, p );
            final String baseName = FilenameUtils.getBaseName( p.toString() );
            log.debug( "add dictionary {}", baseName );
            dictionaries.put( baseName, dictionary );
         } );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   @Override
   public Either<List<String>, Object> validate( JsonValidatorProperties properties, DictionarySchemaAST schema, Object value ) {
      final Dictionary dictionary = dictionaries.get( schema.name );

      if( dictionary == null ) return Either.left(
         Lists.of(
            properties.error( "dictionary not found" ) ) );

      if( dictionary.values.contains( value.toString() ) ) {
         return Either.right( value );
      } else {
         return Either.left( Collections.singletonList(
            properties.error( "instance does not match any member of the enumeration [" +
               String.join( ",", dictionary.values ) + "]"
            ) ) );
      }
   }

   @Override
   public SchemaAST parse( JsonSchemaParserProperties properties ) {
      SchemaAST.CommonSchemaAST common = node( properties ).asCommon();
      final String name = node( properties ).asString( "name" ).required();

      return new DictionarySchemaAST( common, name );
   }
}
