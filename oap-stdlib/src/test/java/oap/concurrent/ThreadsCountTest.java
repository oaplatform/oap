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

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ThreadsCountTest {
    private static final int CORES = 20;

    @Test
    public void auto() {
        assertThat( Threads.count( 0, 7, CORES ) ).isEqualTo( 7 );
        assertThat( Threads.count( -0.0, 7, CORES ) ).isEqualTo( 7 );
        assertThat( Threads.count( 0, 0, CORES ) ).isEqualTo( 1 );
    }

    @Test
    public void integer() {
        assertThat( Threads.count( 4, 7, CORES ) ).isEqualTo( 4 );
        assertThat( Threads.count( 64, 7, CORES ) ).isEqualTo( 64 );
        assertThat( Threads.count( -1, 7, CORES ) ).isEqualTo( 19 );
        assertThat( Threads.count( -19, 7, CORES ) ).isEqualTo( 1 );
        assertThat( Threads.count( -25, 7, CORES ) ).isEqualTo( 1 );
    }

    @Test
    public void fraction() {
        assertThat( Threads.count( 1.2, 7, CORES ) ).isEqualTo( 24 );
        assertThat( Threads.count( 0.5, 7, CORES ) ).isEqualTo( 10 );
        assertThat( Threads.count( 0.01, 7, CORES ) ).isEqualTo( 1 );
        assertThat( Threads.count( -1.2, 7, CORES ) ).isEqualTo( 17 );
        assertThat( Threads.count( -0.5, 7, CORES ) ).isEqualTo( 40 );
    }

    @Test
    public void notFinite() {
        assertThatThrownBy( () -> Threads.count( Double.NaN, 7, CORES ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> Threads.count( Double.POSITIVE_INFINITY, 7, CORES ) ).isInstanceOf( IllegalArgumentException.class );
    }
}
