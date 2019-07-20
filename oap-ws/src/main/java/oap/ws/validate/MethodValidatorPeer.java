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
package oap.ws.validate;

import lombok.extern.slf4j.Slf4j;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.Stream;
import oap.ws.WsException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
public class MethodValidatorPeer implements ValidatorPeer {
    private final List<Validator> validators;

    public MethodValidatorPeer( WsValidate validate, Reflection.Method targetMethod, Object instance, Type type ) {
        if( type == Type.PARAMETER )
            this.validators = Lists.map( validate.value(), m -> new ParameterValidator( m, targetMethod, instance ) );
        else
            this.validators = Lists.map( validate.value(), m -> new MethodValidator( m, targetMethod, instance ) );
    }

    @Override
    public ValidationErrors validate( Object value, Map<Reflection.Parameter, Object> originalValues ) {
        var ret = ValidationErrors.empty();
        for( var validator : validators ) {
            ret = ret.merge( validator.validate( value ) );
        }

        return ret;
    }

    private abstract static class Validator {
        protected final Reflection.Method method;
        protected final Object instance;

        protected Validator( String method, Reflection.Method targetMethod, Object instance ) {
            this.method = Reflect.reflect( instance.getClass() )
                .method( method ) // TODO: replace it with method( method, targetMethod.parameters ),
                //  once the issue with @WsValidate is fixed
                .orElseThrow( () -> new WsException( String.format( "No such method %s with the following parameters: %s",
                    method, targetMethod.parameters ) ) );
            this.instance = instance;
        }

        abstract ValidationErrors validate( Object value );
    }

    private static class ParameterValidator extends Validator {
        public ParameterValidator( String method, Reflection.Method targetMethod, Object instatnce ) {
            super( method, targetMethod, instatnce );
        }

        @Override
        public ValidationErrors validate( Object value ) {
            return method.invoke( instance, value );
        }
    }

    private static class MethodValidator extends Validator {
        private final Map<String, Integer> validatorMethodParamIndices;

        protected MethodValidator( String method, Reflection.Method targetMethod, Object instance ) {
            super( method, targetMethod, instance );
            validatorMethodParamIndices = Stream.of( targetMethod.parameters )
                .map( Reflection.Parameter::name )
                .zipWithIndex()
                .filter( ( p, i ) -> this.method.hasParameter( p ) )
                .toMap();
        }

        @Override
        ValidationErrors validate( Object value ) {
            Object[] params = new Object[method.parameters.size()];
            for( int i = 0; i < params.length; i++ ) {
                String argumentName = method.parameters.get( i ).name();
                Integer argumentIndex = validatorMethodParamIndices.get( argumentName );
                if( argumentIndex == null ) {
                    throw new IllegalArgumentException( argumentName + " required by validator " + this.method.name()
                        + " is not supplied by web method" );
                }
                params[i] = ( ( Object[] ) value )[argumentIndex];
            }
            try {
                return method.invoke( instance, params );
            } catch( ReflectException e ) {
                log.error( e.getMessage() );
                log.info( "method = " + method.name() );
                log.info( "method parameters = " + method.parameters.stream()
                    .map( p -> p.type().underlying )
                    .collect( toList() ) );
                log.info( "method parameters = " + Arrays.stream( params ).map( p -> p == null ? "<NULL>"
                    : p.getClass() ).collect( toList() ) );
                throw e;
            }
        }

    }
}
