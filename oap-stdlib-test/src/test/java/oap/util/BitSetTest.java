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

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitSetTest {
    @Test
    public void maxBit() {
        var bitSet = new BitSet( 10 );
        bitSet.set( 4 );
        assertThat( bitSet.max() ).isEqualTo( 4 );

        bitSet.set( 6 );
        assertThat( bitSet.max() ).isEqualTo( 6 );

        bitSet.set( 6, false );
        assertThat( bitSet.max() ).isEqualTo( 4 );
    }

    @Test
    public void newBitSetFromString() {
        var bs = new BitSet( "1,2,  4, 6 -8" );

        assertThat( bs.get( 0 ) ).isFalse();
        assertThat( bs.get( 1 ) ).isTrue();
        assertThat( bs.get( 2 ) ).isTrue();
        assertThat( bs.get( 3 ) ).isFalse();
        assertThat( bs.get( 4 ) ).isTrue();
        assertThat( bs.get( 5 ) ).isFalse();
        assertThat( bs.get( 6 ) ).isTrue();
        assertThat( bs.get( 7 ) ).isTrue();
        assertThat( bs.get( 8 ) ).isTrue();
        assertThat( bs.get( 9 ) ).isFalse();
    }
}
