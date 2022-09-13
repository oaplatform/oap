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

import oap.id.Identifier;
import oap.reflect.Reflect;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static oap.id.Identifier.Option.COMPACT;
import static oap.id.Identifier.Option.FILL;
import static oap.util.Pair.__;
import static oap.util.function.Functions.empty.reject;
import static org.assertj.core.api.Assertions.assertThat;

public class StringsTest {
    @Test
    public void toHexStringLong() {
        assertThat( Strings.toHexString( 0xFF000000L ) ).isEqualTo( "FF000000" );
        assertThat( Strings.toHexString( 0x10L ) ).isEqualTo( "10" );
        assertThat( Strings.toHexString( 0x101L ) ).isEqualTo( "101" );
        long millis = DateTimeUtils.currentTimeMillis();
        assertThat( Strings.toHexString( millis ) ).isEqualTo( Long.toHexString( millis ).toUpperCase() );
    }

    @Test
    public void toHexStringBytes() {
        assertThat( Strings.toHexString( new byte[] { 16 } ) ).isEqualTo( "10" );
        assertThat( Strings.toHexString( new byte[] { 1, 10, 120, -78 } ) ).isEqualTo( "010A78B2" );
        assertThat( Strings.toHexString( new byte[] { 40, -78, -67, 42, -93, -91 } ) ).isEqualTo( "28B2BD2AA3A5" );
    }


    @Test
    public void substringAfter() {
        assertThat( Strings.substringAfter( "/bbb/aaa", "/bbb" ) ).isEqualTo( "/aaa" );
    }

    @Test
    public void substititute() {
        assertThat( Strings.substitute( "${x.a.s} -> ${y.a.s} -> ${a.b.c} -> ${x.d.e}", Reflect.substitutor( Map.of(
            "x", new B( new A( "aaa" ) ),
            "y", new B( new A( "bbb" ) )
        ) ) ) ).isEqualTo( "aaa -> bbb ->  -> " );
    }

    public static class A {
        public String s;

        public A( String s ) {
            this.s = s;
        }
    }

    public static class B {
        public A a;

        public B( A a ) {
            this.a = a;
        }
    }

    @Test
    public void substringBeforeLast() {
        assertThat( Strings.substringBeforeLast( "aa.conf.bak", "." ) ).isEqualTo( "aa.conf" );
    }

    @Test
    public void split() {
        assertThat( Strings.split( "aaaa.bb.cc", "." ) ).isEqualTo( __( "aaaa", "bb.cc" ) );
    }

    @Test
    public void regex() {
        assertThat( Strings.regexAll( "aaaXbbb:cccXddd", "X([^:]*)" ) ).containsExactly( "bbb", "ddd" );
        assertThat( Strings.regex( "aaaXbbb:cccXddd", "X([^:]*)" ) ).isEqualTo( "bbb" );
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
        assertThat( Strings.toSyntheticGuid( "a" ) ).isEqualTo( "0CC175B9-C0F1-B6A8-31C3-99E269772661" );
        assertThat( Strings.isGuid( Strings.toSyntheticGuid( "a" ) ) ).isTrue();
    }

    @Test
    public void remove() {
        assertThat( Strings.remove( "12345", ' ', '-' ) ).isEqualTo( "12345" );
        assertThat( Strings.remove( "-123 - 45-", ' ', '-' ) ).isEqualTo( "12345" );
    }

    @Test
    public void fill() {
        assertThat( Strings.fill( "12", 2 ) ).isEqualTo( "1212" );
    }

    @Test
    public void deepToString() {
        assertThat( Strings.deepToString( null ) ).isEqualTo( "null" );
        assertThat( Strings.deepToString( new Object[] { "x", "y" } ) ).isEqualTo( "[x, y]" );
        assertThat( Strings.deepToString( new int[] { 1, 2 } ) ).isEqualTo( "[1, 2]" );
        assertThat( Strings.deepToString( "aaa" ) ).isEqualTo( "aaa" );
        assertThat( Strings.deepToString( new Object[] { new Object[] { "x" }, "y" } ) ).isEqualTo( "[[x], y]" );
    }

    @Test
    public void replace() {
        assertThat( Strings.replace( "test", "a", "b" ) ).isEqualTo( "test" );
        assertThat( Strings.replace( "test", "te", "b" ) ).isEqualTo( "bst" );
        assertThat( Strings.replace( "test", "st", "b" ) ).isEqualTo( "teb" );
        assertThat( Strings.replace( "test", "es", "b" ) ).isEqualTo( "tbt" );
    }

    @Test
    public static void toUserFriendlyId() {
        assertThat( Identifier.generate( "some text", 7, reject(), COMPACT, FILL ) )
            .isEqualTo( "SMTXTXX" );
        assertThat( Identifier.generate( "another text", 7, reject(), COMPACT, FILL ) )
            .isEqualTo( "NTHRTXT" );

        assertThat( Identifier.generate( "some text", 7, reject(), COMPACT ) )
            .isEqualTo( "SMTXT" );

        assertThat( Identifier.generate( "some text", 7, reject() ) )
            .isEqualTo( "SOMETEX" );
        assertThat( Identifier.generate( "another text", 7, reject() ) )
            .isEqualTo( "ANOTHER" );

        Set<String> items = Sets.empty();
        for( int i = 0; i < 39; i++ )
            items.add( Identifier.generate( "some text", 7, items::contains, COMPACT, FILL ) );

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

    @Test
    public void toAccessKey() {
        assertThat( Strings.toAccessKey( "j.smith@smith.com" ) ).isEqualTo( "SMVRLFSMTXJH" );
        assertThat( Strings.toAccessKey( "j.smith@smith.com", 16 ) ).isEqualTo( "MWLFJHCSRMSHVHTX" );
        assertThat( Strings.toAccessKey( "j@smith.com" ) ).isEqualTo( "SQNRMFCMNUJH" );
        assertThat( Strings.toAccessKey( "a" ) ).isEqualTo( "VKUYJXLWMITZ" );
        assertThat( Strings.toAccessKey( "A" ) ).isEqualTo( "PWOSXRVQUYNT" );
        assertThat( Strings.toAccessKey( "b" ) ).isEqualTo( "DKCGLFJEIMBH" );
        assertThat( Strings.toAccessKey( "/" ) ).isEqualTo( "XMWQLZNYOKVP" );
        assertThat( Strings.toAccessKey( "@" ) ).isEqualTo( "OVNRWQUPTXMS" );
    }

}
