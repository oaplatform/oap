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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

public class StringBits {
   private static final int UNKNOWN = 0;

   private final HashMap<String, Integer> bits = new HashMap<>();
   private final AtomicInteger bit = new AtomicInteger( 1 );

   public StringBits() {
      bits.put( Strings.UNKNOWN, UNKNOWN );
   }

   public final synchronized int computeIfAbsent( String name ) {
      return bits.computeIfAbsent( name, n -> bit.getAndIncrement() );
   }

   public final int get( String name ) {
      return bits.getOrDefault( name, UNKNOWN );
   }

   public final int[] get( List<String> name ) {
      int[] result = new int[name.size()];

      for( int i = 0; i < result.length; i++ ) {
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

      values.forEach( v -> bitSet.set( get( v ) ) );
      return bitSet;
   }

   public int size() {
      return bits.size();
   }

   public String valueOf( int bit ) {
      return bits.entrySet()
         .stream()
         .filter( e -> e.getValue() == bit )
         .findAny()
         .map( Map.Entry::getKey )
         .orElse( Strings.UNKNOWN );
   }

   public List<String> valueOf( java.util.BitSet bits ) {
      return valueOf( bits.stream() );
   }

   public List<String> valueOf( long[] bits ) {
      return valueOf( LongStream.of( bits ).mapToInt( l -> ( int ) l ) );
   }

   public List<String> valueOf( int[] bits ) {
      return valueOf( IntStream.of( bits ) );
   }

   private List<String> valueOf( IntStream bits ) {
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
