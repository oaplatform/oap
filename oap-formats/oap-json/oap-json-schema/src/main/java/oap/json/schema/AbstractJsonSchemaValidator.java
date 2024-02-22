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

import oap.util.Lists;
import oap.util.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class AbstractJsonSchemaValidator<A extends AbstractSchemaAST<A>> {
    public static final String JSON_PATH = "json-path";
    protected static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public final String type;

    protected AbstractJsonSchemaValidator( String type ) {
        this.type = type;
    }

    public static String getType( Object object ) {
        if( object instanceof Boolean ) return "boolean";
        else if( object instanceof String ) return "string";
        else if( object instanceof Number ) return "number";
        else if( object instanceof Map<?, ?> ) return "object";
        else if( object instanceof List<?> ) return "array";
        else
            throw new JsonSchemaException( "Unknown type " + ( object != null ? object.getClass() : "<NULL???>" ) + ", fix me!!!" );
    }

    public static List<String> typeFailed( JsonValidatorProperties properties, AbstractSchemaAST<?> schema, Object value ) {
        return Lists.of( properties.error( "instance type is " + getType( value )
            + ", but allowed type is " + schema.common.schemaType ) );
    }

    public static DefaultSchemaASTWrapper defaultParse( JsonSchemaParserContext context ) {
        DefaultSchemaASTWrapper wrapper = new DefaultSchemaASTWrapper( context.getId() );
        wrapper.common = node( context ).asCommon();

        return wrapper;
    }

    public static NodeParser node( JsonSchemaParserContext context ) {
        return new NodeParser( context );
    }

    public abstract List<String> validate( JsonValidatorProperties properties, A schema, Object value );

    public abstract AbstractSchemaASTWrapper<A> parse( JsonSchemaParserContext context );

    public static class PropertyParser<A> {
        private final Optional<A> value;
        private final JsonSchemaParserContext properties;
        private final String property;

        public PropertyParser( String property, JsonSchemaParserContext properties, Optional<A> value ) {
            this.property = property;
            this.properties = properties;
            this.value = value;
        }

        public Optional<A> optional() {
            return value;
        }

        public A required() {
            return value.orElseThrow( () -> new ValidationSyntaxException( properties.error( "'" + property + "' is required" ) ) );
        }
    }

    public static class NodeParser {
        private final JsonSchemaParserContext properties;

        public NodeParser( JsonSchemaParserContext properties ) {
            this.properties = properties;
        }

        public PropertyParser<Integer> asInt( String property ) {
            return new PropertyParser<>( property, properties,
                Optional.ofNullable( ( Long ) properties.node.get( property ) ).map( Long::intValue ) );
        }

        public PropertyParser<Double> asDouble( String property ) {
            return new PropertyParser<>( property, properties,
                Optional.ofNullable( ( Number ) properties.node.get( property ) ).map( Number::doubleValue ) );
        }

        public PropertyParser<Boolean> asBoolean( String property ) {
            return new PropertyParser<>( property, properties,
                Optional.ofNullable( ( Boolean ) properties.node.get( property ) ) );
        }

        public PropertyParser<String> asString( String property ) {
            return new PropertyParser<>( property, properties, Optional.ofNullable( ( String ) properties.node.get( property ) ) );
        }

        public <T extends Enum<T>> PropertyParser<T> asEnum( String property, Class<T> clazz ) {
            return new PropertyParser<>( property, properties,
                Optional.ofNullable( ( String ) properties.node.get( property ) ).map( v -> Enum.valueOf( clazz, v ) ) );
        }

        public PropertyParser<Map<?, ?>> asMap( String property ) {
            return new PropertyParser<>( property, properties, Optional.ofNullable( ( Map<?, ?> ) properties.node.get( property ) ) );
        }

        public PropertyParser<Pattern> asPattern( String property ) {
            return new PropertyParser<>( property, properties, Optional.ofNullable( ( String ) properties.node.get( property ) ).map( Pattern::compile ) );
        }

        public PropertyParser<AbstractSchemaASTWrapper> asAST( String property, JsonSchemaParserContext context ) {
            return new PropertyParser<>( property, properties,
                Optional.ofNullable( context.node.get( property ) ).map( n -> {
                    JsonSchemaParserContext newContext = context.withNode( property, n );
                    AbstractSchemaASTWrapper aw = context.mapParser.apply( newContext );
                    newContext.astW.putIfAbsent( aw.id, aw );
                    return aw;
                } ) );
        }

        @SuppressWarnings( "unchecked" )
        private Optional<EnumFunction> toEnum( Object anEnum ) {
            if( anEnum == null ) {
                return Optional.empty();
            } else if( anEnum instanceof List<?> ) {
                return Optional.of( new ListObjectEnumFunction( ( List<Object> ) anEnum ) );
            } else if( anEnum instanceof Map<?, ?> ) {
                Map<?, ?> map = ( Map<?, ?> ) anEnum;

                String jsonPath = ( String ) map.get( JSON_PATH );
                Function<Object, List<Object>> sourceFunc = obj -> new JsonPath( jsonPath ).traverse( obj );

                var of = getOperationFunction( map );

                Map filterMap = ( Map ) map.get( "filter" );
                if( filterMap != null ) {
                    Map source = ( Map ) filterMap.get( "source" );
                    String filterJsonPath = ( String ) source.get( JSON_PATH );
                    Function<Object, List<Object>> filterSourceFunc = obj -> new JsonPath( filterJsonPath ).traverse( obj );
                    var filterOf = getOperationFunction( filterMap );

                    return Optional.of( new FilteredEnumFunction( sourceFunc, of, Pair.__( filterSourceFunc, filterOf ) ) );
                } else {
                    return Optional.of( new FilteredEnumFunction( sourceFunc, of ) );
                }

            } else throw new ValidationSyntaxException( "Unknown enum type " + anEnum.getClass() );
        }

        private OperationFunction getOperationFunction( Map<?, ?> map ) {
            return OperationFunction.parse( map );
        }

        public AbstractSchemaAST.CommonSchemaAST asCommon() {
            Optional<BooleanReference> required = asBooleanReference( "required" );
            Optional<BooleanReference> enabled = asBooleanReference( "enabled" );
            Optional<Object> defaultValue = Optional.ofNullable( properties.node.get( "default" ) );
            Object anEnum = properties.node.get( "enum" );
            Optional<Boolean> index = asBoolean( "index" ).optional();
            Optional<Boolean> includeInAll = asBoolean( "include_in_all" ).optional();
            Optional<String> denormalized = asString( "denormalized" ).optional();
            Optional<String> analyzer = asString( "analyzer" ).optional();
            Optional<Boolean> norms = asBoolean( "norms" ).optional();

            return new AbstractSchemaAST.CommonSchemaAST(
                properties.schemaType, required, enabled,
                defaultValue, toEnum( anEnum ),
                index, includeInAll,
                denormalized, analyzer, norms
            );
        }

        @SuppressWarnings( "unchecked" )
        public PropertyParser<LinkedHashMap<String, AbstractSchemaASTWrapper<?>>> asMapAST( String property, JsonSchemaParserContext context ) {
            LinkedHashMap<String, AbstractSchemaASTWrapper<?>> p = new LinkedHashMap<>();

            Optional<Map<Object, Object>> map =
                Optional.ofNullable( ( Map<Object, Object> ) context.node.get( property ) );

            map.ifPresent(
                m -> m.forEach( ( okey, value ) -> {
                    String key = ( String ) okey;
                    JsonSchemaParserContext newContext = context.withNode( key, value );
                    AbstractSchemaASTWrapper astw = newContext.astW.get( newContext.getId() );
                    if( astw == null ) {
                        astw = context.mapParser.apply( newContext );
                        newContext.astW.put( newContext.getId(), astw );
                    }
                    p.put( key, astw );
                } )
            );

            return new PropertyParser<>( property, properties, map.map( v -> p ) );
        }

        public Optional<BooleanReference> asBooleanReference( String field ) {
            Object enabled = properties.node.get( field );
            if( enabled == null ) return Optional.empty();

            if( enabled instanceof Boolean ) {
                return Optional.of( ( Boolean ) enabled ? BooleanReference.TRUE : BooleanReference.FALSE );
            } else {
                Map map = ( Map ) enabled;
                String jsonPath = ( String ) map.get( JSON_PATH );

                OperationFunction of = getOperationFunction( map );

                return Optional.of( new DynamicBooleanReference( jsonPath, of ) );
            }
        }
    }
}
