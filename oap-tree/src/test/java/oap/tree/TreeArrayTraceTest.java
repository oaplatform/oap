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

import java.util.Optional;

import static oap.tree.Dimension.ARRAY_ENUM;
import static oap.tree.Dimension.ARRAY_LONG;
import static oap.tree.Tree.ArrayOperation.AND;
import static oap.tree.Tree.ArrayOperation.NOT;
import static oap.tree.Tree.ArrayOperation.OR;
import static oap.tree.Tree.a;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static oap.tree.TreeTest.TestEnum.Test1;
import static oap.tree.TreeTest.TestEnum.Test2;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeArrayTraceTest {
    @Test
    public void testArrayTrace() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( OR, 1L, 2L ) ) ),
                v( "2", l( a( OR, 1L, 2L ) ) ),
                v( "3", l( a( OR, 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "query = [d1:1]\nALL OK" );
        assertThat( tree.trace( l( 3L ) ) ).isEqualTo( "query = [d1:3]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] CONTAINS 3\n"
            + "2: \n"
            + "    d1/0: [1,2] CONTAINS 3" );
        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] CONTAINS 5\n"
            + "2: \n"
            + "    d1/0: [1,2] CONTAINS 5\n"
            + "3: \n"
            + "    d1/0: [1,2,3] CONTAINS 5" );
    }

    @Test
    public void testArrayTraceAND() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( AND, 1L, 2L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( l( 1L, 2L ) ) ) ).isEqualTo( "query = [d1:[1, 2]]\nALL OK" );
        assertThat( tree.trace( l( l( 3L ) ) ) ).isEqualTo( "query = [d1:[3]]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] CONTAINS_ALL [3]" );
    }

    @Test
    public void testArrayExcludeTrace() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( NOT, 1L, 2L ) ) ),
                v( "2", l( a( NOT, 2L ) ) ),
                v( "3", l( a( NOT, 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 2L ) ) ).isEqualTo( "query = [d1:2]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] NOT_CONTAINS 2\n"
            + "2: \n"
            + "    d1/0: [2] NOT_CONTAINS 2\n"
            + "3: \n"
            + "    d1/0: [1,2,3] NOT_CONTAINS 2" );
        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "query = [d1:1]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] NOT_CONTAINS 1\n"
            + "3: \n"
            + "    d1/0: [1,2,3] NOT_CONTAINS 1" );

        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\nALL OK" );
    }

    @Test
    public void testRequired() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( OR, 1L, 2L ) ) ),
                v( "2", l( a( OR, 1L, 2L ) ) ),
                v( "3", l( a( OR, 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( l() ) ) ).isEqualTo( "query = [d1:UNKNOWN]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [1,2] CONTAINS []\n"
            + "2: \n"
            + "    d1/0: [1,2] CONTAINS []\n"
            + "3: \n"
            + "    d1/0: [1,2,3] CONTAINS []" );
    }

    @Test
    public void testTraceEmptyQueryEnum() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_ENUM( "d1", TreeTraceTest.TestEnum.class, TreeTraceTest.TestEnum.UNKNOWN ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", l( a( OR, Test1, Test2 ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( l() ) ) ).isEqualTo( "query = [d1:UNKNOWN]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [Test1,Test2] CONTAINS []" );

        assertThat( tree.trace( l( Optional.empty() ) ) ).isEqualTo( "query = [d1:UNKNOWN]\n"
            + "Expecting:\n"
            + "1: \n"
            + "    d1/0: [Test1,Test2] CONTAINS UNKNOWN" );
    }
}
