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

package oap.concurrent;

import oap.concurrent.CircularBuffer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CircularBufferTest {

    @Test
    public void cycle() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>( Integer.class, 3 );
        assertThat( buffer.getElements() ).containsExactly();
        buffer.add( 1 );
        buffer.add( 2 );
        assertThat( buffer.getElements() ).containsExactly( 1, 2 );
        buffer.add( 3 );
        buffer.add( 4 );
        assertThat( buffer.getElements() ).containsExactly( 2, 3, 4 );
        buffer.add( 4 );
        assertThat( buffer.getElements() ).containsExactly( 3, 4, 4 );
    }

}
