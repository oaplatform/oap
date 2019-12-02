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

package oap.id;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Identifier<I, T> {
    void set( T object, I id );

    I getOrInit( T object, Predicate<I> conflict );

    I get( T object );

    I fromString( String id );

    String toString( I id );

    static <T> StringIdentifierBuilder<T> forPath( String path ) {
        return StringIdentifierBuilder.forPath( path );
    }

    static <T> StringIdentifierBuilder<T> forAnnotation() {
        return StringIdentifierBuilder.forAnnotation();
    }

    static <T> StringIdentifier<T> forAnnotationFixed() {
        return StringIdentifierBuilder.<T>forAnnotation().build();
    }

    static <T> StringIdentifierBuilder<T> forId( final Function<T, String> getter ) {
        return StringIdentifierBuilder.forId( getter );
    }

    static <T> StringIdentifierBuilder<T> forId( final Function<T, String> getter, BiConsumer<T, String> setter ) {
        return StringIdentifierBuilder.forId( getter, setter );
    }

    static <T, I> Predicate<I> toConflict( Function<I, Optional<T>> f ) {
        return id -> f.apply( id ).isPresent();
    }
}
