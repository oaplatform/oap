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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import oap.tree.Dimension.OperationType;
import oap.util.MemoryMeter;
import oap.util.Pair;
import oap.util.Stream;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static oap.tree.Dimension.Direction;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class Tree<T> {
    public static final long ANY = Long.MIN_VALUE;
    public static final long[] ANY_AS_ARRAY = new long[0];

    TreeNode<T> root = new Leaf<>( emptyList() );
    private List<Dimension> dimensions;
    private long memory;
    private long nodeCount = 0;
    private long leafCount = 0;

    Tree( List<Dimension> dimensions ) {
        this.dimensions = dimensions;
    }

    public static <T> ValueData<T> v( T selection, List<?> data ) {
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

    public static <T> Array a( boolean include, T... values ) {
        return new Array( l( values ), include );
    }

    public long getMemory() {
        return memory;
    }

    public long getNodeCount() {
        return nodeCount;
    }

    public long getLeafCount() {
        return leafCount;
    }

    public void load( List<ValueData<T>> data ) {
        init( data );
        root = toNode( data, new BitSet( dimensions.size() ) );

        updateCount( root );

        memory = MemoryMeter.get().measureDeep( this );
    }

    private void updateCount( TreeNode<T> node ) {
        if( node == null ) return;

        if( node instanceof Tree.Node ) {
            nodeCount++;
            final Node n = ( Node ) node;
            updateCount( n.any );
            updateCount( n.left );
            updateCount( n.right );
            updateCount( n.equal );
            n.sets.forEach( s -> updateCount( s.equal ) );
        } else {
            leafCount++;
        }
    }

    @SuppressWarnings( "unchecked" )
    private void init( List<ValueData<T>> data ) {
        Stream.of( dimensions )
            .zipWithIndex()
            .forEach( p -> p._1.init( data.stream().flatMap( d -> toStream( d.data.get( p._2 ) ) ) ) );
    }

    private Stream<?> toStream( Object item ) {
        return item instanceof Array ? Stream.of( ( ( Array ) item ) ) : Stream.of( item );
    }

    private long[][] convertQueryToLong( List<?> query ) {
        final long[][] longData = new long[dimensions.size()][];

        for( int i = 0; i < dimensions.size(); i++ ) {
            final Object value = query.get( i );
            final Dimension dimension = dimensions.get( i );
            longData[i] = dimension.getOrDefault( value );
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
            .map( es -> {
                final Array key = ( Array ) es.getKey();
                return new ArrayBitSet( dimension.toBitSet( key ), key.include, toNode( es.getValue(), bitSetWithDimension ) );
            } )
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
        int priority = Dimension.PRIORITY_LOW;
        int priorityArray = Dimension.PRIORITY_LOW;
        long uniqueSize = -1;
        long uniqueArraySize = Long.MAX_VALUE;
        int splitDimension = -1;
        int splitArrayDimension = -1;

        for( int i = 0; i < dimensions.size(); i++ ) {
            if( eqBitSet.get( i ) ) continue;

            final Dimension dimension = dimensions.get( i );

            final boolean isArray = dimension.operationType == null;

            if( isArray && splitDimension >= 0 ) continue;

            final HashSet<Long> unique = new HashSet<>();
            final HashSet<Array> uniqueArray = new HashSet<>();

            for( val vd : data ) {
                final Object value = vd.data.get( i );
                if( value instanceof Array ) {
                    final Array array = ( Array ) value;
                    if( !array.isEmpty() ) uniqueArray.add( array );
                } else {
                    final long[] longValue = dimension.getOrDefault( value );
                    if( longValue != ANY_AS_ARRAY ) unique.add( longValue[0] );
                }

            }

            if( !isArray && unique.size() > 0 && ( unique.size() > uniqueSize || dimension.priority >= priority ) ) {
                uniqueSize = unique.size();
                splitDimension = i;
                priority = dimension.priority;
            } else if( isArray && uniqueArray.size() > 0 && ( uniqueArray.size() < uniqueArraySize || dimension.priority >= priorityArray ) ) {
                uniqueArraySize = uniqueArray.size();
                splitArrayDimension = i;
                priorityArray = dimension.priority;
            }
        }

        if( splitDimension < 0 && splitArrayDimension < 0 ) return null;

        final int finalSplitDimension = splitDimension >= 0 ? splitDimension : splitArrayDimension;

        final Dimension dimension = dimensions.get( finalSplitDimension );

        if( dimension.operationType == null ) { //array
            val partition_sets_empty = Stream.of( data ).partition( vd -> !( ( Array ) vd.data.get( finalSplitDimension ) ).isEmpty() );

            final List<ValueData<T>> any = Stream.of( partition_sets_empty._2 ).collect( toList() );
            final List<ValueData<T>> sets = partition_sets_empty._1.collect( toList() );

            return new SplitDimension( finalSplitDimension, ANY, emptyList(), emptyList(), emptyList(), any, sets );
        } else {

            val partition_any_other = Stream.of( data ).partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) ) == ANY_AS_ARRAY );

            final List<ValueData<T>> sorted = partition_any_other._2
                .sorted( Comparator.comparingLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) )[0] ) )
                .collect( toList() );

            final long[] unique = sorted.stream().mapToLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) )[0] ).distinct().toArray();

            final long splitValue = unique[unique.length / 2];

            val partition_left_eq_right = Stream.of( sorted ).partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) )[0] < splitValue );
            val partition_eq_right = partition_left_eq_right._2.partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ) )[0] == splitValue );

            final List<ValueData<T>> left = partition_left_eq_right._1.collect( toList() );
            final List<ValueData<T>> right = partition_eq_right._2.collect( toList() );
            final List<ValueData<T>> eq = partition_eq_right._1.collect( toList() );
            final List<ValueData<T>> any = Stream.of( partition_any_other._1 ).collect( toList() );

            return new SplitDimension( finalSplitDimension, splitValue, left, right, eq, any, emptyList() );
        }
    }

    public Set<T> find( List<?> query ) {
        final HashSet<T> result = new HashSet<>();
        final long[][] longQuery = convertQueryToLong( query );
        find( root, longQuery, result );
        return result;
    }

    private void find( TreeNode<T> node, long[][] query, HashSet<T> result ) {
        if( node == null ) return;

        if( node instanceof Leaf ) {
            result.addAll( ( ( Leaf<T> ) node ).selections );
        } else {
            final Node n = ( Node ) node;
            find( n.any, query, result );

            final long[] qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY_AS_ARRAY ) {
                if( dimension.queryRequired ) return;

                find( n.equal, query, result );
                find( n.right, query, result );
                find( n.left, query, result );

                for( ArrayBitSet set : n.sets ) {
                    find( set.equal, query, result );
                }
            } else if( !n.sets.isEmpty() ) {
                for( ArrayBitSet set : n.sets ) {
                    if( set.find( qValue ) ) {
                        find( set.equal, query, result );
                    }
                }
            } else {
                final int direction = dimension.direction( qValue, n.eqValue );
                if( ( direction & Direction.LEFT ) > 0 )
                    find( n.left, query, result );
                if( ( direction & Direction.EQUAL ) > 0 )
                    find( n.equal, query, result );
                if( ( direction & Direction.RIGHT ) > 0 )
                    find( n.right, query, result );
            }
        }
    }

    public String trace( List<?> query ) {
        final HashMap<T, HashMap<Integer, TraceOperationTypeValues>> result = new HashMap<>();
        final long[][] longQuery = convertQueryToLong( query );
        trace( root, longQuery, result, new TraceBuffer(), true );


        final String queryStr = "query = " + Stream.of( query )
            .zipWithIndex()
            .map( p -> dimensions.get( p._2 ).name + ":" + p._1 )
            .collect( joining( ",", "[", "]" ) ) + "\n";

        final String out = result.entrySet().stream().map( e -> e.getKey().toString() + ": \n" +
            e.getValue().entrySet().stream().map( dv -> {
                    final Dimension dimension = dimensions.get( dv.getKey() );
                    return "    " + dimension.name + "/" + dv.getKey() + ": "
                        + dv.getValue().toString( dimension ) + " " + queryToString( longQuery, dv.getKey() );
                }
            ).collect( joining( "\n" ) )
        ).collect( joining( "\n" ) );
        return queryStr + ( out.length() > 0 ? "Expecting:\n" + out : "ALL OK" );
    }

    private String queryToString( long[][] query, int key ) {
        final long[] value = query[key];
        return LongStream.of( value ).mapToObj( dimensions.get( key )::toString ).collect( joining( ",", "[", "]" ) );
    }

    private void trace( TreeNode<T> node, long[][] query,
                        HashMap<T, HashMap<Integer, TraceOperationTypeValues>> result,
                        TraceBuffer buffer, boolean success ) {
        if( node == null ) return;

        if( node instanceof Leaf ) {
            final List<T> selections = ( ( Leaf<T> ) node ).selections;
            if( !success ) {
                selections.forEach( s -> {
                    final HashMap<Integer, TraceOperationTypeValues> dv = result.computeIfAbsent( s, ( ss ) -> new HashMap<>() );
                    buffer.forEach( ( d, otv ) ->
                        otv.forEach( ( ot, v ) ->
                            dv
                                .computeIfAbsent( d, ( dd ) -> new TraceOperationTypeValues() )
                                .addAll( ot, v )
                        )
                    );
                } );
            } else {
                selections.forEach( result::remove );
            }
        } else {
            final Node n = ( Node ) node;
            trace( n.any, query, result, buffer.clone(), success );

            final long[] qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY_AS_ARRAY ) {
                trace( n.equal, query, result, buffer.cloneWith( n.dimension, n.eqValue, dimension.operationType, !dimension.queryRequired ), success && !dimension.queryRequired );
                trace( n.right, query, result, buffer.clone(), success && !dimension.queryRequired );
                trace( n.left, query, result, buffer.clone(), success && !dimension.queryRequired );

                for( ArrayBitSet set : n.sets ) {
                    trace( set.equal, query, result, buffer.clone(), success && !dimension.queryRequired );
                }
            } else if( !n.sets.isEmpty() ) {
                for( ArrayBitSet set : n.sets ) {
                    final boolean eqSuccess = set.find( qValue );
                    trace( set.equal, query, result, buffer.cloneWith( n.dimension, set.bitSet.stream(),
                        set.include ? CONTAINS : NOT_CONTAINS, eqSuccess ), success && eqSuccess );
                }
            } else {
                final int direction = dimension.direction( qValue, n.eqValue );

                final boolean left = ( direction & Direction.LEFT ) > 0;
                trace( n.left, query, result, buffer.clone(), success && left );

                final boolean right = ( direction & Direction.RIGHT ) > 0;
                trace( n.right, query, result, buffer.clone(), success && right );

                final boolean eq = ( direction & Direction.EQUAL ) > 0;
                trace( n.equal, query, result, buffer.cloneWith( n.dimension, n.eqValue, dimension.operationType, eq ), success && eq );
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        print( root, out );

        return out.toString();
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

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static class Array extends ArrayList<Object> {
        public final boolean include;

        public Array( Collection<?> c, boolean include ) {
            super( c );
            this.include = include;
        }
    }

    public static class ValueData<T> {
        public final List<?> data;
        public final T value;

        public ValueData( List<?> data, T value ) {
            this.data = data;
            this.value = value;
        }
    }

    @ToString
    static class Leaf<T> implements TreeNode<T> {
        final List<T> selections;

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

    private static class TraceOperationTypeValues extends HashMap<OperationType, HashSet<Long>> {
        public void add( OperationType operationType, long eqValue ) {
            this.computeIfAbsent( operationType, ( ot ) -> new HashSet<>() ).add( eqValue );
        }

        public void addAll( OperationType operationType, HashSet<Long> v ) {
            this.computeIfAbsent( operationType, ( ot ) -> new HashSet<>() ).addAll( v );
        }

        public String toString( Dimension dimension ) {
            return entrySet().stream().map( e -> e.getValue().stream()
                .map( dimension::toString )
                .collect( joining( ",", "[", "]" ) ) + " " + e.getKey() )
                .collect( joining( ", " ) );
        }
    }

    private static class TraceBuffer extends HashMap<Integer, TraceOperationTypeValues> {


        public TraceBuffer() {
        }

        private TraceBuffer( Map<Integer, TraceOperationTypeValues> m ) {
            super( m );
        }

        @Override
        public TraceBuffer clone() {
            return new TraceBuffer( this );
        }

        public TraceBuffer cloneWith( int dimension, long eqValue, OperationType operationType, boolean success ) {
            return cloneWith( dimension, LongStream.of( eqValue ), operationType, success );
        }

        public TraceBuffer cloneWith( int dimension, IntStream eqValue, OperationType operationType, boolean success ) {
            return cloneWith( dimension, eqValue.mapToLong( v -> v ), operationType, success );
        }

        public TraceBuffer cloneWith( int dimension, LongStream eqValue, OperationType operationType, boolean success ) {
            final TraceBuffer clone = clone();
            if( !success ) {
                final TraceOperationTypeValues v = clone
                    .computeIfAbsent( dimension, ( d ) -> new TraceOperationTypeValues() );

                eqValue.forEach( eqv -> v.add( operationType, eqv ) );
            }
            return clone;
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

        public final boolean find( long[] qValue ) {
            if( include ) {
                for( long value : qValue ) {
                    if( bitSet.get( ( int ) value ) ) return true;
                }

                return false;
            }

            for( long value : qValue ) {
                if( bitSet.get( ( int ) value ) ) return false;
            }

            return true;
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

    @ToString
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
