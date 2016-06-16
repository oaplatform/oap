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
import oap.util.Lists;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

   private JsonValidatorFactory( String schema, SchemaStorage storage ) {
      final JsonSchemaParserContext context = new JsonSchemaParserContext(
         "", null, "",
         JsonValidatorFactory::parse,
         ( rp, url ) -> parse( url, storage.get( url ), storage, rp ),
         "", "", new HashMap<>(), new HashMap<>() );

      this.schema = parse( schema, context ).unwrap( context );
   }

   public static JsonValidatorFactory schema( String url, SchemaStorage storage ) {
      return schemas.computeIfAbsent( url, u -> schemaFromString( storage.get( url ), storage ) );
   }

   public static JsonValidatorFactory schemaFromString( String schema, SchemaStorage storage ) {
      return new JsonValidatorFactory( schema, storage );
   }

   @SuppressWarnings( "unchecked" )
   private static List<String> validate( JsonValidatorProperties properties, SchemaAST schema, Object value ) {
      JsonSchemaValidator jsonSchemaValidator = validators.get( schema.common.schemaType );
      if( jsonSchemaValidator == null ) {
         logger.trace( "registered validators: " + validators.keySet() );
         throw new ValidationSyntaxException( "[schema:type]: unknown simple type [" + schema.common.schemaType + "]" );
      }

      if( value == null && !properties.ignoreRequiredDefault
         && schema.common.required.orElse( BooleanReference.FALSE ).apply( properties.rootJson, properties.path ) )
         return Lists.of( properties.error( "required property is missing" ) );
      else if( value == null ) return Lists.empty();
      else {
         List<String> errors = jsonSchemaValidator.validate( properties, schema, value );
         schema.common.enumValue
            .filter( e -> !e.apply( properties.rootJson ).contains( value ) )
            .ifPresent( e -> errors.add( properties.error( "instance does not match any member resolve the enumeration " + e.apply( properties.rootJson ) ) ) );
         return errors;
      }
   }

   public static void reset() {
      schemas.clear();
   }

   static SchemaASTWrapper parse( String schema, SchemaStorage storage ) {
      return parse( "", schema, storage, "" );
   }

   static SchemaASTWrapper parse( String schemaName, String schema, SchemaStorage storage, String rootPath ) {
      final JsonSchemaParserContext context = new JsonSchemaParserContext(
         schemaName,
         null, "",
         JsonValidatorFactory::parse,
         ( rp, url ) -> parse( url, storage.get( url ), storage, rp ),
         rootPath, "", new HashMap<>(), new HashMap<>() );
      return parse( schema, context );
   }

   private static SchemaASTWrapper parse( String schema, JsonSchemaParserContext context ) {
      return parse( context.withNode( "", Binder.hocon.unmarshal( Object.class, schema ) ) );
   }

   private static SchemaASTWrapper parse( JsonSchemaParserContext context ) {
      JsonSchemaValidator<?> schemaParser = validators.get( context.schemaType );
      if( schemaParser != null ) {
         return schemaParser.parse( context );
      } else {
         if( logger.isTraceEnabled() ) logger.trace( "registered parsers: " + validators.keySet() );
         throw new ValidationSyntaxException(
            "[schema:type]: unknown simple type [" + context.schemaType + "]" );
      }
   }

   @SuppressWarnings( "unchecked" )
   public List<String> validate( Object json, boolean ignore_required_default ) {
      JsonValidatorProperties properties = new JsonValidatorProperties(
         schema,
         json,
         Optional.empty(),
         Optional.empty(),
         ignore_required_default,
         JsonValidatorFactory::validate
      );
      return validate( properties, schema, json );
   }
}
