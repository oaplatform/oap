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

import lombok.val;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by igor.petrenko on 16.05.2017.
 */
public class Collections {
    public static <T, L extends Collection<T>> long count( L collection, Predicate<T> predicate ) {
        long count = 0;

        for( val item : collection ) {
            if( predicate.test( item ) ) count++;
        }

        return count;
    }

    public static <E, L extends Collection<E>> Optional<E> find( L list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return Optional.ofNullable( e );
        return Optional.empty();
    }

    public static <E, L extends Collection<E>> E find2( L list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return e;
        return null;
    }

    public static <E, L extends Collection<E>> boolean allMatch( L list, Predicate<E> predicate ) {
        for( val e : list ) {
            if( !predicate.test( e ) ) return false;
        }
        return true;
    }
}
