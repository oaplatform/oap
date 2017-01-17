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

package oap.tree;

import org.testng.annotations.Test;

import static oap.tree.Dimension.ARRAY_LONG;
import static oap.tree.Tree.a;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreeArrayTraceTest {
    @Test
    public void testArrayTrace() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", false ) )
            .load( l(
                v( "1", l( a( true, 1L, 2L ) ) ),
                v( "2", l( a( true, 1L, 2L ) ) ),
                v( "3", l( a( true, 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "query = [d1:1]\nALL OK" );
        assertThat( tree.trace( l( 3L ) ) ).isEqualTo( "query = [d1:3]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1,2] CONTAINS 3\n" +
            "2: \n" +
            "    d1/0: [1,2] CONTAINS 3" );
        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1,2] CONTAINS 5\n" +
            "2: \n" +
            "    d1/0: [1,2] CONTAINS 5\n" +
            "3: \n" +
            "    d1/0: [1,2,3] CONTAINS 5" );
    }

    @Test
    public void testArrayExcludeTrace() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", false ) )
            .load( l(
                v( "1", l( a( false, 1L, 2L ) ) ),
                v( "2", l( a( false, 2L ) ) ),
                v( "3", l( a( false, 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 2L ) ) ).isEqualTo( "query = [d1:2]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1,2] NOT_CONTAINS 2\n" +
            "2: \n" +
            "    d1/0: [2] NOT_CONTAINS 2\n" +
            "3: \n" +
            "    d1/0: [1,2,3] NOT_CONTAINS 2" );
        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "query = [d1:1]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1,2] NOT_CONTAINS 1\n" +
            "3: \n" +
            "    d1/0: [1,2,3] NOT_CONTAINS 1" );

        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\nALL OK" );
    }
}