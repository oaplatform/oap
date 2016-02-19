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
package oap.json.schema._object;

import oap.json.schema.JsonSchemaParserProperties;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaAST;
import oap.util.*;

import java.util.*;
import java.util.stream.Collectors;

import static oap.util.Pair.__;

public class ObjectJsonValidator implements JsonSchemaValidator<ObjectSchemaAST> {
    @SuppressWarnings( "unchecked" )
    @Override
    public Either<List<String>, Object> validate( JsonValidatorProperties properties, ObjectSchemaAST schema,
                                                  Object value ) {
        if( !(value instanceof Map<?, ?>) ) return Either.left(
                Lists.of(
                    properties.error( "instance is of type " + getType( value ) +
                        ", which is none of the allowed primitive types ([" + schema.common.schemaType +
                        "])" ) ) );

        Map<Object, Object> mapValue = (Map<Object, Object>) value;

        Either<List<String>, List<Pair<String, Object>>> result = Either.fold2( schema.properties
                .entrySet()
                .stream()
                .flatMap(
                        ( e ) -> {
                            Object v = mapValue.get( e.getKey() );

                            Either<List<String>, Pair<String, Object>> res = properties.validator
                                    .apply(
                                            properties
                                                    .withPath( e.getKey() )
                                                    .withAdditionalProperties( schema.additionalProperties ),
                                            e.getValue(),
                                            v
                                    )
                                    .right()
                                    .map( r -> __( e.getKey(), r ) );

                            return v == null && res.isRight() && res.right().get()._2 == null ?
                                    java.util.stream.Stream.empty() : java.util.stream.Stream.of( res );
                        }
                ) );


        List<String> additionalProperties = mapValue.keySet()
                .stream()
                .filter( v -> !schema.properties.containsKey( v.toString() ) )
                .map( v -> (String) v )
                .collect( Collectors.toList() );

        if( !schema.additionalProperties.orElse( properties.additionalProperties.orElse( true ) ) ) {
            if( !additionalProperties.isEmpty() ) {
                Optional<String> additionalPropertiesResult = Optional.of(
                        properties.error( "additional properties are not permitted [" +
                                String.join( ",", additionalProperties.stream().map(
                                        Object::toString ).collect( Collectors.toList() ) ) + "]" ) );

                if( additionalPropertiesResult.isPresent() ) {
                    result = result.<Either<List<String>, List<Pair<String, Object>>>>fold(
                            l -> {
                                l.add( additionalPropertiesResult.get() );
                                return Either.left( l );
                            },
                            (r) -> Either.left( Collections.singletonList( additionalPropertiesResult.get() ) )
                    );
                }
            }
        } else {
            result = result.<Either<List<String>, List<Pair<String, Object>>>>fold(
                    Either::left,
                    r -> {
                        r.addAll( additionalProperties.stream().map( ap -> __( ap, mapValue.get( ap ) ) ).collect(
                                Collectors.toList() ) );
                        return Either.right( r );
                    }
            );
        }

        return result
                .right()
                .map( rr -> (Object) Stream.of( rr.stream() ).collect( Maps.Collectors.<String, Object>toMap() ) );

    }

    @Override
    public SchemaAST parse( JsonSchemaParserProperties properties ) {
        SchemaAST.CommonSchemaAST common = node( properties ).asCommon();
        Optional<Boolean> additionalProperties = node( properties ).asBoolean( "additionalProperties" ).optional();
        Optional<String> extendsValue = node( properties ).asString( "extends" ).optional();

        LinkedHashMap<String, SchemaAST> objectProperties = extendsValue
                .map( url -> ((ObjectSchemaAST) properties.urlParser.apply( url )).properties )
                .orElse( node( properties ).asMapAST( "properties" ).required() );

        return new ObjectSchemaAST( common,
                additionalProperties,
                extendsValue,
                objectProperties
        );
    }
}
