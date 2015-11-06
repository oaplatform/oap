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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public class Arrays {
    private static Random random = new Random();

    @SafeVarargs
    public static <T> Optional<T> find( Predicate<T> predicate, T... array ) {
        if( array == null ) return Optional.empty();
        for( T t : array ) if( predicate.test( t ) ) return Optional.ofNullable( t );
        return Optional.empty();
    }

    @SuppressWarnings( "unchecked" )
    public static <A, B> B[] map( Class<?> elementType, Function<A, B> mapper, A... array ) {
        B[] result = (B[]) Array.newInstance( elementType, array.length );
        for( int i = 0; i < array.length; i++ ) result[i] = mapper.apply( array[i] );
        return result;
    }

    @SuppressWarnings( "unchecked" )
    public static <E> E[] of( Class<?> componentType, Collection<E> collection ) {
        return collection.toArray( (E[]) Array.newInstance( componentType, collection.size() ) );
    }

    @SafeVarargs
    public static <E> boolean contains( E v, E... array ) {
        if( v == null ) {
            for( final E e : array ) if( e == null ) return true;
        } else {
            for( final E e : array ) if( e == v || v.equals( e ) ) return true;
        }
        return false;
    }

    public static boolean contains( int v, int[] array ) {
        for( final int e : array ) if( e == v ) return true;
        return false;
    }

    @SafeVarargs
    public static <E> E[] of( E... e ) {
        return e;
    }

    public static <E> Optional<E> random( E[] array ) {
        return array.length > 0 ? Optional.ofNullable( array[random.nextInt( array.length )] ) : Optional.empty();
    }

    public static int[] range( int from, int to ) {
        int[] result = new int[to - from];
        for( int i = from; i < to; i++ ) result[i - from] = i;
        return result;
    }

    public static IntBuilder ints() {
        return new IntBuilder();
    }

    public static class IntBuilder {
        private List<Integer> numbers = new ArrayList<>();

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

