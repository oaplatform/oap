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

import java.util.BitSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Igor Petrenko on 04.04.2016.
 */
public class StringBitsTest extends AbstractTest {
    @Test
    public void testValueOfInt() throws Exception {
        final StringBits stringBits = new StringBits();
        final long test = stringBits.computeIfAbsent( "test" );

        assertThat( stringBits.valueOf( test ) ).isEqualTo( "test" );
        assertThat( stringBits.valueOf( test + 1 ) ).isEqualTo( Strings.UNKNOWN );
    }

    @Test
    public void testValueOfInts() throws Exception {
        final StringBits stringBits = new StringBits();
        final long test1 = stringBits.computeIfAbsent( "test" );
        final long test2 = stringBits.computeIfAbsent( "test2" );

        assertThat( stringBits.valueOf( new int[]{ ( int ) test2, ( int ) test1 } ) ).containsExactly( "test2", "test" );
        assertThat( stringBits.valueOf( new int[]{ ( int ) test1, ( int ) test2 + 1 } ) ).containsExactly( "test", Strings.UNKNOWN );
    }

    @Test
    public void testValueOfLongs() throws Exception {
        final StringBits stringBits = new StringBits();
        final long[] test = stringBits.computeIfAbsent( java.util.Arrays.asList( "test", "test2" ) );

        assertThat( stringBits.valueOf( new long[]{ test[1], test[0] } ) ).containsExactly( "test2", "test" );
        assertThat( stringBits.valueOf( new long[]{ test[0], test[1] + 1 } ) ).containsExactly( "test", Strings.UNKNOWN );
    }

    @Test
    public void testValueOfBits() throws Exception {
        final StringBits stringBits = new StringBits();
        final long test1 = stringBits.computeIfAbsent( "test" );

        final BitSet bitSet = new BitSet();
        bitSet.set( ( int ) test1 );
        bitSet.set( ( int ) ( test1 + 10 ) );

        assertThat( stringBits.valueOf( bitSet ) ).containsExactly( "test", Strings.UNKNOWN );
    }
}
