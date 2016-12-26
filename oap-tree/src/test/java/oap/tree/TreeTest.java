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

import static oap.tree.Tree.ANY;
import static oap.tree.Tree.s;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreeTest {
    private static <T> T[] a( T... values ) {
        return values;
    }

    @Test
    public void testFindOneDimension() {
        final Tree<String> tree = new Tree<>( "d1" );

        tree.load( a( s( "1", 1 ), s( "2", 2 ), s( "3", 3 ), s( "33", 3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 3L ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( 5L ) ).isEmpty();
    }

    @Test
    public void testFindTwoDimension() {
        final Tree<String> tree = new Tree<>( "d1", "d2" );

        tree.load( a( s( "1", 1, 1 ), s( "2", 2, 2 ), s( "3", 1, 3 ), s( "33", 1, 3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L, 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L, 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 1L, 3L ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( 1L, 2L ) ).isEmpty();
        assertThat( tree.find( 3L, 3L ) ).isEmpty();
    }

    @Test
    public void testFindAny() {
        final Tree<String> tree = new Tree<>( "d1", "d2" );

        tree.load( a( s( "1", 1, ANY ), s( "2", 2, 2 ), s( "3", 1, 3 ), s( "33", 1, 3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L, 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L, 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 1L, 3L ) ).containsOnlyOnce( "1", "3", "33" );

        assertThat( tree.find( 1L, 2L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( ANY, 3L ) ).containsOnlyOnce( "3", "33" );
    }

    @Test
    public void testTrace() {
        final Tree<String> tree = new Tree<>( "d1", "d2" );

        tree.load( a( s( "1", 1, 1 ), s( "2", 2, 2 ), s( "3", 1, 3 ), s( "33", 1, 3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( 1L, 2L ) ).isEqualTo( "" +
            "33 -> (1,2) not in: [(1,3)]\n" +
            "1 -> (1,2) not in: [(1,1)]\n" +
            "2 -> (1,2) not in: [(2,2)]\n" +
            "3 -> (1,2) not in: [(1,3)]\n" );
        assertThat( tree.trace( 3L, 3L ) ).isEqualTo( "" +
            "33 -> (3,3) not in: [(1,3)]\n" +
            "1 -> (3,3) not in: [(1,1)]\n" +
            "2 -> (3,3) not in: [(2,2)]\n" +
            "3 -> (3,3) not in: [(1,3)]\n" );

        assertThat( tree.trace( 4L, 4L ) ).isEqualTo( "" +
            "33 -> (4,4) not in: [(1,3)]\n" +
            "1 -> (4,4) not in: [(1,1)]\n" +
            "2 -> (4,4) not in: [(2,2)]\n" +
            "3 -> (4,4) not in: [(1,3)]\n" );
        assertThat( tree.trace( 1L, 1L ) ).isEqualTo( "" +
            "33 -> (1,1) not in: [(1,3)]\n" +
            "2 -> (1,1) not in: [(2,2)]\n" +
            "3 -> (1,1) not in: [(1,3)]\n" );
    }
}