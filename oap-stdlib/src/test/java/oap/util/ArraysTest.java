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

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;


public class ArraysTest {
    @Test
    public void reversed() {
        assertThat( Arrays.ints().range( 0, 10 ).reversed() )
            .containsExactly( 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 );
    }

    @Test
    public void splitAt() {
        int[] a = { 1, 2, 3, 4, 5 };
        assertThat( Arrays.splitAt( 3, a ) )
            .isEqualTo( __( new int[] { 1, 2, 3 }, new int[] { 4, 5 } ) );
    }

    @Test
    public void splitBy() {
        assertThat( Arrays.splitBy( 2, 1, 2, 3, 4, 5, 6 ) )
            .containsExactly( new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } } );
        assertThat( Arrays.splitBy( 6, 1, 2, 3, 4, 5, 6 ) )
            .containsExactly( new Integer[][] { { 1, 2, 3, 4, 5, 6 } } );
        assertThat( Arrays.splitBy( 3, 1, 2, 3, 4, 5, 6 ) )
            .containsExactly( new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } } );
        assertThat( Arrays.splitBy( 1, 1, 2, 3, 4, 5, 6 ) )
            .containsExactly( new Integer[][] { { 1 }, { 2 }, { 3 }, { 4 }, { 5 }, { 6 } } );
    }

    @Test
    public void filter() {
        assertThat( Arrays.filter( x -> x % 2 == 0, 1, 2, 3, 4, 5, 6 ) )
            .containsExactly( 2, 4, 6 );
    }
}
