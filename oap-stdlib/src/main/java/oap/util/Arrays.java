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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

import static oap.util.Pair.__;

public class Arrays {
    private static final Random random = new Random();

    @SafeVarargs
    public static <T> Optional<T> find( Predicate<T> predicate, T... array ) {
        if( array == null ) return Optional.empty();
        for( T t : array ) if( predicate.test( t ) ) return Optional.ofNullable( t );
        return Optional.empty();
    }

    @SuppressWarnings( "unchecked" )
    public static <E> E[] filter( Predicate<E> predicate, E... array ) {
        ArrayList<E> result = Lists.filter( List.of( array ), predicate );
        return result.toArray( ( E[] ) Array.newInstance( array.getClass().getComponentType(), result.size() ) );
    }

    @SuppressWarnings( "unchecked" )
    public static <A, B> B[] map( Class<? super B> resultElementType, Function<A, B> mapper, A... array ) {
        B[] result = ( B[] ) Array.newInstance( resultElementType, array.length );
        for( int i = 0; i < array.length; i++ ) result[i] = mapper.apply( array[i] );
        return result;
    }

    @SuppressWarnings( "unchecked" )
    public static <A, B> B[] map( Class<? super B> resultElementType, List<A> list, Function<A, B> mapper ) {
        var size = list.size();
        B[] result = ( B[] ) Array.newInstance( resultElementType, size );

        for( var i = 0; i < size; i++ ) result[i] = mapper.apply( list.get( i ) );

        return result;
    }

    @SuppressWarnings( "unchecked" )
    public static <E> E[] of( Class<E> componentType, Collection<E> collection ) {
        return collection.toArray( ( E[] ) Array.newInstance( componentType, collection.size() ) );
    }

    @SafeVarargs
    public static <E> E[] of( E... e ) {
        return e;
    }


    @SuppressWarnings( "unchecked" )
    public static <E> Pair<E[], E[]> splitAt( int index, E... array ) {
        return __(
            java.util.Arrays.copyOfRange( array, 0, index ),
            java.util.Arrays.copyOfRange( array, index, array.length )
        );
    }

    public static Pair<int[], int[]> splitAt( int index, int... array ) {
        return __(
            java.util.Arrays.copyOfRange( array, 0, index ),
            java.util.Arrays.copyOfRange( array, index, array.length )
        );
    }


    @SafeVarargs
    @Deprecated
    public static <E> E[][] splitBy( Class<?> componentType, int by, E... a ) {
        return splitBy( by, a );
    }

    @SafeVarargs
    public static <E> E[][] splitBy( int by, E... a ) {
        Preconditions.checkArgument( a.length % by == 0, "illegal array size" );
        int segments = a.length / by;
        @SuppressWarnings( "unchecked" )
        E[][] result = ( E[][] ) Array.newInstance( a.getClass().getComponentType(), segments, by );
        for( int i = 0; i < segments; i++ )
            result[i] = java.util.Arrays.copyOfRange( a, i * by, i * by + by );
        return result;
    }


    @SafeVarargs
    public static <E> boolean contains( E v, E... array ) {
        if( v == null ) {
            for( E e : array ) if( e == null ) return true;
        } else for( E e : array ) if( e == v || v.equals( e ) ) return true;
        return false;
    }

    public static boolean contains( int v, int[] array ) {
        for( int e : array ) if( e == v ) return true;
        return false;
    }

    public static <E> Optional<E> random( E[] array ) {
        return array.length > 0 ? Optional.ofNullable( array[random.nextInt( array.length )] ) : Optional.empty();
    }

    public static int[] range( int from, int to ) {
        int[] result = new int[to - from];
        for( int i = from; i < to; i++ ) result[i - from] = i;
        return result;
    }

    public static void times( int times, Runnable runnable ) {
        for( int i = 0; i < times; i++ ) runnable.run();
    }

    public static IntBuilder ints() {
        return new IntBuilder();
    }

    public static class IntBuilder {
        private final List<Integer> numbers = new ArrayList<>();

        public IntBuilder with( int... ints ) {
            for( int i : ints ) numbers.add( i );
            return this;
        }

        public IntBuilder range( int from, int to ) {
            for( int i = from; i < to; i++ ) numbers.add( i );
            return this;
        }

        public int[] array() {
            int[] result = new int[numbers.size()];
            for( int i = 0; i < result.length; i++ )
                result[i] = numbers.get( i );
            return result;
        }

        public int[] reversed() {
            int[] result = new int[numbers.size()];
            for( int i = result.length - 1; i >= 0; i-- )
                result[i] = numbers.get( numbers.size() - 1 - i );
            return result;
        }
    }
}

