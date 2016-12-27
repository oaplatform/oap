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

import lombok.val;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class Tree<T> {
    public static final long ANY = Long.MIN_VALUE;

    private List<Dimension<?>> dimensions;
    private TreeNode<T> root = new Leaf<>( emptyList() );

    Tree( List<Dimension<?>> dimensions ) {
        this.dimensions = dimensions;
    }

    public static <T> ValueData<T> s( T selection, Object... data ) {
        return new ValueData<>( data, selection );
    }

    public static <T> TreeBuilder<T> tree( List<Dimension<?>> dimensions ) {
        return new TreeBuilder<>( dimensions );
    }

    public static <T> TreeBuilder<T> tree( Dimension<?>... dimensions ) {
        return new TreeBuilder<>( asList( dimensions ) );
    }

    public void load( ValueData<T>[] data ) {
        final LongValueData<T>[] longData = convertObjectToLong( data );
        root = toNode( asList( longData ), new BitSet( dimensions.size() ) );
    }

    @SuppressWarnings( "unchecked" )
    private LongValueData<T>[] convertObjectToLong( ValueData<T>[] data ) {
        Stream.of( dimensions )
            .zipWithIndex()
            .forEach( p -> {
                final Stream<Object> rStream = Stream.of( data ).map( d -> d.data[p._2] );
                ( ( Dimension<Object> ) p._1 ).init( rStream );
            } );

        return Stream.of( data )
            .<LongValueData>map( vd -> new LongValueData<>( convertDataToLong( vd.data ), vd.value ) )
            .toArray( ( IntFunction<LongValueData<T>[]> ) LongValueData[]::new );
    }

    private long[] convertDataToLong( Object[] data ) {
        final long[] longData = new long[dimensions.size()];

        for( int i = 0; i < dimensions.size(); i++ ) {
            final Object value = data[i];
            if( value == null ) longData[i] = ANY;
            else longData[i] = ( ( Dimension<Object> ) dimensions.get( i ) ).getOrDefault( value );
        }

        return longData;
    }

    private TreeNode<T> toNode( List<LongValueData<T>> data, BitSet eq ) {
        if( data.isEmpty() ) return null;

        final SplitDimension splitDimension = findSplitDimension( data, eq );

        if( splitDimension == null ) return new Leaf<>( data.stream().map( sd -> sd.value ).collect( toList() ) );

        final BitSet bitSetWithDimension = withSet( eq, splitDimension.dimension );

        return new Node(
            splitDimension.dimension,
            splitDimension.value,
            toNode( splitDimension.left, eq ),
            toNode( splitDimension.right, eq ),
            toNode( splitDimension.equal, bitSetWithDimension ),
            toNode( splitDimension.any, bitSetWithDimension )
        );
    }

    private BitSet withSet( BitSet eq, int dimension ) {
        final BitSet bitSet = BitSet.valueOf( eq.toLongArray() );
        bitSet.set( dimension );
        return bitSet;
    }

    private SplitDimension findSplitDimension( List<LongValueData<T>> data, BitSet eqBitSet ) {
        long uniqueSize = 0;
        int splitDimension = -1;
        for( int i = 0; i < dimensions.size(); i++ ) {
            if( eqBitSet.get( i ) ) continue;

            final int finalI = i;
            long count = data
                .stream()
                .mapToLong( s -> s.data[finalI] )
                .filter( v -> v != ANY )
                .distinct()
                .count();

            if( count > uniqueSize ) {
                uniqueSize = count;
                splitDimension = i;
            }
        }

        if( splitDimension < 0 ) return null;

        final int finalSplitDimension = splitDimension;
        val partition_any_other = Stream.of( data ).partition( sd -> sd.data[finalSplitDimension] == ANY );

        final List<LongValueData<T>> sorted = partition_any_other._2
            .sorted( Comparator.comparingLong( sd -> sd.data[finalSplitDimension] ) )
            .collect( toList() );

        final long[] unique = sorted.stream().mapToLong( sd -> sd.data[finalSplitDimension] ).distinct().toArray();

        final long splitValue = unique[unique.length / 2];

        val partition_left_eq_right = Stream.of( sorted ).partition( sd -> sd.data[finalSplitDimension] < splitValue );
        val partition_eq_right = partition_left_eq_right._2.partition( sd -> sd.data[finalSplitDimension] == splitValue );

        final List<LongValueData<T>> left = partition_left_eq_right._1.collect( toList() );
        final List<LongValueData<T>> right = partition_eq_right._2.collect( toList() );
        final List<LongValueData<T>> eq = partition_eq_right._1.collect( toList() );
        final List<LongValueData<T>> any = partition_any_other._1.collect( toList() );
        return new SplitDimension( splitDimension, splitValue, left, right, eq, any );
    }

    public Set<T> find( Object... query ) {
        final HashSet<T> result = new HashSet<>();
        final long[] longQuery = convertDataToLong( query );
        find( root, longQuery, result );
        return result;
    }

    private void find( TreeNode<T> node, long[] query, HashSet<T> result ) {
        if( node == null ) return;

        if( node instanceof Leaf ) {
            result.addAll( ( ( Leaf<T> ) node ).selections );
        } else {
            final Node n = ( Node ) node;
            find( n.any, query, result );

            final long qValue = query[n.dimension];
            if( qValue == ANY ) {
                find( n.equal, query, result );
                find( n.right, query, result );
                find( n.left, query, result );
            } else if( qValue < n.eqValue ) {
                find( n.left, query, result );
            } else if( qValue == n.eqValue ) {
                find( n.equal, query, result );
            } else {
                find( n.right, query, result );
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        print( root, out );

        return out.toString();
    }

    public String trace( Object... query ) {

        final long[] longQuery = convertDataToLong( query );

        final BitSet fails = new BitSet();
        final HashMap<T, List<long[]>> notFound = new HashMap<>();

        trace( root, longQuery, notFound, new long[dimensions.size()], fails );

        if( !notFound.isEmpty() ) {
            return notFound
                .entrySet()
                .stream()
                .map(
                    s ->
                        s.getKey() + " -> " + arrayToString( longQuery ) + " not in: " + Stream.of( s.getValue() )
                            .map( this::arrayToString )
                            .collect( joining( ",", "[", "]" ) )
                )
                .collect( joining( "\n" ) )
                + "\n";
        } else {
            return "ALL OK";
        }
    }

    private String arrayToString( long[] set ) {
        final StringBuilder result = new StringBuilder( "(" );

        for( int i = 0; i < dimensions.size(); i++ ) {
            if( i > 0 ) result.append( "," );

            final long value = set[i];
            if( value == ANY ) result.append( "ANY" );
            else {
                final Dimension<?> dimension = dimensions.get( i );
                result.append( dimension.toString( value ) );
            }
        }

        result.append( ')' );

        return result.toString();
    }

    private void trace( TreeNode<T> node, long[] query, HashMap<T, List<long[]>> notFound, long[] eq, BitSet fail ) {
        if( node == null ) return;

        if( node instanceof Leaf ) {
            if( !fail.isEmpty() ) {
                final Leaf<T> n = ( Leaf<T> ) node;

                n.selections.forEach( s -> notFound
                    .computeIfAbsent( s, ( ss ) -> new ArrayList<>() )
                    .add( eq ) );
            }
        } else {
            final Node n = ( Node ) node;
            final long[] newEq = Arrays.copyOf( eq, eq.length );
            newEq[n.dimension] = n.eqValue;

            trace( n.any, query, notFound, newEq, fail );

            final long qValue = query[n.dimension];
            if( qValue == ANY ) {
                trace( n.equal, query, notFound, newEq, fail );
                trace( n.right, query, notFound, eq, fail );
                trace( n.left, query, notFound, eq, fail );
            } else if( qValue < n.eqValue ) {
                trace( n.left, query, notFound, eq, fail );
                BitSet newFail = logFail( fail, n );
                trace( n.equal, query, notFound, newEq, newFail );
                trace( n.right, query, notFound, eq, newFail );
            } else if( qValue == n.eqValue ) {
                trace( n.equal, query, notFound, newEq, fail );
                BitSet newFail = logFail( fail, n );
                trace( n.right, query, notFound, eq, newFail );
                trace( n.left, query, notFound, eq, newFail );
            } else {
                trace( n.right, query, notFound, eq, fail );
                BitSet newFail = logFail( fail, n );
                trace( n.left, query, notFound, eq, newFail );
                trace( n.equal, query, notFound, newEq, newFail );
            }
        }
    }

    private BitSet logFail( BitSet fail, Node n ) {
        BitSet newFail = fail;
        if( !fail.get( n.dimension ) ) {
            newFail = BitSet.valueOf( fail.toLongArray() );
            newFail.set( n.dimension );
        }
        return newFail;
    }

    private void print( TreeNode<T> node, StringBuilder out ) {
        print( "", true, node, out, "root" );
    }

    private void print( String prefix, boolean isTail, TreeNode<T> node, StringBuilder out, String type ) {
        out.append( prefix ).append( isTail ? "└── " : "├── " ).append( type ).append( ":" );
        if( node != null ) {
            node.print( out );
            out.append( "\n" );

            final List<Pair<String, TreeNode<T>>> children =
                node.children().stream().filter( p -> p._2 != null ).collect( toList() );

            for( int i = 0; i < children.size(); i++ ) {
                final Pair<String, TreeNode<T>> child = children.get( i );
                final String name = child._1;
                final TreeNode<T> value = child._2;

                if( value != null )
                    print( prefix + ( isTail ? "    " : "│   " ), i + 1 >= children.size(), value, out, name );

            }
        }
    }

    private interface TreeNode<T> {
        List<Pair<String, TreeNode<T>>> children();

        void print( StringBuilder out );
    }

    public static class ValueData<T> {
        public final Object[] data;
        public final T value;

        public ValueData( Object[] data, T value ) {
            this.data = data;
            this.value = value;
        }
    }

    private static class LongValueData<T> {
        private final long[] data;
        private final T value;

        private LongValueData( long[] data, T value ) {
            this.data = data;
            this.value = value;
        }
    }

    private static class Leaf<T> implements TreeNode<T> {
        private final List<T> selections;

        private Leaf( List<T> selections ) {
            this.selections = selections;
        }

        @Override
        public List<Pair<String, TreeNode<T>>> children() {
            return Collections.emptyList();
        }

        @Override
        public void print( StringBuilder out ) {
            final String collect = selections.stream()
                .map( Object::toString )
                .collect( java.util.stream.Collectors.joining( "," ) );
            out.append( "dn|[" )
                .append( collect )
                .append( "]" );
        }
    }

    private class SplitDimension {
        private final List<LongValueData<T>> left;
        private final List<LongValueData<T>> right;
        private final List<LongValueData<T>> equal;
        private final List<LongValueData<T>> any;
        private final int dimension;
        private final long value;

        private SplitDimension(
            int dimension,
            long value,
            List<LongValueData<T>> left,
            List<LongValueData<T>> right,
            List<LongValueData<T>> equal,
            List<LongValueData<T>> any ) {
            this.dimension = dimension;
            this.value = value;

            this.left = left;
            this.right = right;
            this.equal = equal;
            this.any = any;
        }
    }

    private class Node implements TreeNode<T> {
        final TreeNode<T> left;
        final TreeNode<T> right;
        final TreeNode<T> equal;
        final TreeNode<T> any;
        final int dimension;
        final long eqValue;

        private Node( int dimension, long eqValue, TreeNode<T> left, TreeNode<T> right, TreeNode<T> equal, TreeNode<T> any ) {
            this.dimension = dimension;
            this.eqValue = eqValue;
            this.left = left;
            this.right = right;
            this.equal = equal;
            this.any = any;
        }

        @Override
        public List<Pair<String, TreeNode<T>>> children() {
            return Lists.of( __( "l", left ), __( "r", right ), __( "eq", equal ), __( "a", any ) );
        }

        @Override
        public void print( StringBuilder out ) {
            final Dimension<?> dimension = dimensions.get( this.dimension );
            out.append( "kdn|" )
                .append( "d:" )
                .append( dimension.name ).append( '/' ).append( this.dimension )
                .append( ",sv:" ).append( dimension.toString( eqValue ) );
        }
    }
}
