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
package oap.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

public class Lists {
    private static final ArrayList<Object> EMPTY = new ArrayList<>();

    public static <T> Pair<List<T>, List<T>> partition( List<T> list, Predicate<T> p ) {
        ArrayList<T> left = new ArrayList<>();
        ArrayList<T> right = new ArrayList<>();

        for( T item : list )
            if( p.test( item ) ) left.add( item );
            else right.add( item );

        return __( left, right );
    }

    public static <E> Optional<E> find( List<E> list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return Optional.ofNullable( e );
        return Optional.empty();
    }

    public static <E, R> List<R> map( List<? extends E> list, Function<? super E, R> mapper ) {
        return list.stream().map( mapper ).collect( toList() );
    }

    public static <E, R> List<R> map( E[] array, Function<? super E, R> mapper ) {
        return map( of( array ), mapper );
    }

    public static <E, R> List<R> map( Enumeration<E> enumeration, Function<? super E, R> mapper ) {
        return map( Collections.list( enumeration ), mapper );
    }

    public static <G, E> Map<G, List<E>> groupBy( List<E> list, Function<E, G> classifier ) {
        return Stream.of( list ).collect( java.util.stream.Collectors.groupingBy( classifier ) );
    }

    private static Random random = new Random();

    public static <E> Optional<E> random( List<E> list ) {
        return list.isEmpty() ? Optional.empty() :
            Optional.of( list.get( random.nextInt( list.size() ) ) );
    }

    @SafeVarargs
    public static <E> ArrayList<E> of( E... array ) {
        ArrayList<E> list = new ArrayList<>( array.length );
        Collections.addAll( list, array );
        return list;
    }

    public static ArrayList<Long> of( long[] array ) {
        ArrayList<Long> list = new ArrayList<>( array.length );
        for( long i : array ) list.add( i );
        return list;
    }

    public static ArrayList<Integer> of( int[] array ) {
        ArrayList<Integer> list = new ArrayList<>( array.length );
        for( int i : array ) list.add( i );
        return list;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> ArrayList<T> empty() {
        return (ArrayList<T>) EMPTY;
    }

    public static class Collectors {
        public static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
            return new oap.util.Collectors.CollectorImpl<T, ArrayList<T>, ArrayList<T>>( ArrayList::new, ArrayList::add,
                ( left, right ) -> {
                    left.addAll( right );
                    return left;
                },
                oap.util.Collectors.CH_ID );
        }
    }
}
