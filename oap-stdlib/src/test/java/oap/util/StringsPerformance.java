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

import oap.testng.AbstractPerformance;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.util.HashSet;

@Test( enabled = false )
public class StringsPerformance extends AbstractPerformance {
    private static String test_remove_set( String str, char... characters ) {
        if( StringUtils.indexOfAny( str, characters ) < 0 ) return str;

        final HashSet<Character> set = new HashSet<>( characters.length );
        for( char ch : characters ) set.add( ch );

        char[] output = new char[str.length()];
        int i = 0;

        for( char ch : str.toCharArray() ) {
            if( !set.contains( ch ) )
                output[i++] = ch;
        }

        return new String( output, 0, i );
    }

    private static String test_remove_bitset( String str, char... characters ) {
        if( StringUtils.indexOfAny( str, characters ) < 0 ) return str;

        final BitSet set = new BitSet( 256 );

        for( char ch : characters ) set.set( ch );

        char[] output = new char[str.length()];
        int i = 0;

        for( char ch : str.toCharArray() ) {
            if( !set.get( ch ) )
                output[i++] = ch;
        }

        return new String( output, 0, i );
    }

    @Test( enabled = false )
    public void testRemove() {
        final int samples = 10000000;
        final int experiments = 5;
        benchmark( "remove-bit-set", samples, experiments, ( i ) -> {
            test_remove_bitset( "12345", ' ', '-' );
            test_remove_bitset( "-123 - 45-", ' ', '-', '_' );
            test_remove_bitset( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } );

        benchmark( "remove", samples, experiments, ( i ) -> {
//            Strings.remove( "12345", ' ', '-' );
//            Strings.remove( "-123 - 45-", ' ', '-', '_' );
            Strings.remove( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } );

        benchmark( "remove-set", samples, experiments, ( i ) -> {
//            test_remove_set( "12345", ' ', '-' );
//            test_remove_set( "-123 - 45-", ' ', '-', '_' );
            test_remove_set( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } );

    }

}
