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

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;

public class StringsTest extends AbstractTest {
    @Test
    public void toHexString() {
        assertEquals( Strings.toHexString( new byte[]{ 16 } ), "10" );
        assertEquals( Strings.toHexString( new byte[]{ 1, 10, 120, -78 } ), "010A78B2" );
        assertEquals( Strings.toHexString( new byte[]{ 40, -78, -67, 42, -93, -91 } ), "28B2BD2AA3A5" );
    }

    @Test
    public void substringAfter() {
        assertEquals( Strings.substringAfter( "/bbb/aaa", "/bbb" ), "/aaa" );
    }

    @Test
    public void substringBeforeLast() {
        assertEquals( Strings.substringBeforeLast( "aa.conf.bak", "." ), "aa.conf" );
    }

    @Test
    public void split() {
        assertEquals( Strings.split( "aaaa.bb.cc", "." ), __( "aaaa", "bb.cc" ) );
    }

    @Test
    public void regex() {
        assertEquals( Strings.regexAll( "aaaXbbb:cccXddd", "X([^:]*)" ), Lists.of( "bbb", "ddd" ) );
        assertEquals( Strings.regex( "aaaXbbb:cccXddd", "X([^:]*)" ), "bbb" );
    }

    @Test
    public void testIndexOfAny() {
        assertEquals( Strings.indexOfAny( "test", "e", 0 ), 1 );
        assertEquals( Strings.indexOfAny( "test", "et", 0 ), 0 );
        assertEquals( Strings.indexOfAny( "test", "bso", 1 ), 2 );
    }
}
