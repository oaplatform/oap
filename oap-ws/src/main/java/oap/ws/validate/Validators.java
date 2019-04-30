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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.reflect.Reflect;
import oap.reflect.Reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Validators {
    private static ConcurrentHashMap<Key<Reflection.Parameter>, Validator> forParams = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Key<Reflection.Method>, Validator> forMethods = new ConcurrentHashMap<>();

    public static Validator forParameter( Reflection.Method method,
                                          Reflection.Parameter parameter,
                                          Object instance,
                                          boolean beforeUnmarshaling ) {
        return forParams.computeIfAbsent( new Key<>( parameter, instance, beforeUnmarshaling ),
            p -> getValidator( method, instance, parameter.annotations(),
                ValidatorPeer.Type.PARAMETER, beforeUnmarshaling ) );
    }

    public static Validator forMethod( Reflection.Method method,
                                       Object instance,
                                       boolean beforeUnmarshaling ) {
        return forMethods.computeIfAbsent( new Key<>( method, instance, beforeUnmarshaling ),
            p -> getValidator( method, instance, method.annotations(),
                ValidatorPeer.Type.METHOD, beforeUnmarshaling ) );

    }

    private static Validator getValidator( Reflection.Method method,
                                           Object instance, List<Annotation> annotations,
                                           ValidatorPeer.Type type,
                                           boolean beforeUnmarshaling ) {
        final Validator validator = new Validator();
        for( Annotation annotation : annotations )
            Reflect.reflect( annotation.annotationType() )
                .findAnnotation( Peer.class )
                .filter( v -> v.applyBeforeUnmarshaling() == beforeUnmarshaling )
                .ifPresent( v -> validator.peers.add( Reflect.newInstance( v.value(), annotation, method, instance, type ) ) );
        return validator;
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class Key<T> {
        public final T parameter;
        public final Object instance;
        public final boolean beforeUnmashaling;
    }

    public static class Validator {
        private final List<ValidatorPeer> peers = new ArrayList<>();

        public ValidationErrors validate( Object value, Map<Reflection.Parameter, Object> originalValues ) {
            var total = ValidationErrors.empty();
            for( var peer : peers ) {
                var result = peer.validate( value, originalValues );
                if( result.failed() && !result.hasDefaultCode() ) return result;
                total = total.merge( result );
            }
            return total;
        }
    }

}
