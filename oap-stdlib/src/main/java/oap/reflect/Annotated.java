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
package oap.reflect;

import oap.util.Lists;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class Annotated<T extends java.lang.reflect.AnnotatedElement> {
    public final T underlying;

    Annotated( T underlying ) {
        this.underlying = underlying;
    }

    public boolean isAnnotatedWith( Class<? extends Annotation> clazz ) {
        return underlying.isAnnotationPresent( clazz );
    }

    public <A extends Annotation> List<A> annotationOf( Class<A> clazz ) {
        return Lists.of( underlying.getAnnotationsByType( clazz ) );
    }

    public <A extends Annotation> Optional<A> findAnnotation( Class<A> clazz ) {
        A[] as = underlying.getAnnotationsByType( clazz );
        return as.length == 0 ? Optional.empty() : Optional.of( as[0] );
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    public List<Annotation> annotations() {
        return Lists.of( underlying.getAnnotations() );
    }


    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;
        Annotated<?> annotated = ( Annotated<?> ) o;
        System.out.println( underlying + " and " + annotated.underlying + " are: " );
        System.out.println( Objects.equals( underlying, annotated.underlying ) );
        // TODO: make equals similar to java.lang.reflect equals methods
        return Objects.equals( underlying.toString(), annotated.underlying.toString() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( underlying );
    }
}
