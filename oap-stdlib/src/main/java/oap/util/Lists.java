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

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.AbstractIntCollection;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;

import static oap.util.Pair.__;

public class Lists extends oap.util.Collections {

    private static Random random = new Random();

    @SafeVarargs
    public static <E> List<E> addAll( List<E> list, E... array ) {
        Collections.addAll( list, array );
        return list;
    }

    @SafeVarargs
    public static <E> ArrayList<E> concat( List<? extends E>... lists ) {
        final ArrayList<E> concatenated = new ArrayList<>();

        for( List<? extends E> list : lists ) concatenated.addAll( list );
        return concatenated;
    }

    public static <T> List<T> tail( List<T> list ) {
        if( list.isEmpty() ) throw new NoSuchElementException();

        return list.subList( 1, list.size() );
    }

    public static <T> T head( List<T> list ) {
        return headOpt( list ).orElseThrow( NoSuchElementException::new );
    }

    public static <T> Optional<T> headOpt( List<T> list ) {
        if( list.isEmpty() ) return Optional.empty();

        return Optional.of( list.get( 0 ) );
    }

    public static <T> Pair<List<T>, List<T>> partition( List<T> list, Predicate<T> p ) {
        ArrayList<T> match = new ArrayList<>();
        ArrayList<T> nomatch = new ArrayList<>();

        for( T item : list )
            if( p.test( item ) ) match.add( item );
            else nomatch.add( item );

        return __( match, nomatch );
    }

    public static <E, R> ArrayList<R> map( Collection<? extends E> list, Function<? super E, R> mapper ) {
        final ArrayList<R> result = new ArrayList<>( list.size() );
        for( val e : list ) {
            result.add( mapper.apply( e ) );
        }
        return result;
    }

    public static <E, R> ArrayList<R> map( E[] array, Function<? super E, R> mapper ) {
        return map( of( array ), mapper );
    }

    public static <E> ArrayList<E> filter( List<E> list, Predicate<E> predicate ) {
        final ArrayList<E> result = new ArrayList<>();

        for( E e : list ) if( predicate.test( e ) ) result.add( e );

        return result;
    }

    public static <E, R> ArrayList<R> map( Enumeration<E> enumeration, Function<? super E, R> mapper ) {
        final ArrayList<R> result = new ArrayList<>();
        while( enumeration.hasMoreElements() ) {
            result.add( mapper.apply( enumeration.nextElement() ) );
        }
        return result;
    }

    public static <E> Optional<E> random( List<E> list ) {
        return list.isEmpty() ? Optional.empty()
            : Optional.of( list.get( random.nextInt( list.size() ) ) );
    }

    public static <E> List<E> randomSublist( List<E> list ) {
        return randomSublist( list, random.nextInt( list.size() + 1 ) );
    }

    public static <E> List<E> randomSublist( List<E> list, int sublistSize ) {
        Preconditions.checkArgument( sublistSize <= list.size() );
        return shuffle( list ).subList( 0, sublistSize );
    }

    public static <E> ArrayList<E> shuffle( List<E> list ) {
        val localCopy = new ArrayList<E>( list );
        Collections.shuffle( localCopy );
        return localCopy;
    }

    public static <E> ArrayList<E> distinct( List<E> list ) {
        return new ArrayList<>( new HashSet<>( list ) );
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

    public static <T> ArrayList<T> empty() {
        return of();
    }

    public static <T> T last( List<T> list ) {
        return list.get( list.size() - 1 );
    }

    public static <E> boolean contains( List<E> list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return true;
        return false;
    }

    public static <T> int[] mapToIntArray( List<T> list, ToIntFunction<T> func ) {
        val size = list.size();
        val array = new int[size];

        for( int i = 0; i < size; i++ ) {
            array[i] = func.applyAsInt( list.get( i ) );
        }

        return array;
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

        public static Collector<Integer, ?, IntArrayList> toIntArrayList() {
            return new oap.util.Collectors.CollectorImpl<>( IntArrayList::new, AbstractIntCollection::add,
                ( left, right ) -> {
                    left.addAll( right );
                    return left;
                },
                oap.util.Collectors.CH_ID );
        }
    }

}
