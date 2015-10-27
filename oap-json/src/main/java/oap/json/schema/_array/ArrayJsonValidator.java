/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
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
package oap.json.schema._array;

import oap.json.schema.JsonSchemaParserProperties;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaAST;
import oap.util.Either;
import oap.util.Lists;
import oap.util.OptionalList;
import oap.util.Stream;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ArrayJsonValidator implements JsonSchemaValidator<ArraySchemaAST> {
    @SuppressWarnings( "unchecked" )
    @Override
    public Either<List<String>, Object> validate( JsonValidatorProperties properties, ArraySchemaAST schema,
        Object value ) {
        if( !(value instanceof Collection<?>) ) return Either.left(
            Lists.of(
                properties.error( "instance is of type " + getType( value ) +
                    ", which is none of the allowed primitive types ([" + schema.common.schemaType +
                    "])" ) ) );

        Collection<?> arrayValue = (Collection<?>) value;

        Optional<String> minItemsResult = schema.minItems
            .filter( minItems -> arrayValue.size() < minItems )
            .map( minItems -> properties.error( "array has less than minItems elements " + minItems ) );

        Optional<String> maxItemsResult = schema.maxItems
            .filter( maxItems -> arrayValue.size() > maxItems )
            .map( maxItems -> properties.error( "array has more than maxItems elements " + maxItems ) );

        Either<List<String>, Object> result = OptionalList
            .<String>builder()
            .add( minItemsResult )
            .add( maxItemsResult )
            .toEigher( value );

        return result.right().flatMap( r -> Either.fold2(
                Stream
                    .of( arrayValue.stream() )
                    .zipWithIndex()
                    .<Either<List<String>, Object>>map(
                        pair -> properties.validator.apply( properties.withPath(
                                String.valueOf( pair._2 ) ), schema.items,
                            pair._1 ) )
            )
                .right()
                .map( l -> (Object) l )
        );
    }

    @Override
    public SchemaAST parse( JsonSchemaParserProperties properties ) {
        SchemaAST.CommonSchemaAST common = node( properties ).asCommon();
        Optional<Integer> minItems = node( properties ).asInt( "minItems" ).optional();
        Optional<Integer> maxItems = node( properties ).asInt( "maxItems" ).optional();
        SchemaAST items = node( properties ).asAST( "items" ).required();

        return new ArraySchemaAST( common, minItems, maxItems, items );
    }
}
