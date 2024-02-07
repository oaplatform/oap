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

package oap.ws.sso;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static oap.testng.Asserts.assertString;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

public class UserProviderTest {
    @Test
    public void toAccessKey() {
        assertString( UserProvider.toAccessKey( "j.smith@smith.com" ) ).isEqualTo( "HXMLFVRJTSMS" );
        assertString( UserProvider.toAccessKey( "j@smith.com" ) ).isEqualTo( "HUMMFNRJNSQC" );
        assertString( UserProvider.toAccessKey( "a" ) ).isEqualTo( "ZIWJXUYTMVKL" );
        assertString( UserProvider.toAccessKey( "A" ) ).isEqualTo( "TYQXROSNUPWV" );
        assertString( UserProvider.toAccessKey( "b" ) ).isEqualTo( "HMELFCGBIDKJ" );
        assertString( UserProvider.toAccessKey( "/" ) ).isEqualTo( "PKYLZWQVOXMN" );
        assertString( UserProvider.toAccessKey( "@" ) ).isEqualTo( "SXPWQNRMTOVU" );
    }

    @Test
    public void stable() {
        List<Integer> l1 = Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 );
        List<Integer> l2 = Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 );
        Collections.shuffle( l1, new Random( l1.size() ) );
        System.out.println( l1 );
        Collections.shuffle( l2, new Random( l2.size() ) );
        System.out.println( l2 );
        assertArrayEquals( l1.toArray(), l2.toArray() );
    }
}
