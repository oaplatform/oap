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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static oap.tree.Dimension.ENUM;
import static oap.tree.Dimension.LONG;
import static oap.tree.Dimension.OperationType.BETWEEN_INCLUSIVE;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.GREATER_THEN;
import static oap.tree.Dimension.OperationType.GREATER_THEN_OR_EQUAL_TO;
import static oap.tree.Dimension.OperationType.LESS_THEN;
import static oap.tree.Dimension.OperationType.LESS_THEN_OR_EQUAL_TO;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.tree.Dimension.PRIORITY_DEFAULT;
import static oap.tree.Dimension.STRING;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static oap.tree.TreeTest.TestEnum.Test1;
import static oap.tree.TreeTest.TestEnum.Test2;
import static oap.tree.TreeTest.TestEnum.Test3;
import static oap.tree.TreeTest.TestEnum.Test4;
import static oap.tree.TreeTest.TestEnum.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeTest {
    @SafeVarargs
    private static <T> Set<T> s( T... data ) {
        return new HashSet<>( asList( data ) );
    }

    @Test
    public void testCONTAINS() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L ), v( "2", 2L ), v( "3", 3L ), v( "33", 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( l( 1L, 2L ) ) ) ).containsOnly( "1", "2" );
        assertThat( tree.find( l( l( 1L, 3L ) ) ) ).containsOnly( "1", "3", "33" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( 3L ) ) ).containsOnly( "3", "33" );

        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testEmpty() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l() );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 0 );
    }

    @Test
    public void testEmptyAsFailed() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, PRIORITY_DEFAULT, null, true ),
                LONG( "d2", CONTAINS, PRIORITY_DEFAULT, null, true ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L, null ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L, 2L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 0 );
    }

    @Test
    public void testGREATER_THEN_OR_EQUAL_TO() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", GREATER_THEN_OR_EQUAL_TO, null ) )
            .load( l( v( "1", 1L ), v( "5", 5L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 0L ) ) ).containsOnly( "1", "5" );
        assertThat( tree.find( l( 1L ) ) ).containsOnly( "1", "5" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "5" );
        assertThat( tree.find( l( 5L ) ) ).containsOnly( "5" );

        assertThat( tree.find( l( 6L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testGREATER_THEN() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", GREATER_THEN, null ) )
            .load( l( v( "1", 1L ), v( "5", 5L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 0L ) ) ).containsOnly( "1", "5" );
        assertThat( tree.find( l( 1L ) ) ).containsOnly( "5" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "5" );
        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.find( l( 6L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testLESS_THEN_OR_EQUAL_TO() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", LESS_THEN_OR_EQUAL_TO, null ) )
            .load( l( v( "1", 1L ), v( "5", 5L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 0L ) ) ).isEmpty();

        assertThat( tree.find( l( 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 5L ) ) ).containsOnly( "1", "5" );
        assertThat( tree.find( l( 6L ) ) ).containsOnly( "1", "5" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testLESS_THEN() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", LESS_THEN, null ) )
            .load( l( v( "1", 1L ), v( "5", 5L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 0L ) ) ).isEmpty();
        assertThat( tree.find( l( 1L ) ) ).isEmpty();

        assertThat( tree.find( l( 2L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 5L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 6L ) ) ).containsOnly( "1", "5" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testBETWEEN_INCLUSIVE() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", BETWEEN_INCLUSIVE, null ) )
            .load( l( v( "3", 3L ), v( "7", 7L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l( 0L, 9L ) ) ) ).containsOnly( "3", "7" );
        assertThat( tree.find( l( l( 1L, 7L ) ) ) ).containsOnly( "3", "7" );
        assertThat( tree.find( l( l( 3L, 6L ) ) ) ).containsOnly( "3" );
        assertThat( tree.find( l( l( 3L, 7L ) ) ) ).containsOnly( "3", "7" );
        assertThat( tree.find( l( l( 3L, 8L ) ) ) ).containsOnly( "3", "7" );

        assertThat( tree.find( l( l( 4L, 6L ) ) ) ).isEmpty();
        assertThat( tree.find( l( l( 0L, 2L ) ) ) ).isEmpty();
        assertThat( tree.find( l( l( 8L, 10L ) ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testBETWEEN_Empty() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", BETWEEN_INCLUSIVE, null ) )
            .load( l( v( "3", 3L ), v( "7", 7L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( Optional.empty() ) ) ).isEmpty();
    }

    @Test
    public void testExclude() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", NOT_CONTAINS, null ) )
            .load( l( v( "1", 1L ), v( "2", 2L ), v( "3", 3L ), v( "33", 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnly( "2", "3", "33" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "1", "3", "33" );
        assertThat( tree.find( l( 3L ) ) ).containsOnly( "1", "2" );

        assertThat( tree.find( l( 5L ) ) ).containsOnly( "1", "2", "3", "33" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testEnum() {
        final Tree<String> tree = Tree
            .<String>tree( ENUM( "d1", TestEnum.class, CONTAINS, 0, UNKNOWN, false ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", Test1 ), v( "2", Test2 ), v( "3", Test3 ), v( "33", Test3 ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( Test1 ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( Test2 ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( Test3 ) ) ).containsOnly( "3", "33" );

        assertThat( tree.find( l( Test4 ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testString() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", CONTAINS ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", "s1" ), v( "2", "s2" ), v( "3", "s3" ), v( "33", "s3" ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "s1" ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( "s2" ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( "s3" ) ) ).containsOnly( "3", "33" );

        assertThat( tree.find( l( "s4" ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testFindTwoDimension() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ), LONG( "d2", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L, 1L ), v( "2", 2L, 2L ), v( "3", 1L, 3L ), v( "33", 1L, 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L, 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 2L, 2L ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( 1L, 3L ) ) ).containsOnly( "3", "33" );

        assertThat( tree.find( l( 1L, 2L ) ) ).isEmpty();
        assertThat( tree.find( l( 3L, 3L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 4 );
    }

    @Test
    public void testFindTwoDimensionHash() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ), LONG( "d2", CONTAINS, null ) )
            .withHashFillFactor( 0.75 )
            .load( l( v( "1", 1L, 1L ), v( "2", 2L, 2L ), v( "3", 1L, 3L ), v( "33", 1L, 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L, 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 2L, 2L ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( 1L, 3L ) ) ).containsOnly( "3", "33" );

        assertThat( tree.find( l( 1L, 2L ) ) ).isEmpty();
        assertThat( tree.find( l( 3L, 3L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testFindAny() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, -1, null, false ), LONG( "d2", CONTAINS, null ), LONG( "d3", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l(
                v( "1", 1L, null, 1L ),
                v( "2", 2L, 2L, 1L ),
                v( "3", 1L, 3L, 1L ),
                v( "33", 1L, 3L, 1L )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L, 1L, 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 2L, 2L, 1L ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( 1L, 3L, 1L ) ) ).containsOnly( "1", "3", "33" );

        assertThat( tree.find( l( 1L, 2L, 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 1L, null, 1L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( null, 3L, null ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 5 );
    }

    @Test
    public void testFindOrDimension() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l(
                v( "1", 1L ),
                v( "2", 2L ),
                v( "3", 3L ),
                v( "33", 3L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l( 1L, 2L ) ) ) ).containsOnly( "1", "2" );
        assertThat( tree.find( l( l( 2L, 5L ) ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( l() ) ) ).isEmpty();
    }

    @Test
    public void testFindOptionalString() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", CONTAINS ) )
            .withHashFillFactor( 1 )
            .load( l(
                v( "1", "s1" ),
                v( "2", ( String ) null ),
                v( "3", "s3" ),
                v( "33", "s3" )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( Optional.of( "s1" ) ) ) ).containsOnly( "1", "2" );
        assertThat( tree.find( l( ( String ) null ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( Optional.<String>empty() ) ) ).containsOnly( "2" );
    }

    @Test
    public void testOptionalData() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", CONTAINS ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", Optional.of( "s1" ) ), v( "2", Optional.empty() ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "s1" ) ) ).containsOnly( "1", "2" );
        assertThat( tree.find( l( "s2" ) ) ).containsOnly( "2" );
    }

    @Test
    public void testNullData() {
        final Tree<String> tree = Tree
            .<String>tree( STRING( "d1", CONTAINS ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", "s1" ), v( "2", ( Object ) null ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "s1" ) ) ).containsOnly( "1", "2" );
        assertThat( tree.find( l( "s2" ) ) ).containsOnly( "2" );
    }


    @Test
    public void testSet() {
        final Tree<String> tree = Tree
            .<String>tree( LONG( "d1", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l( v( "1", 1L ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( s( 1L ) ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( s( 2L, 1L ) ) ) ).containsOnly( "1" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 2 );
    }

    public enum TestEnum {
        Test1, Test2, Test3, Test4, UNKNOWN
    }
}
