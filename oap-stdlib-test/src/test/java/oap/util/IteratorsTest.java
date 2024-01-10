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

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;


public class IteratorsTest {

    @Test
    public void flatTraverse() {
        var iterator = Iterators.flatTraverse( 0, x -> x < 3
            ? Lists.of( x + 1, x + 2 ).iterator()
            : Iterators.empty() );
        assertThat( iterator ).toIterable().containsExactly( 0, 1, 2, 3, 4, 3, 2, 3, 4 );
    }

    @Test
    public void traverse() {
        Iterator<Integer> traverse = Iterators.traverse( 1, x -> x < 10 ? x + 1 : null );
        assertThat( traverse ).toIterable().containsExactly( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );
    }


}
