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
package oap.util;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.google.common.collect.Iterators.concat;

public class Iterators {


    public static <T> Iterator<T> of( Supplier<Boolean> hasNext, Supplier<T> next ) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return hasNext.get();
            }

            @Override
            public T next() {
                return next.get();
            }
        };
    }

    public static <T> Iterator<T> of( T initialState, Predicate<T> hasNext, UnaryOperator<T> next ) {
        AtomicReference<T> state = new AtomicReference<>( initialState );
        return of( () -> hasNext.test( state.get() ), () -> state.getAndUpdate( next ) );
    }

    public static <T> Iterator<T> traverse( T initialState, UnaryOperator<T> traverse ) {
        return of( initialState, Objects::nonNull, traverse );
    }

    public static <T> Iterator<T> flatTraverse( T initialState, Function<T, Iterator<T>> traverse ) {
        return concat( Lists.of( initialState ).iterator(),
            concat( Stream.of( traverse.apply( initialState ) )
                .map( t -> flatTraverse( t, traverse ) )
                .iterator() ) );
    }

    public static <T> Iterator<T> empty() {
        return of( () -> false, () -> null );
    }
}
