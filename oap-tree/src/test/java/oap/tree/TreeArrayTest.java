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

import static oap.tree.Dimension.LONG;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.tree.Dimension.STRING;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreeArrayTest {
    @Test
    public void testArray() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", true, CONTAINS ) )
            .load( l( v( "1", l( l( 1L, 2L ) ) ), v( "2", l( l( 2L ) ) ), v( "3", l( l( 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnlyOnce( "1", "3" );
        assertThat( tree.find( l( 2L ) ) ).containsOnlyOnce( "1", "2", "3" );
        assertThat( tree.find( l( 3L ) ) ).containsOnlyOnce( "3" );

        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 2 );
        assertThat( ( ( Tree.Node ) tree.root ).sets ).hasSize( 3 );
    }

    @Test
    public void testArrayString() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", true, CONTAINS ) )
            .load( l( v( "1", l( l( "s1", "s2" ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "s1" ) ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( l( "s5" ) ) ).isEmpty();
    }

    @Test
    public void testArrayNotContains() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", true, NOT_CONTAINS ) )
            .load( l( v( "1", l( l( 1L, 2L ) ) ), v( "2", l( l( 2L ) ) ), v( "3", l( l( 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 2L ) ) ).isEmpty();
        assertThat( tree.find( l( 1L ) ) ).containsOnlyOnce( "2" );

        assertThat( tree.find( l( 5L ) ) ).containsOnlyOnce( "1", "2", "3" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 2 );
        assertThat( ( ( Tree.Node ) tree.root ).sets ).hasSize( 3 );
    }

    @Test
    public void testArrayOptimize() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", true, CONTAINS ) )
            .load( l( v( "1", l( l( 1L, 2L ) ) ), v( "2", l( l( 1L, 2L ) ) ), v( "3", l( l( 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnlyOnce( "1", "2", "3" );
        assertThat( tree.find( l( 2L ) ) ).containsOnlyOnce( "1", "2", "3" );

        assertThat( ( ( Tree.Node ) tree.root ).sets ).hasSize( 2 );
    }

    @Test
    public void testArrayTrace() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", true, CONTAINS ) )
            .load( l( v( "1", l( l( 1L, 2L ) ) ), v( "2", l( l( 1L, 2L ) ) ), v( "3", l( l( 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "ALL OK" );
        assertThat( tree.trace( l( 3L ) ) ).isEqualTo( "" +
            "1 -> (3) not in: [({1,2})]\n" +
            "2 -> (3) not in: [({1,2})]\n" );
        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "1 -> (5) not in: [({1,2})]\n" +
            "2 -> (5) not in: [({1,2})]\n" +
            "3 -> (5) not in: [({1,2,3})]\n" );
    }

    @Test
    public void testArrayNotContainsTrace() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", true, NOT_CONTAINS ) )
            .load( l( v( "1", l( l( 1L, 2L ) ) ), v( "2", l( l( 2L ) ) ), v( "3", l( l( 1L, 2L, 3L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 2L ) ) ).isEqualTo( "" +
            "1 -> (2) not in: [(!{1,2})]\n" +
            "2 -> (2) not in: [(!{2})]\n" +
            "3 -> (2) not in: [(!{1,2,3})]\n" );
        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "" +
            "1 -> (1) not in: [(!{1,2})]\n" +
            "3 -> (1) not in: [(!{1,2,3})]\n" );

        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "ALL OK" );
    }
}