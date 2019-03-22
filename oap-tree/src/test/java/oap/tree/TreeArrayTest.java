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

import oap.util.Sets;
import org.testng.annotations.Test;

import java.util.Set;

import static oap.tree.Dimension.ARRAY_LONG;
import static oap.tree.Dimension.ARRAY_STRING;
import static oap.tree.Dimension.LONG;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Tree.ArrayOperation.AND;
import static oap.tree.Tree.ArrayOperation.NOT;
import static oap.tree.Tree.ArrayOperation.OR;
import static oap.tree.Tree.a;
import static oap.tree.Tree.l;
import static oap.tree.Tree.v;
import static org.assertj.core.api.Assertions.assertThat;

public class TreeArrayTest {

    @SafeVarargs
    private static <T> Set<T> s( T... data ) {
        return Sets.of( data );
    }

    private static <T> Tree.Array as( Tree.ArrayOperation operation, Set<T> values ) {
        return new Tree.Array( values, operation );
    }

    @Test
    public void testArray() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( OR, 1L, 2L ) ) ),
                v( "2", l( a( OR, 2L ) ) ),
                v( "3", l( a( OR, 1L, 2L, 3L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnly( "1", "3" );
        assertThat( tree.find( l( l( 5L, 1L ) ) ) ).containsOnly( "1", "3" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "1", "2", "3" );
        assertThat( tree.find( l( 3L ) ) ).containsOnly( "3" );

        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 2 );
        assertThat( ( ( Tree.Node ) tree.root ).sets ).hasSize( 3 );
    }

    @Test
    public void testArrayAND() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( AND, 1L, 2L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l( 1L ) ) ) ).isEmpty();
        assertThat( tree.find( l( l( 2L, 3L ) ) ) ).isEmpty();
        assertThat( tree.find( l( l( 1L, 2L ) ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( l( 1L, 2L, 3L ) ) ) ).containsOnly( "1" );
    }

    @Test( enabled = false )
    public void testArrayExpand() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .withArrayToTree( 4 )
            .withHashFillFactor( 1 )
            .load( l(
                v( "1", l( a( OR, 1L, 2L ) ) ),
                v( "2", l( a( OR, 2L ) ) ),
                v( "3", l( a( OR, 1L, 2L, 3L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L ) ) ).containsOnly( "1", "3" );
        assertThat( tree.find( l( l( 5L, 1L ) ) ) ).containsOnly( "1", "3" );
        assertThat( tree.find( l( 2L ) ) ).containsOnly( "1", "2", "3" );
        assertThat( tree.find( l( 3L ) ) ).containsOnly( "3" );

        assertThat( tree.find( l( 5L ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
        assertThat( ( ( Tree.Node ) tree.root ).sets ).isEmpty();
    }

    @Test
    public void testArrayString() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_STRING( "d1" ) )
            .load( l( v( "1", l( a( OR, "s1", "s2" ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "s1" ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( "s5" ) ) ).isEmpty();
    }

    @Test
    public void testArrayExclude() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ), LONG( "sv1", CONTAINS, null ) )
            .withHashFillFactor( 1 )
            .load( l(
                v( "1", l( a( NOT, 1L, 2L ), 1 ) ),
                v( "2", l( a( NOT, 2L ), 1 ) ),
                v( "3", l( a( NOT, 1L, 2L, 3L ), 1 ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 2L, 1L ) ) ).isEmpty();
        assertThat( tree.find( l( 1L, 1L ) ) ).containsOnly( "2" );
        assertThat( tree.find( l( 1L, 2L ) ) ).isEmpty();

        assertThat( tree.find( l( 5L, 1L ) ) ).containsOnly( "1", "2", "3" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testArrayQueryForArrayExclude() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l(
                v( "1", l( a( NOT, 1L, 2L ) ) ),
                v( "2", l( a( NOT, 2L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l( 3L, 2L ) ) ) ).isEmpty();
        assertThat( tree.find( l( l( 1L, 3L ) ) ) ).containsOnly( "2" );

        assertThat( tree.find( l( l( 5L, 6L ) ) ) ).containsOnly( "1", "2" );
    }

    @Test
    public void testArrayOptimize() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ), ARRAY_STRING( "d2" ) )
            .load( l(
                v( "1", l( a( OR, 1L, 2L ), a( OR, "1", "2" ) ) ),
                v( "2", l( a( OR, 1L, 2L ), a( OR, "1", "2" ) ) ),
                v( "3", l( a( OR, 1L, 2L, 3L ), a( OR, "1", "2", "3" ) ) ),
                v( "e1", l( a( OR ), a( OR ) ) ),
                v( "e2", l( a( NOT ), a( NOT ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( 1L, "1" ) ) ).containsOnly( "1", "2", "3", "e1", "e2" );
        assertThat( tree.find( l( 2L, "2" ) ) ).containsOnly( "1", "2", "3", "e1", "e2" );
        assertThat( tree.find( l( 2L, "3" ) ) ).containsOnly( "3", "e1", "e2" );

        assertThat( ( ( Tree.Node ) tree.root ).sets ).hasSize( 2 );
        assertThat( tree.getMaxDepth() ).isEqualTo( 3 );
    }

    @Test
    public void testArrayAnyAny() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l( v( "1", l( a( NOT ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l() ) ) ).containsOnly( "1" );
    }

    @Test
    public void testFindNoData() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_STRING( "d1" ), ARRAY_STRING( "d2" ) )
            .load( l(
                v( "1", l( a( OR ), a( NOT ) ) ),
                v( "2", l( a( NOT ), a( OR ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "1", null ) ) ).containsOnly( "1", "2" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 1 );
    }

    @Test
    public void testFindQueryAnyAndDimensionRequired() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l( v( "1", l( a( OR ) ) ), v( "2", l( a( OR, 2L ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( l() ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( ( Long ) null ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 1L, 3L ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( 3L, 1L ) ) ).containsOnly( "1" );
    }

    @Test
    public void testSet() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_LONG( "d1", null ) )
            .load( l( v( "1", l( as( OR, s( 1L, 2L ) ) ) ) ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( s( 1L ) ) ) ).containsOnly( "1" );
        assertThat( tree.find( l( s( 2L, 1L ) ) ) ).containsOnly( "1" );

        assertThat( tree.getMaxDepth() ).isEqualTo( 2 );
    }

    @Test
    public void testEmptyFailed() {
        final Tree<String> tree = Tree
            .<String>tree( ARRAY_STRING( "d1", true ), ARRAY_STRING( "d2", true ) )
            .load( l(
                v( "1", l( a( OR, 1L ), a( NOT ) ) ),
                v( "2", l( a( NOT ), a( OR, 2L ) ) )
            ) );

        System.out.println( tree.toString() );

        assertThat( tree.find( l( "1", null ) ) ).isEmpty();

        assertThat( tree.getMaxDepth() ).isEqualTo( 0 );
    }
}
