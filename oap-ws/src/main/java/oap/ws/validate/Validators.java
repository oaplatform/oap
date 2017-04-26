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

import oap.reflect.Reflect;
import oap.reflect.Reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Validators {

    public static Validator forParameter( Map<Reflection.Parameter, Object> values,
                                          Reflection.Method method, Reflection.Parameter parameter,
                                          Object instance, boolean originalParameters ) {
        return getValidator( values, method, instance, parameter.annotations(),
                ValidatorPeer.Type.PARAMETER, originalParameters );
     }

    public static Validator forMethod( Map<Reflection.Parameter, Object> values,
                                       Reflection.Method method, Object instance,
                                       boolean originalParameters ) {
        return getValidator( values, method, instance, method.annotations(),
                ValidatorPeer.Type.METHOD, originalParameters );
    }

    private static Validator getValidator( Map<Reflection.Parameter, Object> values,
                                           Reflection.Method method,
                                           Object instance, List<Annotation> annotations,
                                           ValidatorPeer.Type type,
                                           boolean originalParameters ) {
        final Validator validator = new Validator();
        for( final Annotation annotation : annotations )
            Reflect.reflect( annotation.annotationType() ).findAnnotation( Peer.class )
                .filter( peer -> peer.originalParameters() == originalParameters )
                .ifPresent( peer -> validator.peers.add(
                    Reflect.newInstance( peer.value(), annotation, values, method, instance, type ) )
                );

        return validator;
    }

    public static class Validator {
        private final List<ValidatorPeer> peers = new ArrayList<>();

        public ValidationErrors validate( Object value ) {
            ValidationErrors total = ValidationErrors.empty();
            for( ValidatorPeer peer : peers ) {
                ValidationErrors result = peer.validate( value );
                if( result.isFailed() && !result.hasDefaultCode() ) return result;
                total.merge( result );
            }
            return total;
        }
    }

}
