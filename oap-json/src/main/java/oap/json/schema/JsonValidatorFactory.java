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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import lombok.val;
import oap.io.Resources;
import oap.json.Binder;
import oap.util.Either;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class JsonValidatorFactory {
   private static final HashMap<String, JsonSchemaValidator> validators = new HashMap<>();

   private static org.slf4j.Logger logger = getLogger( JsonValidatorFactory.class );
   private static Map<String, JsonValidatorFactory> schemas = new ConcurrentHashMap<>();

   static {
      for( URL url : Resources.urls( "META-INF/oap-validator.conf" ) ) {
         val map = Binder.hoconWithoutSystemProperties.unmarshal( new TypeReference<Map<String, String>>() {}, url );
         map.forEach( ( name, clazz ) -> {
            try {
               validators.put( name, ( JsonSchemaValidator ) Class.forName( clazz ).newInstance() );
            } catch( InstantiationException | IllegalAccessException | ClassNotFoundException e ) {
               throw Throwables.propagate( e );
            }
         } );
      }
   }

   public final SchemaAST schema;
   private final SchemaStorage storage;

   private JsonValidatorFactory( String schema, SchemaStorage storage ) {
      this.storage = storage;
      this.schema = parse( schema );
   }

   public static JsonValidatorFactory schema( String url, SchemaStorage storage ) {
      return schemas.computeIfAbsent( url, u -> schemaFromString( storage.get( url ), storage ) );
   }

   public static JsonValidatorFactory schemaFromString( String schema, SchemaStorage storage ) {
      return new JsonValidatorFactory( schema, storage );
   }

   @SuppressWarnings( "unchecked" )
   private static Either<List<String>, Object> validate( JsonValidatorProperties properties, SchemaAST schema,
                                                         Object json ) {
      JsonSchemaValidator jsonSchemaValidator = validators.get( schema.common.schemaType );
      if( jsonSchemaValidator == null ) {
         if( logger.isTraceEnabled() ) logger.trace( "registered validators: " + validators.keySet() );
         throw new ValidationSyntaxException(
            "[schema:type]: unknown simple type [" + schema.common.schemaType + "]" );
      }

      if( json == null && !properties.ignore_required_default && schema.common.required.orElse( false ) )
         return Either.left( Collections.singletonList(
            properties.error( "required property is missing" )
         ) );
      else if( json == null ) return Either.right(
         schema.common.defaultValue.filter( dv -> !properties.ignore_required_default ).orElse( null )
      );
      else {
         Either result = jsonSchemaValidator.validate( properties, schema, json );
         return result
            .right()
            .flatMap( v -> schema.common.enumValue
               .filter( e -> !e.apply( properties.rootJson ).contains( v ) )
               .map( e ->
                  Either.left( Collections.singletonList(
                     properties.error( "instance does not match any member of the enumeration [" +
                        String.join( ",", e.apply( properties.rootJson ).stream()
                           .map( Object::toString )
                           .collect( Collectors.toList() )
                        ) + "]" )
                  ) ) )
               .orElse( result )
            );
      }
   }

   @SuppressWarnings( "unchecked" )
   public Either<List<String>, Object> validate( Object json, boolean ignore_required_default ) {
      JsonValidatorProperties properties = new JsonValidatorProperties(
         json,
         Optional.empty(),
         Optional.empty(),
         ignore_required_default,
         JsonValidatorFactory::validate
      );
      return validate( properties, schema, json );
   }

   private SchemaAST parse( Object mapObject ) {
      if( !( mapObject instanceof Map<?, ?> ) )
         throw new IllegalArgumentException( "object expected, but " + mapObject );
      Map<?, ?> map = ( Map<?, ?> ) mapObject;
      Object schemaType = map.get( "type" );
      if( schemaType instanceof String ) {
         JsonSchemaValidator<?> schemaParser = validators.get( schemaType );
         if( schemaParser != null ) {
            return schemaParser.parse(
               new JsonSchemaParserProperties(
                  map, ( String ) schemaType,
                  this::parse,
                  ( url ) -> JsonValidatorFactory.schema( url, storage ).schema
               ) );
         } else {
            if( logger.isTraceEnabled() ) logger.trace( "registered parsers: " + validators.keySet() );
            throw new ValidationSyntaxException(
               "[schema:type]: unknown simple type [" + schemaType + "]" );
         }
      } else {
         throw new UnknownTypeValidationSyntaxException(
            "Unknown type" + ( schemaType == null ? "nothing" : schemaType.getClass() )
         );
      }
   }

   private SchemaAST parse( String schema ) {
      return parse( ( Object ) Binder.hocon.unmarshal( Object.class, schema ) );
   }
}
