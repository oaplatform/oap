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

public class PrioritySetTest {
    @Test
    public void get() {
        PrioritySet<String> set = new PrioritySet<>();
        assertThat( set.add( "a" ) ).isTrue();
        assertThat( set.add( 1, "b" ) ).isTrue();
        assertThat( set.add( 3, "b" ) ).isTrue();
        assertThat( set.add( 3, "b" ) ).isFalse();
        assertThat( set.add( 2, "c" ) ).isTrue();
        assertThat( set ).containsExactly( "a", "c", "b" );
        assertThat( set.add( 4, "c" ) ).isTrue();
        assertThat( set ).containsExactly( "a", "b", "c" );
        assertThat( set ).hasSize( 3 );
    }


}
