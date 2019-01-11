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

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.util.HashSet;

import static oap.benchmark.Benchmark.benchmark;

@Test
public class StringsPerformance {
    private static String removeSet( String str, char... characters ) {
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

    private static String removeBitset( String str, char... characters ) {
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

    private static String removeBitwise( String str, char... characters ) {
        if( StringUtils.indexOfAny( str, characters ) < 0 ) return str;

        long bitwise0 = 0;
        long bitwise1 = 0;
        long bitwise2 = 0;
        long bitwise3 = 0;

        for( char ch : characters ) {
            long shift = ( 1 << ( ch - 1 ) );
            if( ch < 64 ) bitwise0 |= shift;
            else if( ch < 128 ) bitwise1 |= shift;
            else if( ch < 192 ) bitwise2 |= shift;
            else bitwise3 |= shift;
        }

        char[] output = new char[str.length()];
        int i = 0;

        for( char ch : str.toCharArray() ) {
            long shift = ( 1 << ( ch - 1 ) );
            if( ch < 64 ) {
                if( ( bitwise0 & shift ) == 0 )
                    output[i++] = ch;
            } else if( ch < 128 ) {
                if( ( bitwise1 & shift ) == 0 )
                    output[i++] = ch;
            } else if( ch < 192 ) {
                if( ( bitwise2 & shift ) == 0 )
                    output[i++] = ch;
            } else {
                if( ( bitwise3 & shift ) == 0 )
                    output[i++] = ch;
            }
        }

        return new String( output, 0, i );
    }

    @Test
    public void remove() {
        final int samples = 10000000;

        benchmark( "remove-bidwise", samples, () -> {
            removeBitwise( "12345", ' ', '-' );
            removeBitwise( "-123 - 45-", ' ', '-', '_' );
            removeBitwise( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } ).run();

        benchmark( "remove-bit-set", samples, () -> {
            removeBitset( "12345", ' ', '-' );
            removeBitset( "-123 - 45-", ' ', '-', '_' );
            removeBitset( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } ).run();

        benchmark( "remove", samples, () -> {
            Strings.remove( "12345", ' ', '-' );
            Strings.remove( "-123 - 45-", ' ', '-', '_' );
            Strings.remove( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } ).run();

        benchmark( "remove-set", samples, () -> {
            removeSet( "12345", ' ', '-' );
            removeSet( "-123 - 45-", ' ', '-', '_' );
            removeSet( "-123 - 45-", ' ', '-', 'a', 'b', 'c', 'd', 'e' );
        } ).run();

    }

    @Test
    public void testReplace() {
        final int samples = 5000000;
        final String text = "sdhfg dkuhsdorifue itfgorufgeryjsfgrhfgj hsdgj";

        benchmark( "Strings::replace", samples, () -> Strings.replace( text, "fg", "obc" ) ).run();
        benchmark( "String::replace", samples, () -> text.replace( "fg", "obc" ) ).run();
        benchmark( "StringUtils::replace", samples, () -> StringUtils.replace( text, "fg", "obc" ) ).run();
    }

}
