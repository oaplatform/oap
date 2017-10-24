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
package oap.ws.validate.testng;

import oap.json.schema.TestJsonValidators;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Stream;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.Validators;
import org.assertj.core.api.AbstractAssert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationErrorsAssertion extends AbstractAssert<ValidationErrorsAssertion, ValidationErrors> {

    protected ValidationErrorsAssertion( ValidationErrors actual ) {
        super( actual, ValidationErrorsAssertion.class );
    }


    public static ValidationErrorsAssertion assertValidationErrors( ValidationErrors actual ) {
        return new ValidationErrorsAssertion( actual );
    }

    public static <I> ValidatedInvocation<I> validating( Class<I> anInterface ) {
        return new ValidatedInvocation<>( anInterface );
    }

    public ValidationErrorsAssertion hasCode( int code ) {
        assertThat( this.actual.code ).isEqualTo( code );
        return this;
    }

    public ValidationErrorsAssertion containsErrors( String... errors ) {
        assertThat( this.actual.errors ).contains( errors );
        return this;
    }

    public ValidationErrorsAssertion isError( int code, String error ) {
        return hasCode( code ).containsErrors( error );
    }

    public ValidationErrorsAssertion isFailed() {
        assertThat( this.actual.isFailed() )
            .withFailMessage( "should contain errors" )
            .isTrue();
        return this;
    }

    public ValidationErrorsAssertion isNotFailed() {
        assertThat( this.actual.isFailed() )
            .withFailMessage( "shouldn't contain errors but contain: " + this.actual )
            .isFalse();
        return this;
    }

    public static class ValidatedInvocation<I> implements InvocationHandler {
        private final I proxy;
        private List<Function<ValidationErrorsAssertion, ValidationErrorsAssertion>> assertions = new ArrayList<>();
        private I instance;

        @SuppressWarnings( "unchecked" )
        public ValidatedInvocation( Class<I> anInterface ) {
            proxy = ( I ) Proxy.newProxyInstance( anInterface.getClassLoader(), new Class[] { anInterface }, this );
        }

        public ValidatedInvocation<I> hasCode( int code ) {
            assertions.add( a -> a.hasCode( code ) );
            return this;
        }

        public ValidatedInvocation<I> containsErrors( String... errors ) {
            assertions.add( a -> a.containsErrors( errors ) );
            return this;
        }

        public ValidatedInvocation<I> isError( int code, String error ) {
            return hasCode( code ).containsErrors( error );
        }

        public ValidatedInvocation<I> isFailed() {
            assertions.add( ValidationErrorsAssertion::isFailed );
            return this;
        }

        public ValidatedInvocation<I> isNotFailed() {
            assertions.add( ValidationErrorsAssertion::isNotFailed );
            return this;
        }

        @Override
        public Object invoke( Object proxy, Method jmethod, Object[] args ) throws Throwable {
            Optional<Reflection.Method> methodOpt = Reflect.reflect( instance.getClass() )
                .method( jmethod );
            if( !methodOpt.isPresent() ) throw new NoSuchMethodError( jmethod.toString() );
            return methodOpt
                .map( method -> {
                    ValidationErrors paramErrors = ValidationErrors.empty();

                    List<Reflection.Parameter> parameters = method.parameters;

                    LinkedHashMap<Reflection.Parameter, Object> values = new LinkedHashMap<>();
                    for( int i = 0; i < parameters.size(); i++ ) {
                        values.put( parameters.get( i ), args[i] );
                    }

                    for( int i = 0; i < parameters.size(); i++ ) {
                        Reflection.Parameter parameter = parameters.get( i );
                        paramErrors.merge( Validators
                            .forParameter( method, parameter, instance, false, TestJsonValidators.jsonValidatos() )
                            .validate( args[i], values ) );
                    }
                    if( paramErrors.isFailed() ) {
                        runAsserts( paramErrors );
                        return null;
                    } else {
                        ValidationErrors methodErrors = Validators
                            .forMethod( method, instance, false, TestJsonValidators.jsonValidatos() )
                            .validate( args, values );
                        runAsserts( methodErrors );
                        if( methodErrors.isFailed() ) return null;
                    }
                    return method.invoke( instance, args );
                } )
                .orElse( null );
        }

        private void runAsserts( ValidationErrors errors ) {
            ValidationErrorsAssertion assertion = ValidationErrorsAssertion.assertValidationErrors( errors );
            Stream.of( assertions )
                .foldLeft( assertion, ( a, f ) -> f.apply( a ) );
        }

        public I forInstance( I instance ) {
            this.instance = instance;
            return proxy;
        }
    }
}
