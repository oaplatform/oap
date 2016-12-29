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

import lombok.ToString;
import lombok.val;
import oap.tree.Dimension.OperationType;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class Tree<T> {
    public static final long ANY = Long.MIN_VALUE;
    TreeNode<T> root = new Leaf<>( emptyList() );
    private List<Dimension> dimensions;

    Tree( List<Dimension> dimensions ) {
        this.dimensions = dimensions;
    }

    public static <T> ValueData<T> v( T selection, List<Object> data ) {
        return new ValueData<>( data, selection );
    }

    public static <T> List<T> l( T... data ) {
        return asList( data );
    }

    public static <T> ValueData<T> v( T selection, Object... data ) {
        return v( selection, asList( data ) );
    }

    public static <T> TreeBuilder<T> tree( List<Dimension> dimensions ) {
        return new TreeBuilder<>( dimensions );
    }

    public static <T> TreeBuilder<T> tree( Dimension... dimensions ) {
        return new TreeBuilder<>( asList( dimensions ) );
    }

    public void load( List<ValueData<T>> data ) {
        init( data );
        root = toNode( data, new BitSet( dimensions.size() ) );
    }

    @SuppressWarnings( "unchecked" )
    private void init( List<ValueData<T>> data ) {
        Stream.of( dimensions )
            .zipWithIndex()
            .forEach( p -> p._1.init( data.stream().map( d -> d.data.get( p._2 ) ) ) );
    }

    private long[] convertDataToLong( List<Object> data ) {
        final long[] longData = new long[dimensions.size()];

        for( int i = 0; i < dimensions.size(); i++ ) {
            final Object value = data.get( i );
            if( value == null ) longData[i] = ANY;
            else longData[i] = dimensions.get( i ).getOrDefault( value );
        }

        return longData;
    }

    private TreeNode<T> toNode( List<ValueData<T>> data, BitSet eq ) {
        if( data.isEmpty() ) return null;

        final SplitDimension splitDimension = findSplitDimension( data, eq );

        if( splitDimension == null ) return new Leaf<>( data.stream().map( sd -> sd.value ).collect( toList() ) );

        final BitSet bitSetWithDimension = withSet( eq, splitDimension.dimension );

        final Dimension dimension = dimensions.get( splitDimension.dimension );
        final List<ArrayBitSet> sets = splitDimension.sets
            .stream()
            .collect( groupingBy( s -> s.data.get( splitDimension.dimension ) ) )
            .entrySet()
            .stream()
            .map( es -> new ArrayBitSet( dimension.toBitSet( ( List ) es.getKey() ), dimension.operationType == CONTAINS, toNode( es.getValue(), bitSetWithDimension ) ) )
            .collect( toList() );
        return new Node(
            splitDimension.dimension,
            splitDimension.value,
            toNode( splitDimension.left, eq ),
            toNode( splitDimension.right, eq ),
            toNode( splitDimension.equal, bitSetWithDimension ),
            toNode( splitDimension.any, bitSetWithDimension ),
            sets
        );
    }

    private BitSet withSet( BitSet eq, int dimension ) {
        final BitSet bitSet = BitSet.valueOf( eq.toLongArray() );
        bitSet.set( dimension );
        return bitSet;
    }

    private SplitDimension findSplitDimension( List<ValueData<T>> data, BitSet eqBitSet ) {
        long uniqueSize = 0;
        int splitDimension = -1;
        for( int i = 0; i < dimensions.size(); i++ ) {
            if( eqBitSet.get( i ) ) continue;

            final int finalI = i;
            final Dimension dimension = dimensions.get( finalI );
            long count = data
                .stream()
                .filter( s -> !( s.data.get( finalI ) instanceof List ) )
                .mapToLong( s -> dimension.getOrDefault( s.data.get( finalI ) ) )
                .filter( v -> v != ANY )
                .distinct()
                .count();

            if( count > uniqueSize || dimension.array ) {
                uniqueSize = count;
                splitDimension = i;
            }
        }

        if( splitDimension < 0 ) return null;

        if( uniqueSize == 0 ) { //array
            return new SplitDimension( splitDimension, ANY, emptyList(), emptyList(), emptyList(), emptyList(), data );
        } else {

            final int finalSplitDimension = splitDimension;
            final Dimension dimension = dimensions.get( finalSplitDimension );

            val partition_sets_other = Stream.of( data ).partition( sd -> sd.data.get( finalSplitDimension ) instanceof List );

            val partition_any_other = partition_sets_other._2.partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) == ANY );

            final List<ValueData<T>> sorted = partition_any_other._2
                .sorted( Comparator.comparingLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) ) )
                .collect( toList() );

            final long[] unique = sorted.stream().mapToLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) ).distinct().toArray();

            final long splitValue = unique[unique.length / 2];

            val partition_left_eq_right = Stream.of( sorted ).partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) < splitValue );
            val partition_eq_right = partition_left_eq_right._2.partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) == splitValue );

            final List<ValueData<T>> left = partition_left_eq_right._1.collect( toList() );
            final List<ValueData<T>> right = partition_eq_right._2.collect( toList() );
            final List<ValueData<T>> eq = partition_eq_right._1.collect( toList() );
            final List<ValueData<T>> any = partition_any_other._1.collect( toList() );
            final List<ValueData<T>> sets = partition_sets_other._1.collect( toList() );

            return new SplitDimension( splitDimension, splitValue, left, right, eq, any, sets );
        }
    }

    public Set<T> find( Object... query ) {
        return find( asList( query ) );
    }

    public Set<T> find( List<Object> query ) {
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

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY ) {
                find( n.equal, query, result );
                find( n.right, query, result );
                find( n.left, query, result );
            } else if( !n.sets.isEmpty() ) {
                for( ArrayBitSet set : n.sets ) {
                    if( set.bitSet.get( ( int ) qValue ) == set.include )
                        find( set.equal, query, result );
                }
            } else if( dimension.operationType == NOT_CONTAINS ) {
                find( n.left, query, result );
                find( n.right, query, result );
                if( qValue != n.eqValue )
                    find( n.equal, query, result );
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
        return trace( asList( query ) );
    }

    public String trace( List<Object> query ) {
        final long[] longQuery = convertDataToLong( query );

        final BitSet fails = new BitSet();
        final HashMap<T, List<Pair<OperationType, long[][]>>> notFound = new HashMap<>();

        trace( root, longQuery, notFound, new long[dimensions.size()][], fails, null );

        if( !notFound.isEmpty() ) {
            return notFound
                .entrySet()
                .stream()
                .map(
                    s -> s.getKey() + " -> " + arrayToString( longQuery ) + " not in: " + Stream.of( s.getValue() )
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
                final Dimension dimension = dimensions.get( i );
                result.append( dimension.toString( value ) );
            }
        }

        result.append( ')' );

        return result.toString();
    }

    private String arrayToString( Pair<OperationType, long[][]> p ) {
        final StringBuilder result = new StringBuilder( "(" );

        for( int i = 0; i < dimensions.size(); i++ ) {
            final Dimension dimension = dimensions.get( i );

            if( i > 0 ) result.append( "," );

            final long[] value = p._2[i];

            if( p._1 == NOT_CONTAINS ) result.append( "!" );

            if( dimension.array ) {
                result.append( LongStream.of( value ).mapToObj( dimension::toString ).collect( joining( ",", "{", "}" ) ) );
            } else {
                if( value[0] == ANY ) result.append( "ANY" );
                else {
                    result.append( dimension.toString( value[0] ) );
                }
            }
        }

        result.append( ')' );

        return result.toString();
    }

    private void trace( TreeNode<T> node, long[] query,
                        HashMap<T, List<Pair<OperationType, long[][]>>> notFound,
                        long[][] eq, BitSet fail, OperationType failBy ) {
        if( node == null ) return;

        if( node instanceof Leaf ) {
            if( !fail.isEmpty() ) {
                final Leaf<T> n = ( Leaf<T> ) node;

                n.selections.forEach( s -> notFound
                    .computeIfAbsent( s, ( ss ) -> new ArrayList<>() )
                    .add( __( failBy, eq ) ) );
            }
        } else {
            final Node n = ( Node ) node;
            final long[][] newEq = Arrays.copyOf( eq, eq.length );
            newEq[n.dimension] = new long[] { n.eqValue };

            trace( n.any, query, notFound, newEq, fail, failBy );

            final long qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY ) {
                trace( n.equal, query, notFound, newEq, fail, failBy );
                trace( n.right, query, notFound, eq, fail, failBy );
                trace( n.left, query, notFound, eq, fail, failBy );
            } else if( !n.sets.isEmpty() ) {
                for( ArrayBitSet set : n.sets ) {
                    if( set.bitSet.get( ( int ) qValue ) == set.include )
                        trace( set.equal, query, notFound, newEq, fail, failBy );
                    else {
                        BitSet newFail = logFail( fail, n.dimension );
                        final long[][] newEqArray = Arrays.copyOf( eq, eq.length );
                        newEqArray[n.dimension] = set.bitSet.stream().mapToLong( i -> i ).toArray();


                        trace( set.equal, query, notFound, newEqArray, newFail, !set.include ? NOT_CONTAINS : failBy );
                    }
                }
            } else if( dimension.operationType == NOT_CONTAINS ) {
                trace( n.left, query, notFound, eq, fail, failBy );
                trace( n.right, query, notFound, eq, fail, failBy );
                if( qValue != n.eqValue )
                    trace( n.equal, query, notFound, newEq, fail, failBy );
                else {
                    BitSet newFail = logFail( fail, n.dimension );
                    trace( n.equal, query, notFound, newEq, newFail, NOT_CONTAINS );
                }
            } else if( qValue < n.eqValue ) {
                trace( n.left, query, notFound, eq, fail, failBy );
                BitSet newFail = logFail( fail, n.dimension );
                trace( n.equal, query, notFound, newEq, newFail, failBy );
                trace( n.right, query, notFound, eq, newFail, failBy );
            } else if( qValue == n.eqValue ) {
                trace( n.equal, query, notFound, newEq, fail, failBy );
                BitSet newFail = logFail( fail, n.dimension );
                trace( n.right, query, notFound, eq, newFail, failBy );
                trace( n.left, query, notFound, eq, newFail, failBy );
            } else {
                trace( n.right, query, notFound, eq, fail, failBy );
                BitSet newFail = logFail( fail, n.dimension );
                trace( n.left, query, notFound, eq, newFail, failBy );
                trace( n.equal, query, notFound, newEq, newFail, failBy );
            }
        }
    }

    private BitSet logFail( BitSet fail, int dimension ) {
        BitSet newFail = fail;
        if( !fail.get( dimension ) ) {
            newFail = BitSet.valueOf( fail.toLongArray() );
            newFail.set( dimension );
        }
        return newFail;
    }

    private void print( TreeNode<T> node, StringBuilder out ) {
        print( "", true, node, out, "root" );
    }

    public int getMaxDepth() {
        final AtomicInteger depth = new AtomicInteger( 0 );
        findMaxDepth( root, depth, 1 );

        return depth.get();
    }

    private void findMaxDepth( TreeNode<T> node, AtomicInteger maxDepth, int currentDepth ) {
        if( node == null ) {
            if( currentDepth - 1 > maxDepth.get() ) maxDepth.set( currentDepth - 1 );
            return;
        }

        if( node instanceof Leaf ) {
            if( currentDepth > maxDepth.get() ) maxDepth.set( currentDepth );
            return;
        }

        final Node n = ( Node ) node;
        findMaxDepth( n.left, maxDepth, currentDepth + 1 );
        findMaxDepth( n.right, maxDepth, currentDepth + 1 );
        findMaxDepth( n.any, maxDepth, currentDepth + 1 );
        findMaxDepth( n.equal, maxDepth, currentDepth + 1 );

        for( ArrayBitSet abs : n.sets ) {
            findMaxDepth( abs.equal, maxDepth, currentDepth + 1 );
        }
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
        public final List<Object> data;
        public final T value;

        public ValueData( List<Object> data, T value ) {
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

    @ToString
    private class ArrayBitSet {
        private final BitSet bitSet;
        private final boolean include;
        private final TreeNode<T> equal;

        public ArrayBitSet( BitSet bitSet, boolean include, TreeNode<T> equal ) {
            this.bitSet = bitSet;
            this.include = include;
            this.equal = equal;
        }
    }

    private class SplitDimension {
        private final List<ValueData<T>> left;
        private final List<ValueData<T>> right;
        private final List<ValueData<T>> equal;
        private final List<ValueData<T>> any;
        private final List<ValueData<T>> sets;
        private final int dimension;
        private final long value;

        private SplitDimension(
            int dimension,
            long value,
            List<ValueData<T>> left,
            List<ValueData<T>> right,
            List<ValueData<T>> equal,
            List<ValueData<T>> any,
            List<ValueData<T>> sets ) {
            this.dimension = dimension;
            this.value = value;

            this.left = left;
            this.right = right;
            this.equal = equal;
            this.any = any;
            this.sets = sets;
        }
    }

    class Node implements TreeNode<T> {
        final List<ArrayBitSet> sets;
        final TreeNode<T> left;
        final TreeNode<T> right;
        final TreeNode<T> equal;
        final TreeNode<T> any;
        final int dimension;
        final long eqValue;

        private Node( int dimension, long eqValue, TreeNode<T> left, TreeNode<T> right,
                      TreeNode<T> equal, TreeNode<T> any, List<ArrayBitSet> sets ) {
            this.dimension = dimension;
            this.eqValue = eqValue;
            this.left = left;
            this.right = right;
            this.equal = equal;
            this.any = any;
            this.sets = sets;
        }

        @Override
        public List<Pair<String, TreeNode<T>>> children() {
            final ArrayList<Pair<String, TreeNode<T>>> result = new ArrayList<>();
            result.add( __( "l", left ) );
            result.add( __( "r", right ) );
            result.add( __( "eq", equal ) );
            result.add( __( "a", any ) );

            for( int i = 0; i < sets.size(); i++ ) {
                final ArrayBitSet set = sets.get( i );
                result.add( __( ( set.include ? "in:" : "not in:" ) + bitSetToData( set.bitSet ), set.equal ) );
            }

            return result;
        }

        private String bitSetToData( BitSet bitSet ) {
            final Dimension dimension = dimensions.get( this.dimension );

            return bitSet.stream().mapToObj( dimension::toString ).collect( joining( ",", "[", "]" ) );
        }

        @Override
        public void print( StringBuilder out ) {
            final Dimension dimension = dimensions.get( this.dimension );
            out.append( "kdn|" )
                .append( "d:" )
                .append( dimension.name ).append( '/' ).append( this.dimension )
                .append( ",sv:" ).append( dimension.toString( eqValue ) );
        }
    }
}
