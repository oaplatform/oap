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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface JsonSchemaValidator<A extends SchemaAST> {
    Either<List<String>, Object> validate( JsonValidatorProperties properties, A schema, Object value );

    default String getType( Object object ) {
        if( object instanceof Boolean ) return "boolean";
        else if( object instanceof String ) return "string";
        else if( object instanceof Integer || object instanceof Long || object instanceof Float ||
            object instanceof Double ) return "number";
        else if( object instanceof Map<?, ?> ) return "object";
        else if( object instanceof Collection<?> ) return "array";
        else
            throw new Error( "Unknown type " + (object != null ? object.getClass() : "<NULL???>") + ", fix me!!!" );
    }

    SchemaAST parse( JsonSchemaParserProperties properties );

    default SchemaAST defaultParse( JsonSchemaParserProperties properties ) {
        SchemaAST.CommonSchemaAST common = node( properties ).asCommon();

        return new SchemaAST( common );
    }

    default NodeParser node( JsonSchemaParserProperties properties ) {
        return new NodeParser( properties );
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
        private final JsonSchemaParserProperties properties;

        public NodeParser( JsonSchemaParserProperties properties ) {
            this.properties = properties;
        }

        public PropertyParser<Integer> asInt( String property ) {
            return new PropertyParser<>(
                Optional.ofNullable( (Long) properties.node.get( property ) ).map( Long::intValue ) );
        }

        public PropertyParser<Double> asDouble( String property ) {
            return new PropertyParser<>(
                Optional.ofNullable( (Number) properties.node.get( property ) ).map( Number::doubleValue ) );
        }

        public PropertyParser<Boolean> asBoolean( String property ) {
            return new PropertyParser<>(
                Optional.ofNullable( (Boolean) properties.node.get( property ) ) );
        }

        public PropertyParser<String> asString( String property ) {
            return new PropertyParser<>( Optional.ofNullable( (String) properties.node.get( property ) ) );
        }

        public PropertyParser<SchemaAST> asAST( String property ) {
            return new PropertyParser<>(
                Optional.ofNullable( properties.node.get( property ) ).map( properties.mapParser::apply ) );
        }

        @SuppressWarnings( "unchecked" )
        private Optional<Function<Object, List<Object>>> toEnum( Object anEnum ) {
            if( anEnum == null ) {
                return Optional.empty();
            } else if( anEnum instanceof List<?> ) {
                Function<Object, List<Object>> func = ( obj ) -> (List<Object>) anEnum;
                return Optional.of( func );
            } else if( anEnum instanceof Map<?, ?> ) {
                String jsonPath = (String) ((Map<?, ?>) anEnum).get( "json-path" );

                Function<Object, List<Object>> func = ( obj ) -> new EnumPath( jsonPath ).traverse( obj );

                return Optional.of( func );
            } else throw new ValidationSyntaxException( "Unknown enum type " + anEnum.getClass() );
        }

        @SuppressWarnings( "unchecked" )
        public SchemaAST.CommonSchemaAST asCommon() {
            Optional<Boolean> required = Optional.ofNullable( (Boolean) properties.node.get( "required" ) );
            Optional<Object> defaultValue = Optional.ofNullable( properties.node.get( "default" ) );
            Object anEnum = properties.node.get( "enum" );

            return new SchemaAST.CommonSchemaAST( properties.schemaType, required, defaultValue, toEnum( anEnum ) );
        }

        @SuppressWarnings( "unchecked" )
        public PropertyParser<LinkedHashMap<String, SchemaAST>> asMapAST( String property ) {
            LinkedHashMap<String, SchemaAST> p = new LinkedHashMap<>();

            Optional<Map<Object, Object>> map =
                Optional.ofNullable( (Map<Object, Object>) properties.node.get( property ) );

            map.ifPresent(
                m -> m.forEach( ( key, value ) -> p.put( (String) key, properties.mapParser.apply( value ) ) )
            );

            return new PropertyParser<>( map.map( v -> p ) );
        }
    }
}
