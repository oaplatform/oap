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
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;

import static oap.util.Collectors.CH_ID;
import static oap.util.Pair.__;

public class Lists extends oap.util.Collections {

    @SafeVarargs
    public static <E> List<E> addAll( List<E> list, E... array ) {
        Collections.addAll( list, array );
        return list;
    }

    @SafeVarargs
    public static <E> List<E> concat( List<? extends E>... lists ) {
        List<E> result = new ArrayList<>();

        for( List<? extends E> list : lists ) result.addAll( list );

        return result;
    }

    /**
     * @see #tailOf(List)
     */
    @Deprecated
    public static <E> List<E> tail( List<E> list ) {
        if( list.isEmpty() ) throw new NoSuchElementException();

        return list.subList( 1, list.size() );
    }

    /**
     * @see #headOf(List)
     */
    @Deprecated
    public static <E> E head( List<E> list ) {
        return headOf( list ).orElseThrow( NoSuchElementException::new );
    }

    /**
     * @see #headOf(List)
     */
    @Deprecated
    public static <E> Optional<E> headOpt( List<E> list ) {
        return headOf( list );
    }

    public static <E> Optional<E> headOf( List<E> list ) {
        return list.isEmpty() ? Optional.empty() : Optional.of( list.get( 0 ) );
    }

    public static <E> List<E> tailOf( List<E> list ) {
        if( list.isEmpty() ) throw new NoSuchElementException();
        return list.subList( 1, list.size() );
    }

    public static <E> Pair<List<E>, List<E>> partition( List<E> list, Predicate<E> p ) {
        var match = new ArrayList<E>();
        var nomatch = new ArrayList<E>();

        for( E item : list )
            if( p.test( item ) ) match.add( item );
            else nomatch.add( item );

        return __( match, nomatch );
    }

    public static <E, R> List<R> map( Collection<? extends E> list, Function<? super E, R> mapper ) {
        var result = new ArrayList<R>( list.size() );
        for( var e : list ) {
            result.add( mapper.apply( e ) );
        }
        return result;
    }

    public static <E, R> void map( Collection<? extends E> list, Function<? super E, R> mapper, List<R> toList ) {
        for( var e : list ) {
            toList.add( mapper.apply( e ) );
        }
    }

    public static <E, R> List<R> map( E[] array, Function<? super E, R> mapper ) {
        var result = new ArrayList<R>( array.length );
        for( var e : array ) {
            result.add( mapper.apply( e ) );
        }
        return result;
    }

    public static <E, R> void map( E[] array, Function<? super E, R> mapper, List<R> toList ) {
        for( var e : array ) {
            toList.add( mapper.apply( e ) );
        }
    }

    public static <E, R> List<R> map( Enumeration<E> enumeration, Function<? super E, R> mapper ) {
        var result = new ArrayList<R>();
        while( enumeration.hasMoreElements() ) {
            result.add( mapper.apply( enumeration.nextElement() ) );
        }
        return result;
    }

    public static <E, R> List<R> filterThenMap( Collection<? extends E> list, Predicate<? super E> predicate, Function<? super E, R> mapper ) {
        var result = new ArrayList<R>();
        for( var e : list ) {
            if( !predicate.test( e ) ) continue;
            result.add( mapper.apply( e ) );
        }
        return result;
    }

    @Deprecated( forRemoval = true )
    /**
     * Typo in name! Use filterThenMap instead
     */
    public static <E, R> List<R> filterThanMap( Collection<? extends E> list, Predicate<? super E> predicate, Function<? super E, R> mapper ) {
        return filterThenMap( list, predicate, mapper );
    }

    public static <E> List<E> filter( Collection<E> list, Predicate<E> predicate ) {
        var result = new ArrayList<E>();

        for( E e : list ) if( predicate.test( e ) ) result.add( e );

        return result;
    }

    public static <E> Optional<E> random( List<E> list ) {
        return Optional.ofNullable( list.isEmpty() ? null : list.get( ThreadLocalRandom.current().nextInt( list.size() ) ) );
    }

    @Deprecated
    public static <E> E randomNull( List<E> list ) {
        return random( list ).orElse( null );
    }

    public static <E> List<E> randomSublist( List<E> list ) {
        return randomSublist( list, ThreadLocalRandom.current().nextInt( list.size() + 1 ) );
    }

    public static <E> List<E> randomSublist( List<E> list, int sublistSize ) {
        Preconditions.checkArgument( sublistSize <= list.size() );
        return shuffle( list ).subList( 0, sublistSize );
    }

    public static <E> List<E> shuffle( List<E> list ) {
        var result = new ArrayList<E>( list );
        Collections.shuffle( result );
        return result;
    }

    public static <E> List<E> distinct( List<E> list ) {
        return new ArrayList<>( new HashSet<>( list ) );
    }

    @SafeVarargs
    public static <E> int[] indices( List<E> list, E... elements ) {
        int[] result = new int[elements.length];
        for( int i = 0; i < elements.length; i++ )
            if( list.contains( elements[i] ) ) result[i] = list.indexOf( elements[i] );
            else result[i] = -1;
        return result;
    }

    @SafeVarargs
    public static <E> List<E> of( E... array ) {
        var result = new ArrayList<E>( array.length );
        Collections.addAll( result, array );
        return result;
    }

    public static List<Long> of( long[] array ) {
        var result = new ArrayList<Long>( array.length );
        for( long i : array ) result.add( i );
        return result;
    }

    public static List<Integer> of( int[] array ) {
        var result = new ArrayList<Integer>( array.length );
        for( int i : array ) result.add( i );
        return result;
    }

    public static <E> List<E> empty() {
        return of();
    }

    public static <E> E last( List<E> list ) {
        return list.get( list.size() - 1 );
    }

    public static <E> boolean contains( Collection<E> list, Predicate<E> predicate ) {
        for( E e : list ) if( predicate.test( e ) ) return true;
        return false;
    }

    public static <E> int[] mapToIntArray( List<E> list, ToIntFunction<E> mapper ) {
        var size = list.size();
        var result = new int[size];

        for( int i = 0; i < size; i++ ) result[i] = mapper.applyAsInt( list.get( i ) );

        return result;
    }

    public static <E> List<E> reverse( Collection<E> values ) {
        var ret = new ArrayList<>( values );
        Collections.reverse( ret );
        return ret;
    }

    public <E> void moveItem( List<E> list, int sourceIndex, int targetIndex ) {
        if( sourceIndex <= targetIndex ) Collections.rotate( list.subList( sourceIndex, targetIndex + 1 ), -1 );
        else Collections.rotate( list.subList( targetIndex, sourceIndex + 1 ), 1 );
    }

    public static class Collectors {
        public static <E> Collector<E, ?, List<E>> toArrayList() {
            return new oap.util.Collectors.CollectorImpl<E, List<E>, List<E>>( ArrayList::new, List::add,
                ( left, right ) -> {
                    left.addAll( right );
                    return left;
                },
                CH_ID );
        }

        @SuppressWarnings( "deprecated" )
        public static Collector<Integer, ?, IntArrayList> toIntArrayList() {
            return new oap.util.Collectors.CollectorImpl<>( IntArrayList::new,
                ( integers, key ) -> integers.add( ( int ) key ),
                ( left, right ) -> {
                    left.addAll( right );
                    return left;
                },
                CH_ID );
        }
    }
}
