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

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Access Key Hash
 * Named after the purpose of generating stable access keys from user readable
 * identifiers such as emails.
 * Works by eliminating most used letters in English from the source and transforming it in stable
 * random(seed=length) manner
 * Collision rate is 25-50% and is insensitive to hash length
 * This hash is supposed to be used with secret key as a pair similar to login/password.
 */
public class AKHash {
    private static final ConcurrentMap<Integer, List<Integer>> cache = new ConcurrentHashMap<>();

    public static String hash( String value ) {
        return hash( value, 12 );
    }

    public static String hash( String value, int length ) {
        var transitions = cache.computeIfAbsent( length, l -> {
            var list = IntStream.range( 0, l ).boxed().collect( Collectors.toList() );
            java.util.Collections.shuffle( list, new Random( list.size() ) );
            return list;
        } );
        StringBuilder result = new StringBuilder();
        for( int t : transitions )
            if( t >= value.length() || !goodCharacter( value.charAt( t ) ) ) {
                var c = value.charAt( t % value.length() );
                var base = Character.toUpperCase( goodCharacter( c ) ? c : 'A' + ( c % 26 ) );
                result.append( ( char ) ( base + t <= 'Z' ? base + t : base - t ) );
            } else result.append( Character.toUpperCase( value.charAt( t ) ) );
        return result.toString();
    }

    private static boolean goodCharacter( char c ) {
        return ( c > 64 && c < 91 || c > 96 && c < 123 )
            && c != 'E' && c != 'e'
            && c != 'T' && c != 't'
            && c != 'A' && c != 'a'
            && c != 'O' && c != 'o'
            && c != 'I' && c != 'i'
            && c != 'N' && c != 'n';
    }
}
