/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.util.Stream;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Validators {
    private ConcurrentHashMap<Reflection.Parameter, Validators> forParams = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Reflection.Method, Validators> forMethods = new ConcurrentHashMap<>();

    public Validators forParameter( Reflection.Parameter parameter, Object instance ) {
        return forParams.computeIfAbsent( parameter, p -> {
            Validators validators = new Validators();
            for( Annotation a : parameter.annotations() )
                Reflect.reflect( a.annotationType() ).findAnnotation( Validator.class )
                    .ifPresent( v -> validators.peers.add( Reflect.newInstance( v.value(), a, instance ) ) );
            return validators;
        } );
    }

    private List<ValidatorPeer> peers = new ArrayList<>();

    public List<String> validate( Object value ) {
        return Stream.of( peers ).flatMap( p -> p.validate( value ).stream() ).toList();
    }

    public Validators forMethod( Reflection.Method method, Object instance ) {
        return forMethods.computeIfAbsent( method, p -> {
            Validators validators = new Validators();
            for( Annotation a : method.annotations() )
                Reflect.reflect( a.annotationType() ).findAnnotation( Validator.class )
                    .ifPresent( v -> validators.peers.add( Reflect.newInstance( v.value(), a, instance ) ) );
            return validators;
        } );

    }
}
