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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Collections {
    public static <E, L extends Collection<E>> long count( L collection, Predicate<E> predicate ) {
        long count = 0;

        for( E item : collection ) if( predicate.test( item ) ) count++;

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
        for( E e : list ) if( !predicate.test( e ) ) return false;
        return true;
    }

    public static <E, L extends Collection<E>> boolean anyMatch( L list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return true;
        return false;
    }

    public static <G, E> Map<G, List<E>> groupBy( Collection<E> list, Function<E, G> classifier ) {
        final HashMap<G, List<E>> result = new HashMap<>();

        for( E e : list ) {
            final G key = classifier.apply( e );
            result.computeIfAbsent( key, k -> new ArrayList<>() ).add( e );
        }

        return result;
    }

    /**
     * @see Lists#partition(List, Predicate)
     */
    @Deprecated
    public static <E> void partition( Collection<E> list, List<E> left, List<E> right, Predicate<E> predicate ) {
        for( E v : list ) {
            if( predicate.test( v ) ) right.add( v );
            else left.add( v );
        }
    }

    public static int max( Collection<Integer> ints ) {
        assert !ints.isEmpty();

        int max = Integer.MIN_VALUE;
        for( Integer i : ints ) if( max < i ) max = i;

        return max;
    }

    public static <E> E head2( Collection<E> collection ) {
        return collection.isEmpty() ? null : collection.iterator().next();
    }

    public <T, K, U> LinkedHashMap<K, U> toLinkedHashMap( Function<? super T, ? extends K> keyMapper,
                                                          Function<? super T, ? extends U> valueMapper, Collection<T> collection ) {
        var map = new LinkedHashMap<K, U>();
        for( var item : collection ) {
            map.put( keyMapper.apply( item ), valueMapper.apply( item ) );
        }

        return map;
    }

    public <T, K, U> HashMap<K, U> toHashMap( Function<? super T, ? extends K> keyMapper,
                                              Function<? super T, ? extends U> valueMapper, Collection<T> collection ) {
        var map = new HashMap<K, U>();
        for( var item : collection ) {
            map.put( keyMapper.apply( item ), valueMapper.apply( item ) );
        }

        return map;
    }
}
