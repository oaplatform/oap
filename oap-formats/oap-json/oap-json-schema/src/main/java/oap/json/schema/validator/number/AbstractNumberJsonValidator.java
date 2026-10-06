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
package oap.json.schema.validator.number;

import oap.json.schema.AbstractJsonSchemaValidator;
import oap.json.schema.JsonSchemaError;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonMessage;
import oap.json.schema.JsonValidatorProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractNumberJsonValidator<T extends Number> extends AbstractJsonSchemaValidator<NumberSchemaAST> {
    protected AbstractNumberJsonValidator( String type ) {
        super( type );
    }

    protected abstract boolean valid( Object value );

    @Override
    public List<JsonSchemaError> validate( JsonValidatorProperties properties, NumberSchemaAST schema, Object value ) {
        if( !valid( value ) ) return typeFailed( properties, schema, value );

        final double doubleValue = ( ( Number ) value ).doubleValue();

        final List<JsonSchemaError> errors = new ArrayList<>();

        schema.minimum.filter( minimum -> doubleValue < minimum && !schema.exclusiveMinimum.orElse( false ) )
            .ifPresent( minimum -> errors.add( properties.error( schema, JsonMessage.MINIMUM, Map.of( "value", print( doubleValue ), "minimum", print( minimum ) ) ) ) );

        schema.maximum.filter( maximum -> doubleValue > maximum && !schema.exclusiveMaximum.orElse( false ) )
            .ifPresent( maximum -> errors.add( properties.error( schema, JsonMessage.MAXIMUM, Map.of( "value", print( doubleValue ), "maximum", print( maximum ) ) ) ) );

        schema.minimum.filter( minimum -> doubleValue <= minimum && schema.exclusiveMinimum.orElse( false ) )
            .ifPresent( minimum -> errors.add( properties.error( schema, JsonMessage.MINIMUM_EXCLUSIVE, Map.of( "value", print( doubleValue ), "minimum", print( minimum ) ) ) ) );

        schema.maximum.filter( maximum -> doubleValue >= maximum && schema.exclusiveMaximum.orElse( false ) )
            .ifPresent( maximum -> errors.add( properties.error( schema, JsonMessage.MAXIMUM_EXCLUSIVE, Map.of( "value", print( doubleValue ), "maximum", print( maximum ) ) ) ) );

        return errors;
    }

    protected String print( double value ) {
        return String.valueOf( value );
    }

    @Override
    public NumberSchemaASTWrapper parse( JsonSchemaParserContext context ) {
        final NumberSchemaASTWrapper wrapper = context.createWrapper( NumberSchemaASTWrapper::new );

        wrapper.common = node( context ).asCommon();
        wrapper.conditional = node( context ).asConditional( context );
        wrapper.exclusiveMinimum = node( context ).asBoolean( "exclusiveMinimum" ).optional();
        wrapper.exclusiveMaximum = node( context ).asBoolean( "exclusiveMaximum" ).optional();
        wrapper.minimum = node( context ).asDouble( "minimum" ).optional();
        wrapper.maximum = node( context ).asDouble( "maximum" ).optional();

        return wrapper;
    }
}
