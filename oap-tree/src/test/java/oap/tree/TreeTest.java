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

import static oap.tree.Dimension.ENUM;
import static oap.tree.Dimension.LONG;
import static oap.tree.Dimension.STRING;
import static oap.tree.Tree.ANY;
import static oap.tree.Tree.s;
import static oap.tree.TreeTest.TestEnum.Test1;
import static oap.tree.TreeTest.TestEnum.Test2;
import static oap.tree.TreeTest.TestEnum.Test3;
import static oap.tree.TreeTest.TestEnum.Test4;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreeTest {
    private static <T> Tree.ValueData<T>[] a( Tree.ValueData<T>... values ) {
        return values;
    }

    @Test
    public void testFindOneDimension() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1" ) )
            .load( a( s( "1", 1L ), s( "2", 2L ), s( "3", 3L ), s( "33", 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 3L ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( 5L ) ).isEmpty();
    }

    @Test
    public void testEnum() {
        final Tree<String> tree = Tree
            .<String>tree( ENUM( "d1" ) )
            .load( a( s( "1", Test1 ), s( "2", Test2 ), s( "3", Test3 ), s( "33", Test3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( Test1 ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( Test2 ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( Test3 ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( Test4 ) ).isEmpty();
    }

    @Test
    public void testString() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1" ) )
            .load( a( s( "1", "s1" ), s( "2", "s2" ), s( "3", "s3" ), s( "33", "s3" ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( "s1" ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( "s2" ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( "s3" ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( "s4" ) ).isEmpty();
    }

    @Test
    public void testFindTwoDimension() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1" ), LONG( "d2" ) )
            .load( a( s( "1", 1L, 1L ), s( "2", 2L, 2L ), s( "3", 1L, 3L ), s( "33", 1L, 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L, 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L, 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 1L, 3L ) ).containsOnlyOnce( "3", "33" );

        assertThat( tree.find( 1L, 2L ) ).isEmpty();
        assertThat( tree.find( 3L, 3L ) ).isEmpty();
    }

    @Test
    public void testFindAny() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1" ), LONG( "d2" ) )
            .load( a( s( "1", 1L, ANY ), s( "2", 2L, 2L ), s( "3", 1L, 3L ), s( "33", 1L, 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( 1L, 1L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( 2L, 2L ) ).containsOnlyOnce( "2" );
        assertThat( tree.find( 1L, 3L ) ).containsOnlyOnce( "1", "3", "33" );

        assertThat( tree.find( 1L, 2L ) ).containsOnlyOnce( "1" );
        assertThat( tree.find( ANY, 3L ) ).containsOnlyOnce( "3", "33" );
    }

    @Test
    public void testTrace() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1" ), ENUM( "d2" ) )
            .load( a( s( "1", 1L, Test1 ), s( "2", 2L, Test2 ), s( "3", 1L, Test3 ), s( "33", 1L, Test3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( 1L, Test2 ) ).isEqualTo( "" +
            "33 -> (1,Test2) not in: [(1,Test3)]\n" +
            "1 -> (1,Test2) not in: [(1,Test1)]\n" +
            "2 -> (1,Test2) not in: [(2,Test2)]\n" +
            "3 -> (1,Test2) not in: [(1,Test3)]\n" );
        assertThat( tree.trace( 3L, Test3 ) ).isEqualTo( "" +
            "33 -> (3,Test3) not in: [(1,Test3)]\n" +
            "1 -> (3,Test3) not in: [(1,Test1)]\n" +
            "2 -> (3,Test3) not in: [(2,Test2)]\n" +
            "3 -> (3,Test3) not in: [(1,Test3)]\n" );

        assertThat( tree.trace( 4L, Test4 ) ).isEqualTo( "" +
            "33 -> (4,UNKNOWN) not in: [(1,Test3)]\n" +
            "1 -> (4,UNKNOWN) not in: [(1,Test1)]\n" +
            "2 -> (4,UNKNOWN) not in: [(2,Test2)]\n" +
            "3 -> (4,UNKNOWN) not in: [(1,Test3)]\n" );
        assertThat( tree.trace( 1L, Test1 ) ).isEqualTo( "" +
            "33 -> (1,Test1) not in: [(1,Test3)]\n" +
            "2 -> (1,Test1) not in: [(2,Test2)]\n" +
            "3 -> (1,Test1) not in: [(1,Test3)]\n" );
    }

    public enum TestEnum {
        Test1, Test2, Test3, Test4
    }
}