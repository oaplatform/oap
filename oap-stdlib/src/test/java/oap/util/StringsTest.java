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

import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Set;

import static oap.testng.Asserts.assertString;
import static oap.util.Functions.empty.reject;
import static oap.util.Pair.__;
import static oap.util.Strings.FriendlyIdOption.FILL;
import static oap.util.Strings.FriendlyIdOption.NO_VOWELS;
import static org.assertj.core.api.Assertions.assertThat;

public class StringsTest {
    @Test
    public void toHexStringLong() {
        assertString( Strings.toHexString( 0xFF000000L ) ).isEqualTo( "FF000000" );
        assertString( Strings.toHexString( 0x10L ) ).isEqualTo( "10" );
        assertString( Strings.toHexString( 0x101L ) ).isEqualTo( "101" );
        long millis = DateTimeUtils.currentTimeMillis();
        assertString( Strings.toHexString( millis ) ).isEqualTo( Long.toHexString( millis ).toUpperCase() );
    }

    @Test
    public void toHexStringBytes() {
        assertString( Strings.toHexString( new byte[] { 16 } ) ).isEqualTo( "10" );
        assertString( Strings.toHexString( new byte[] { 1, 10, 120, -78 } ) ).isEqualTo( "010A78B2" );
        assertString( Strings.toHexString( new byte[] { 40, -78, -67, 42, -93, -91 } ) ).isEqualTo( "28B2BD2AA3A5" );
    }


    @Test
    public void substringAfter() {
        assertString( Strings.substringAfter( "/bbb/aaa", "/bbb" ) ).isEqualTo( "/aaa" );
    }

    @Test
    public void substringBeforeLast() {
        assertString( Strings.substringBeforeLast( "aa.conf.bak", "." ) ).isEqualTo( "aa.conf" );
    }

    @Test
    public void split() {
        assertThat( Strings.split( "aaaa.bb.cc", "." ) ).isEqualTo( __( "aaaa", "bb.cc" ) );
    }

    @Test
    public void regex() {
        assertThat( Strings.regexAll( "aaaXbbb:cccXddd", "X([^:]*)" ) ).containsExactly( "bbb", "ddd" );
        assertString( Strings.regex( "aaaXbbb:cccXddd", "X([^:]*)" ) ).isEqualTo( "bbb" );
    }

    @Test
    public void indexOfAny() {
        assertThat( Strings.indexOfAny( "test", "e", 0 ) ).isEqualTo( 1 );
        assertThat( Strings.indexOfAny( "test", "et", 0 ) ).isEqualTo( 0 );
        assertThat( Strings.indexOfAny( "test", "bso", 1 ) ).isEqualTo( 2 );
    }

    @Test
    public void isGuid() {
        assertThat( Strings.isGuid( "22345200-abe8-4f60-90c8-0d43c5f6c0f6" ) ).isTrue();
        assertThat( Strings.isGuid( "2i345200-abe8-4f60-90c8-0d43c5f6c0f6" ) ).isFalse();
    }

    @Test
    public void toSyntheticGuid() {
        assertString( Strings.toSyntheticGuid( "a" ) ).isEqualTo( "0CC175B9-C0F1-B6A8-31C3-99E269772661" );
        assertThat( Strings.isGuid( Strings.toSyntheticGuid( "a" ) ) ).isTrue();
    }

    @Test
    public void remove() {
        assertString( Strings.remove( "12345", ' ', '-' ) ).isEqualTo( "12345" );
        assertString( Strings.remove( "-123 - 45-", ' ', '-' ) ).isEqualTo( "12345" );
    }

    @Test
    public void fill() {
        assertString( Strings.fill( "12", 2 ) ).isEqualTo( "1212" );
    }

    @Test
    public void deepToString() {
        assertString( Strings.deepToString( null ) ).isEqualTo( "null" );
        assertString( Strings.deepToString( new Object[] { "x", "y" } ) ).isEqualTo( "[x, y]" );
        assertString( Strings.deepToString( new int[] { 1, 2 } ) ).isEqualTo( "[1, 2]" );
        assertString( Strings.deepToString( "aaa" ) ).isEqualTo( "aaa" );
        assertString( Strings.deepToString( new Object[] { new Object[] { "x" }, "y" } ) ).isEqualTo( "[[x], y]" );
    }

    @Test
    public void replace() {
        assertString( Strings.replace( "test", "a", "b" ) ).isEqualTo( "test" );
        assertString( Strings.replace( "test", "te", "b" ) ).isEqualTo( "bst" );
        assertString( Strings.replace( "test", "st", "b" ) ).isEqualTo( "teb" );
        assertString( Strings.replace( "test", "es", "b" ) ).isEqualTo( "tbt" );
    }

    @Test
    public void toUserFriendlyId() {
        assertString( Strings.toUserFriendlyId( "some text", 7, reject(), NO_VOWELS, FILL ) )
            .isEqualTo( "SMTXTXX" );
        assertString( Strings.toUserFriendlyId( "another text", 7, reject(), NO_VOWELS, FILL ) )
            .isEqualTo( "NTHRTXT" );

        assertString( Strings.toUserFriendlyId( "some text", 7, reject(), NO_VOWELS ) )
            .isEqualTo( "SMTXT" );

        assertString( Strings.toUserFriendlyId( "some text", 7, reject() ) )
            .isEqualTo( "SOMETEX" );
        assertString( Strings.toUserFriendlyId( "another text", 7, reject() ) )
            .isEqualTo( "ANOTHER" );

        Set<String> items = Sets.empty();
        for( int i = 0; i < 39; i++ )
            items.add( Strings.toUserFriendlyId( "some text", 7, items::contains, NO_VOWELS, FILL ) );

        assertThat( items ).containsExactly(
            "SMTXTXX",
            "SMTXTX0",
            "SMTXTX1",
            "SMTXTX2",
            "SMTXTX3",
            "SMTXTX4",
            "SMTXTX5",
            "SMTXTX6",
            "SMTXTX7",
            "SMTXTX8",
            "SMTXTX9",
            "SMTXTXA",
            "SMTXTXB",
            "SMTXTXC",
            "SMTXTXD",
            "SMTXTXE",
            "SMTXTXF",
            "SMTXTXG",
            "SMTXTXH",
            "SMTXTXI",
            "SMTXTXJ",
            "SMTXTXK",
            "SMTXTXL",
            "SMTXTXM",
            "SMTXTXN",
            "SMTXTXO",
            "SMTXTXP",
            "SMTXTXQ",
            "SMTXTXR",
            "SMTXTXS",
            "SMTXTXT",
            "SMTXTXU",
            "SMTXTXV",
            "SMTXTXW",
            "SMTXTXY",
            "SMTXTXZ",
            "SMTXT10",
            "SMTXT11",
            "SMTXT12"
        );
    }

    @Test
    public void join() {
        assertThat( Strings.join( ";", Lists.of( 1, 2, 3 ), "[", "]" ) ).isEqualTo( "[1;2;3]" );
        assertThat( Strings.join( ";", Lists.of( 1, 2, 3 ), "[", "]", "'" ) ).isEqualTo( "['1';'2';'3']" );
        assertThat( Strings.join( ";", Lists.of( 1, 2, 3 ) ) ).isEqualTo( "1;2;3" );
        assertThat( Strings.join( ";", true, Lists.of( 1, null, 3 ) ) ).isEqualTo( "1;3" );
    }
}
