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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

public class StringBits {
    public static final long UNKNOWN = 0;

    public final HashMap<String, Long> bits;
    private final AtomicLong bit = new AtomicLong( 1L );

    public StringBits() {
        bits = new HashMap<>();
        bits.put( Strings.UNKNOWN, UNKNOWN );
    }

    public StringBits( int initialCapacity, float loadFactor ) {
        bits = new HashMap<>( initialCapacity, loadFactor );
        bits.put( Strings.UNKNOWN, UNKNOWN );
    }

    public final synchronized long computeIfAbsent( String name ) {
        return bits.computeIfAbsent( name, n -> bit.getAndIncrement() );
    }

    public final synchronized long[] computeIfAbsent( List<String> names ) {
        return computeIfAbsent( Stream.of( names ) );
    }

    public final synchronized long[] computeIfAbsent( Stream<String> names ) {
        return names.mapToLong( n -> bits.computeIfAbsent( n, k -> bit.getAndIncrement() ) ).toArray();
    }

    public final long get( String name ) {
        return bits.getOrDefault( name, UNKNOWN );
    }

    public final long[] get( List<String> name ) {
        final int size = name.size();
        final long[] result = new long[size];

        for( int i = 0; i < size; i++ ) {
            result[i] = bits.getOrDefault( name.get( i ), UNKNOWN );
        }

        return result;
    }

    public final BitSet bits( Collection<String> values, boolean fill ) {
        BitSet bitSet = new BitSet( bits.size() );

        if( values == null || values.isEmpty() ) {
            if( fill ) bitSet.set( 0, bits.size() );
            return bitSet;
        }

        values.forEach( v -> bitSet.set( ( int ) get( v ) ) );
        return bitSet;
    }

    public int size() {
        return bits.size();
    }

    public String valueOf( long bit ) {
        return bits.entrySet()
            .stream()
            .filter( e -> e.getValue() == bit )
            .findAny()
            .map( Map.Entry::getKey )
            .orElse( Strings.UNKNOWN );
    }

    public List<String> valueOf( java.util.BitSet bits ) {
        return valueOf( bits.stream().mapToLong( i -> i ) );
    }

    public List<String> valueOf( long[] bits ) {
        return valueOf( LongStream.of( bits ) );
    }

    public List<String> valueOf( int[] bits ) {
        return valueOf( IntStream.of( bits ).mapToLong( i -> i ) );
    }

    private List<String> valueOf( LongStream bits ) {
        return bits
            .mapToObj( b -> this.bits
                .entrySet()
                .stream()
                .filter( e -> e.getValue().equals( b ) )
                .findAny()
                .map( Map.Entry::getKey )
                .orElse( Strings.UNKNOWN )
            )
            .collect( toList() );
    }
}
