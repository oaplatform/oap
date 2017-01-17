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
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.GREATER_THEN_OR_EQUAL_TO;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.tree.Dimension.STRING;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static oap.tree.TreeTest.TestEnum.Test1;
import static oap.tree.TreeTest.TestEnum.Test2;
import static oap.tree.TreeTest.TestEnum.Test3;
import static oap.tree.TreeTest.TestEnum.Test4;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreeTraceTest {
    @Test
    public void testTrace() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, false ), ENUM( "d2", TestEnum.class, CONTAINS, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L, Test1 ), v( "2", 2L, Test2 ), v( "3", 1L, Test3 ), v( "33", 1L, Test3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L, Test2 ) ) ).isEqualTo( "query = [d1:1,d2:Test2]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d2/1: [Test3] CONTAINS Test2\n" +
            "1: \n" +
            "    d2/1: [Test1] CONTAINS Test2\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 1\n" +
            "3: \n" +
            "    d2/1: [Test3] CONTAINS Test2" );
        assertThat( tree.trace( l( 3L, Test3 ) ) ).isEqualTo( "query = [d1:3,d2:Test3]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d1/0: [1] CONTAINS 3\n" +
            "1: \n" +
            "    d1/0: [1] CONTAINS 3\n" +
            "    d2/1: [Test1] CONTAINS Test3\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 3\n" +
            "    d2/1: [Test2] CONTAINS Test3\n" +
            "3: \n" +
            "    d1/0: [1] CONTAINS 3" );

        assertThat( tree.trace( l( 4L, Test4 ) ) ).isEqualTo( "query = [d1:4,d2:Test4]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test3] CONTAINS Test4\n" +
            "1: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test1] CONTAINS Test4\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 4\n" +
            "    d2/1: [Test2] CONTAINS Test4\n" +
            "3: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test3] CONTAINS Test4" );
        assertThat( tree.trace( l( 1L, Test1 ) ) ).isEqualTo( "query = [d1:1,d2:Test1]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d2/1: [Test3] CONTAINS Test1\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 1\n" +
            "    d2/1: [Test2] CONTAINS Test1\n" +
            "3: \n" +
            "    d2/1: [Test3] CONTAINS Test1" );
    }

    @Test
    public void testTraceHash() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, false ), ENUM( "d2", TestEnum.class, CONTAINS, false ) )
            .withHashFillFactor( 0 )
            .load( l( v( "1", 1L, Test1 ), v( "2", 2L, Test2 ), v( "3", 1L, Test3 ), v( "33", 1L, Test3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L, Test2 ) ) ).isEqualTo( "query = [d1:1,d2:Test2]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d2/1: [Test3] CONTAINS Test2\n" +
            "1: \n" +
            "    d2/1: [Test1] CONTAINS Test2\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 1\n" +
            "3: \n" +
            "    d2/1: [Test3] CONTAINS Test2" );
        assertThat( tree.trace( l( 3L, Test3 ) ) ).isEqualTo( "query = [d1:3,d2:Test3]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d1/0: [1] CONTAINS 3\n" +
            "1: \n" +
            "    d1/0: [1] CONTAINS 3\n" +
            "    d2/1: [Test1] CONTAINS Test3\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 3\n" +
            "    d2/1: [Test2] CONTAINS Test3\n" +
            "3: \n" +
            "    d1/0: [1] CONTAINS 3" );

        assertThat( tree.trace( l( 4L, Test4 ) ) ).isEqualTo( "query = [d1:4,d2:Test4]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test3] CONTAINS Test4\n" +
            "1: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test1] CONTAINS Test4\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 4\n" +
            "    d2/1: [Test2] CONTAINS Test4\n" +
            "3: \n" +
            "    d1/0: [1] CONTAINS 4\n" +
            "    d2/1: [Test3] CONTAINS Test4" );
        assertThat( tree.trace( l( 1L, Test1 ) ) ).isEqualTo( "query = [d1:1,d2:Test1]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d2/1: [Test3] CONTAINS Test1\n" +
            "2: \n" +
            "    d1/0: [2] CONTAINS 1\n" +
            "    d2/1: [Test2] CONTAINS Test1\n" +
            "3: \n" +
            "    d2/1: [Test3] CONTAINS Test1" );
    }

    @Test
    public void testTraceUNKNOWN() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", CONTAINS, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", "str" ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( "tt" ) ) ).isEqualTo( "query = [d1:tt]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [str] CONTAINS tt" );
        assertThat( tree.trace( l( l( "tt", "bb" ) ) ) ).isEqualTo( "query = [d1:[tt, bb]]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [str] CONTAINS [tt, bb]" );
    }

    @Test
    public void testTraceOrQuery() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, false ) )
            .load( l( v( "1", 1L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( l( 2L, 1L ) ) ) ).isEqualTo( "query = [d1:[2, 1]]\nALL OK" );
        assertThat( tree.trace( l( l( 1L, 2L ) ) ) ).isEqualTo( "query = [d1:[1, 2]]\nALL OK" );
    }

    @Test
    public void testTraceExclude() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", NOT_CONTAINS, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L ), v( "2", 2L ), v( "3", 3L ), v( "33", 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 1L ) ) ).isEqualTo( "query = [d1:1]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1] NOT_CONTAINS 1" );
        assertThat( tree.trace( l( 2L ) ) ).isEqualTo( "query = [d1:2]\n" +
            "Expecting:\n" +
            "2: \n" +
            "    d1/0: [2] NOT_CONTAINS 2" );
        assertThat( tree.trace( l( 3L ) ) ).isEqualTo( "query = [d1:3]\n" +
            "Expecting:\n" +
            "33: \n" +
            "    d1/0: [3] NOT_CONTAINS 3\n" +
            "3: \n" +
            "    d1/0: [3] NOT_CONTAINS 3" );

        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\nALL OK" );
    }

    @Test
    public void testTraceAny() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, false ), LONG( "d2", CONTAINS, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", null, 99L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( null, 1L ) ) ).isEqualTo( "query = [d1:null,d2:1]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d2/1: [99] CONTAINS 1" );
    }

    @Test
    public void testTraceQueryAnyAndQueryRequired() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, true ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( ( Long ) null ) ) ).isEqualTo( "query = [d1:null]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1] CONTAINS null" );
    }

    @Test
    public void testTraceEmptyQuery() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, false ), LONG( "d2", CONTAINS, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L, 2L ), v( "2", 2L, 2L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( l(), l( 3L ) ) ) ).isEqualTo( "query = [d1:[],d2:[3]]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d2/1: [2] CONTAINS [3]\n" +
            "2: \n" +
            "    d2/1: [2] CONTAINS [3]" );
    }

    @Test
    public void testGREATER_THEN_OR_EQUAL_TO() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", GREATER_THEN_OR_EQUAL_TO, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L ), v( "5", 5L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.trace( l( 0L ) ) ).isEqualTo( "query = [d1:0]\nALL OK" );
        assertThat( tree.trace( l( 5L ) ) ).isEqualTo( "query = [d1:5]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1] GREATER_THEN_OR_EQUAL_TO 5" );
        assertThat( tree.trace( l( 6L ) ) ).isEqualTo( "query = [d1:6]\n" +
            "Expecting:\n" +
            "1: \n" +
            "    d1/0: [1] GREATER_THEN_OR_EQUAL_TO 6\n" +
            "5: \n" +
            "    d1/0: [5] GREATER_THEN_OR_EQUAL_TO 6" );
    }

    public enum TestEnum {
        Test1, Test2, Test3, Test4
    }
}