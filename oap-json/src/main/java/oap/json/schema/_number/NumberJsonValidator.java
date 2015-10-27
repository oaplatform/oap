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
package oap.json.schema._number;

import oap.json.schema.JsonSchemaParserProperties;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaAST;
import oap.util.Either;
import oap.util.Lists;
import oap.util.OptionalList;

import java.util.List;
import java.util.Optional;

public abstract class NumberJsonValidator<T extends Number> implements JsonSchemaValidator<NumberSchemaAST> {
    protected abstract boolean valid( Object value );

    protected abstract T cast( Object value );

    @Override
    public Either<List<String>, Object> validate( JsonValidatorProperties properties, NumberSchemaAST schema,
                                                  Object value ) {
        if( !valid( value ) )
            return Either.left(
                    Lists.of(
                        properties.error( "instance is of type " + getType( value ) +
                            ", which is none of the allowed primitive types ([" + schema.common.schemaType +
                            "])" ) ) );

        T castValue = cast( value );
        Double doubleValue = castValue.doubleValue();

        Optional<String> minimumResult = schema.minimum
                .filter( minimum -> doubleValue < minimum && !schema.exclusiveMinimum.orElse( false ) )
                .map( minimum -> "number is lower than the required minimum " + minimum );

        Optional<String> maximumResult = schema.maximum
                .filter( maximum -> doubleValue > maximum && !schema.exclusiveMaximum.orElse( false ) )
                .map( maximum -> "number is greater than the required maximum " + maximum );

        Optional<String> exclusiveMinimumResult = schema.minimum
                .filter( minimum -> doubleValue <= minimum && schema.exclusiveMinimum.orElse( false ) )
                .map( minimum -> "number is not strictly greater than the required minimum " + minimum );

        Optional<String> exclusiveMaximumResult = schema.maximum
                .filter( maximum -> doubleValue >= maximum && schema.exclusiveMaximum.orElse( false ) )
                .map( maximum -> "number is not strictly lower than the required maximum " + maximum );

        return OptionalList
                .<String>builder()
                .add( minimumResult )
                .add( maximumResult )
                .add( exclusiveMinimumResult )
                .add( exclusiveMaximumResult )
                .toEigher( castValue );
    }

    @Override
    public SchemaAST parse( JsonSchemaParserProperties properties ) {
        SchemaAST.CommonSchemaAST common = node( properties ).asCommon();
        Optional<Boolean> exclusiveMinimum = node( properties ).asBoolean( "exclusiveMinimum" ).optional();
        Optional<Boolean> exclusiveMaximum = node( properties ).asBoolean( "exclusiveMaximum" ).optional();
        Optional<Double> minimum = node( properties ).asDouble( "minimum" ).optional();
        Optional<Double> maximum = node( properties ).asDouble( "maximum" ).optional();

        return new NumberSchemaAST(
                common,
                exclusiveMinimum,
                exclusiveMaximum,
                minimum,
                maximum );
    }
}
