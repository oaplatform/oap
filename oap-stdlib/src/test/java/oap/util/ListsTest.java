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

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ListsTest {
    @Test
    public void concat() {
        List<String> strings = Lists.of( "a", "b" );
        List<Integer> ints = Lists.of( 1, 2 );
        assertThat( Lists.concat( strings, ints ) )
            .containsExactly( "a", "b", 1, 2 );
    }

    @Test
    public void filter() {
        List<Integer> list = Lists.of( 1, 2, 4, 5 );

        assertThat( Lists.filter( list, i -> i > 2 ) ).containsExactly( 4, 5 );
        assertThat( Lists.filter( list, i -> i > 5 ) ).isEmpty();
        assertThat( Lists.filter( list, i -> i == 2 ) ).containsExactly( 2 );
    }

    @Test
    public void partition() {
        ArrayList<Integer> list = Lists.of( 1, 2, 1, 4 );

        Pair<List<Integer>, List<Integer>> partition = Lists.partition( list, v -> v > 2 );

        assertThat( partition._1 ).containsExactly( 4 );
        assertThat( partition._2 ).containsExactly( 1, 2, 1 );
    }

    @Test
    public void sxx() {
        Runnable f = () -> {};
        System.out.println( Try.catching( f ) );
    }


    @Test
    public void testMapToIntArray() {
        assertThat( Lists.mapToIntArray( asList( "1", "2", "3" ), Integer::parseInt ) ).containsExactly( 1, 2, 3 );
    }
}
