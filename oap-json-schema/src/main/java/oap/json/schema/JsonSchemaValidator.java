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

import oap.util.Either;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public interface JsonSchemaValidator<A extends SchemaAST<A>> {
   Either<List<String>, Object> validate( JsonValidatorProperties properties, A schema, Object value );

   default String getType( Object object ) {
      if( object instanceof Boolean ) return "boolean";
      else if( object instanceof String ) return "string";
      else if( object instanceof Integer || object instanceof Long || object instanceof Float ||
         object instanceof Double ) return "number";
      else if( object instanceof Map<?, ?> ) return "object";
      else if( object instanceof Collection<?> ) return "array";
      else
         throw new Error( "Unknown type " + ( object != null ? object.getClass() : "<NULL???>" ) + ", fix me!!!" );
   }

   SchemaASTWrapper<A, ? extends SchemaASTWrapper> parse( JsonSchemaParserContext context );

   default DefaultSchemaASTWrapper defaultParse( JsonSchemaParserContext context ) {
      final DefaultSchemaASTWrapper wrapper = new DefaultSchemaASTWrapper( context.getId() );
      wrapper.common = node( context ).asCommon();

      return wrapper;
   }

   default NodeParser node( JsonSchemaParserContext context ) {
      return new NodeParser( context );
   }

   class PropertyParser<A> {
      private final Optional<A> value;

      public PropertyParser( Optional<A> value ) {
         this.value = value;
      }

      public Optional<A> optional() {
         return value;
      }

      public A required() {
         return value.get();
      }
   }

   class NodeParser {
      private final JsonSchemaParserContext properties;

      public NodeParser( JsonSchemaParserContext properties ) {
         this.properties = properties;
      }

      public PropertyParser<Integer> asInt( String property ) {
         return new PropertyParser<>(
            Optional.ofNullable( ( Long ) properties.node.get( property ) ).map( Long::intValue ) );
      }

      public PropertyParser<Double> asDouble( String property ) {
         return new PropertyParser<>(
            Optional.ofNullable( ( Number ) properties.node.get( property ) ).map( Number::doubleValue ) );
      }

      public PropertyParser<Boolean> asBoolean( String property ) {
         return new PropertyParser<>(
            Optional.ofNullable( ( Boolean ) properties.node.get( property ) ) );
      }

      public PropertyParser<String> asString( String property ) {
         return new PropertyParser<>( Optional.ofNullable( ( String ) properties.node.get( property ) ) );
      }

      public PropertyParser<Map<?, ?>> asMap( String property ) {
         return new PropertyParser<>( Optional.ofNullable( ( Map<?, ?> ) properties.node.get( property ) ) );
      }

      public PropertyParser<Pattern> asPattern( String property ) {
         return new PropertyParser<>( Optional.ofNullable( ( String ) properties.node.get( property ) ).map( Pattern::compile ) );
      }

      public PropertyParser<SchemaASTWrapper> asAST( String property, JsonSchemaParserContext context ) {
         return new PropertyParser<>(
            Optional.ofNullable( context.node.get( property ) ).map( n -> {
               final JsonSchemaParserContext newContext = context.withNode( property, n );
               return newContext.ast.computeIfAbsent( newContext.getId(), ( id ) -> context.mapParser.apply( newContext ) );
            } ) );
      }

      @SuppressWarnings( "unchecked" )
      private Optional<Function<Object, List<Object>>> toEnum( Object anEnum ) {
         if( anEnum == null ) {
            return Optional.empty();
         } else if( anEnum instanceof List<?> ) {
            Function<Object, List<Object>> func = ( obj ) -> ( List<Object> ) anEnum;
            return Optional.of( func );
         } else if( anEnum instanceof Map<?, ?> ) {
            String jsonPath = ( String ) ( ( Map<?, ?> ) anEnum ).get( "json-path" );

            Function<Object, List<Object>> func = ( obj ) -> new JsonPath( jsonPath ).traverse( obj );

            return Optional.of( func );
         } else throw new ValidationSyntaxException( "Unknown enum type " + anEnum.getClass() );
      }

      @SuppressWarnings( "unchecked" )
      public SchemaAST.CommonSchemaAST asCommon() {
         Optional<Boolean> required = Optional.ofNullable( ( Boolean ) properties.node.get( "required" ) );
         Optional<Object> defaultValue = Optional.ofNullable( properties.node.get( "default" ) );
         Object anEnum = properties.node.get( "enum" );

         return new SchemaAST.CommonSchemaAST( properties.schemaType, required, defaultValue, toEnum( anEnum ) );
      }

      @SuppressWarnings( "unchecked" )
      public PropertyParser<LinkedHashMap<String, SchemaASTWrapper>> asMapAST( String property, JsonSchemaParserContext context ) {
         LinkedHashMap<String, SchemaASTWrapper> p = new LinkedHashMap<>();

         Optional<Map<Object, Object>> map =
            Optional.ofNullable( ( Map<Object, Object> ) context.node.get( property ) );

         map.ifPresent(
            m -> m.forEach( ( okey, value ) -> {
               final String key = ( String ) okey;
               final JsonSchemaParserContext newContext = context.withNode( key, value );
               final SchemaASTWrapper astw = newContext.ast.computeIfAbsent( newContext.getId(), ( id ) -> context.mapParser.apply( newContext ) );
               p.put( key, astw );
            } )
         );

         return new PropertyParser<>( map.map( v -> p ) );
      }
   }
}
