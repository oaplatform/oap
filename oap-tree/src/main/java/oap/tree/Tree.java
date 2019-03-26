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
import oap.tree.Dimension.OperationType;
import oap.util.Lists;
import oap.util.MemoryMeter;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static oap.tree.Consts.ANY_AS_ARRAY;
import static oap.tree.Dimension.Direction;
import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Dimension.OperationType.CONTAINS_ALL;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.util.Pair.__;

public class Tree<T> {
    private final int arrayToTree;
    TreeNode<T> root = new Leaf<>( emptyList() );
    private List<Dimension> dimensions;
    private double hashFillFactor;
    private long memory;
    private long nodeCount = 0;
    private long leafCount = 0;

    Tree( List<Dimension> dimensions ) {
        this( dimensions, 0.25, Integer.MAX_VALUE );
    }

    Tree( List<Dimension> dimensions, double hashFillFactor, int arrayToTree ) {
        this.dimensions = dimensions;
        this.hashFillFactor = hashFillFactor;
        this.arrayToTree = arrayToTree;
    }

    public static <T> ValueData<T> v( T selection, List<?> data ) {
        return new ValueData<>( data, selection );
    }

    @SafeVarargs
    public static <T> ArrayList<T> l( T... data ) {
        return Lists.of( data );
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

    @SafeVarargs
    public static <T> Array a( ArrayOperation operationType, T... values ) {
        return new Array( l( values ), operationType );
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

    public TreeArrayStatistic getArrayStatistics() {
        final TreeArrayStatistic tas = new TreeArrayStatistic();

        arrayStatistics( root, tas );

        return tas;
    }

    private void arrayStatistics( TreeNode<T> root, TreeArrayStatistic tas ) {
        if( root == null ) return;

        if( root instanceof Tree.Node ) {
            final List<ArrayBitSet> sets = ( ( Node ) root ).sets;

            tas.update( ArrayOperation.OR, Lists.count( sets, s -> s.operation == ArrayOperation.OR ) );
            tas.update( ArrayOperation.AND, Lists.count( sets, s -> s.operation == ArrayOperation.AND ) );
            tas.update( ArrayOperation.NOT, Lists.count( sets, s -> s.operation == ArrayOperation.NOT ) );

            sets.forEach( s -> tas.updateSize( s.operation, s.bitSet.stream().count() ) );

            arrayStatistics( ( ( Node ) root ).any, tas );
            arrayStatistics( ( ( Node ) root ).left, tas );
            arrayStatistics( ( ( Node ) root ).right, tas );
            arrayStatistics( ( ( Node ) root ).equal, tas );

            sets.forEach( s -> arrayStatistics( s.equal, tas ) );
        }
    }

    public void load( List<ValueData<T>> data ) {
        var newData = fixEmptyAsFailed( data );
        init( newData );
        final long[] uniqueCount = getUniqueCount( newData );
        root = toNode( newData, uniqueCount, new BitSet( dimensions.size() ) );

        updateCount( root );

        memory = MemoryMeter.get().measureDeep( this );
    }

    private List<ValueData<T>> fixEmptyAsFailed( List<ValueData<T>> data ) {
        if( Lists.find2( dimensions, d -> d.emptyAsFailed ) == null ) return data;

        var res = new ArrayList<ValueData<T>>( data.size() );

        next:
        for( var vd : data ) {
            for( int i = 0; i < dimensions.size(); i++ ) {
                if( !dimensions.get( i ).emptyAsFailed ) continue;

                var v = vd.data.get( i );
                if( v == null ) break next;
                if( v instanceof Optional && ( ( Optional ) v ).isEmpty() ) break next;
                if( v instanceof Array && ( ( Array ) v ).isEmpty() ) break next;
            }
            res.add( vd );
        }

        return res;
    }

    private long[] getUniqueCount( List<ValueData<T>> data ) {
        final long[] longs = new long[dimensions.size()];


        for( int i = 0; i < longs.length; i++ ) {
            int finalI = i;
            longs[i] = data.stream().map( d -> d.data.get( finalI ) ).distinct().count();
        }
        return longs;
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
        for( int i = 0; i < dimensions.size(); i++ ) {
            var p = dimensions.get( i );

            for( var dv : data ) {
                var v = dv.data.get( i );
                if( v instanceof Array ) {
                    for( var item : ( ( Array ) v ) ) {
                        p.init( item );
                    }
                } else {
                    p.init( v );
                }
            }
        }
    }

    private long[][] convertQueryToLong( List<?> query ) {
        final int size = dimensions.size();
        final long[][] longData = new long[size][];

        for( int i = 0; i < size; i++ ) {
            final Object value = query.get( i );
            final Dimension dimension = dimensions.get( i );
            longData[i] = dimension.getOrNullValue( value );
        }

        return longData;
    }

    @SuppressWarnings( "unchecked" )
    private TreeNode<T> toNode( List<ValueData<T>> data, long[] uniqueCount, BitSet eq ) {
        if( data.isEmpty() ) return null;

        final SplitDimension splitDimension = findSplitDimension( data, uniqueCount, eq );

        if( splitDimension == null ) return new Leaf<>( Lists.map( data, sd -> sd.value ) );

        final BitSet bitSetWithDimension = withSet( eq, splitDimension.dimension );

        final Dimension dimension = dimensions.get( splitDimension.dimension );


        if( splitDimension.hash.isEmpty() ) {
            final List<ArrayBitSet> sets = Lists.map( Lists.groupBy(
                splitDimension.sets,
                s -> s.data.get( splitDimension.dimension )
            ).entrySet(), es -> {
                final Array key = ( Array ) es.getKey();
                return new ArrayBitSet( dimension.toBitSet( key ), key.operation, toNode( es.getValue(), uniqueCount, bitSetWithDimension ) );
            } );

            return new Node(
                splitDimension.dimension,
                splitDimension.value,
                toNode( splitDimension.left, uniqueCount, eq ),
                toNode( splitDimension.right, uniqueCount, eq ),
                toNode( splitDimension.equal, uniqueCount, bitSetWithDimension ),
                toNode( splitDimension.any, uniqueCount, bitSetWithDimension ),
                sets
            );
        } else {

            final Map<Integer, List<ValueData<T>>> map = Lists.groupBy( splitDimension.hash,
                d -> ( int ) dimension.getOrDefault( d.data.get( splitDimension.dimension ), ANY_AS_ARRAY )[0] );

            final int max = Collections.max( map.keySet() );

            final TreeNode<T>[] array = new TreeNode[max + 1];
            Arrays.fill( array, null );

            map.forEach( ( p, l ) -> array[p] = toNode( l, uniqueCount, bitSetWithDimension ) );

            return new HashNode(
                splitDimension.dimension,
                array,
                toNode( splitDimension.any, uniqueCount, bitSetWithDimension )
            );
        }
    }

    private BitSet withSet( BitSet eq, int dimension ) {
        final BitSet bitSet = BitSet.valueOf( eq.toLongArray() );
        bitSet.set( dimension );
        return bitSet;
    }

    private SplitDimension findSplitDimension( List<ValueData<T>> data, long[] uniqueCount, BitSet eqBitSet ) {
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

            for( var vd : data ) {
                final Object value = vd.data.get( i );
                if( value instanceof Array ) {
                    final Array array = ( Array ) value;
                    if( !array.isEmpty() ) uniqueArray.add( array );
                } else {
                    final long[] longValue = dimension.getOrDefault( value, ANY_AS_ARRAY );
                    if( longValue != ANY_AS_ARRAY ) unique.add( longValue[0] );
                }

            }

            if( !isArray && unique.size() > 0 && ( unique.size() > uniqueSize || dimension.priority > priority ) ) {
                uniqueSize = unique.size();
                splitDimension = i;
                priority = dimension.priority;
            } else if( isArray && uniqueArray.size() > 0 && ( uniqueArray.size() < uniqueArraySize || dimension.priority > priorityArray ) ) {
                uniqueArraySize = uniqueArray.size();
                splitArrayDimension = i;
                priorityArray = dimension.priority;
            }
        }

        if( splitDimension < 0 && splitArrayDimension < 0 ) return null;

        final int finalSplitDimension = splitDimension >= 0 ? splitDimension : splitArrayDimension;

        final Dimension dimension = dimensions.get( finalSplitDimension );

        if( dimension.operationType == null ) { //array

            Pair<List<ValueData<T>>, List<ValueData<T>>> partition = Lists.partition( data, vd -> !( ( Array ) vd.data.get( finalSplitDimension ) ).isEmpty() );

            return new SplitDimension( finalSplitDimension, Consts.ANY, emptyList(), emptyList(), emptyList(), partition._2, partition._1, emptyList() );
        } else {

            var partitionAnyOther = Stream.of( data ).partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ), ANY_AS_ARRAY ) == ANY_AS_ARRAY );

            final List<ValueData<T>> sorted = partitionAnyOther._2
                .sorted( Comparator.comparingLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ), ANY_AS_ARRAY )[0] ) )
                .collect( toList() );

            final long[] unique = sorted
                .stream()
                .mapToLong( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ), ANY_AS_ARRAY )[0] ).distinct().toArray();

            if( ( dimension.operationType == CONTAINS || dimension.operationType == CONTAINS_ALL )
                && unique.length > 1
                && ( double ) unique.length / uniqueCount[finalSplitDimension] > hashFillFactor ) {
                final List<ValueData<T>> any = partitionAnyOther._1.collect( toList() );

                return new SplitDimension( finalSplitDimension, Consts.ANY, emptyList(), emptyList(), emptyList(), any, emptyList(), sorted );
            } else {

//                final long splitValue = dimension.getOrDefault( sorted.get( sorted.size() / 2).data.get( finalSplitDimension ), ANY_AS_ARRAY )[0];
                final long splitValue = unique[unique.length / 2];

                var partitionLeftEqRight = Stream.of( sorted )
                    .partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ), ANY_AS_ARRAY )[0] < splitValue );
                var partitionEqRight = partitionLeftEqRight._2
                    .partition( sd -> dimension.getOrDefault( sd.data.get( finalSplitDimension ), ANY_AS_ARRAY )[0] == splitValue );

                final List<ValueData<T>> left = partitionLeftEqRight._1.collect( toList() );
                final List<ValueData<T>> right = partitionEqRight._2.collect( toList() );
                final List<ValueData<T>> eq = partitionEqRight._1.collect( toList() );
                final List<ValueData<T>> any = Stream.of( partitionAnyOther._1 ).collect( toList() );

                return new SplitDimension( finalSplitDimension, splitValue, left, right, eq, any, emptyList(), emptyList() );
            }
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
        } else if( node instanceof Tree.Node ) {
            final Node n = ( Node ) node;
            find( n.any, query, result );

            final long[] qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY_AS_ARRAY ) return;

            var sets = n.sets;
            if( !sets.isEmpty() ) {
                for( ArrayBitSet set : sets ) {
                    if( set.find( qValue ) ) {
                        find( set.equal, query, result );
                    }
                }
            } else {
                var direction = dimension.direction( qValue, n.eqValue );
                if( ( direction & Direction.LEFT ) > 0 )
                    find( n.left, query, result );
                if( ( direction & Direction.EQUAL ) > 0 )
                    find( n.equal, query, result );
                if( ( direction & Direction.RIGHT ) > 0 )
                    find( n.right, query, result );
            }
        } else {
            final HashNode n = ( HashNode ) node;
            find( n.any, query, result );

            final long[] qValue = query[n.dimension];

            final TreeNode<T>[] hash = n.hash;
            if( qValue == ANY_AS_ARRAY ) return;

            for( long aQValue : qValue ) {
                final int index = ( int ) aQValue;
                if( index < hash.length ) {
                    find( hash[index], query, result );
                }
            }
        }
    }

    public String trace( List<?> query ) {
        return trace( query, ( key ) -> true );
    }

    public String trace( List<?> query, Predicate<T> filter ) {
        final HashMap<T, HashMap<Integer, TraceOperationTypeValues>> result = new HashMap<>();
        final long[][] longQuery = convertQueryToLong( query );
        trace( root, longQuery, result, new TraceBuffer(), true );


        final String queryStr = "query = " + Stream.of( query )
            .zipWithIndex()
            .map( p -> dimensions.get( p._2 ).name + ":" + printValue( p._1 ) )
            .collect( joining( ",", "[", "]" ) ) + "\n";

        final String out = result
            .entrySet()
            .stream()
            .filter( e -> filter.test( e.getKey() ) )
            .map( e -> e.getKey().toString() + ": \n"
                    + e.getValue().entrySet().stream().map( dv -> {
                    final Dimension dimension = dimensions.get( dv.getKey() );
                    return "    " + dimension.name + "/" + dv.getKey() + ": "
                        + dv.getValue().toString( dimension ) + " " + queryToString( query, dv.getKey() );
                }
                ).collect( joining( "\n" ) )
            ).collect( joining( "\n" ) );

        if( root == null ) {
            return queryStr + "Tree is empty";
        }

        return queryStr + ( out.length() > 0 ? "Expecting:\n" + out : "ALL OK" );
    }

    private String printValue( Object o ) {
        if( o == null
            || ( o instanceof Optional<?> && !( ( Optional<?> ) o ).isPresent() )
            || ( o instanceof List<?> && ( ( List<?> ) o ).isEmpty() )
        ) {
            return Strings.UNKNOWN;
        }
        return o.toString();
    }

    public Map<T, Map<String, Integer>> traceStatistics( List<List<?>> queries ) {
        final HashMap<T, Map<String, Integer>> resultStats = new HashMap<>();

        for( List<?> query : queries ) {
            final HashMap<T, HashMap<Integer, TraceOperationTypeValues>> result = new HashMap<>();
            final long[][] longQuery = convertQueryToLong( query );
            trace( root, longQuery, result, new TraceBuffer(), true );

            final Map<T, Map<String, Integer>> stats = result
                .entrySet()
                .stream()
                .collect( toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().entrySet().stream().collect( toMap(
                        dv -> dimensions.get( dv.getKey() ).name,
                        dv -> 1
                    ) )
                ) );

            mergeInto( stats, resultStats );
        }

        return resultStats;
    }

    private void mergeInto( Map<T, Map<String, Integer>> stat, HashMap<T, Map<String, Integer>> result ) {
        stat.forEach( ( s, m ) -> {
            final Map<String, Integer> statBySelection = result.computeIfAbsent( s, ( ss ) -> new HashMap<>() );

            m.forEach( ( dimension, count ) ->
                statBySelection.compute( dimension, ( d, c ) -> c == null ? count : c + count ) );
        } );

    }

    private String queryToString( List<?> query, int key ) {
        final Object value = query.get( key );
        if( value instanceof List<?> ) {
            return ( ( List<?> ) value ).stream().map( v -> v == null ? Strings.UNKNOWN
                : String.valueOf( v ) ).collect( joining( ",", "[", "]" ) );
        } else {
            return queryValueToString( value );
        }
    }

    private String queryValueToString( Object value ) {
        return value == null || value == Optional.empty() ? Strings.UNKNOWN : String.valueOf( value );
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
        } else if( node instanceof Tree.Node ) {
            final Node n = ( Node ) node;
            trace( n.any, query, result, buffer.clone(), success );

            final long[] qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY_AS_ARRAY ) {
                trace( n.equal, query, result, buffer.cloneWith( n.dimension, n.eqValue, dimension.operationType, false ), false );
                trace( n.right, query, result, buffer.clone(), false );
                trace( n.left, query, result, buffer.clone(), false );

                for( ArrayBitSet set : n.sets ) {
                    trace( set.equal, query, result,
                        buffer.cloneWith( n.dimension, set.bitSet.stream(),
                            set.operation.operationType, false ), false );
                }
            } else if( !n.sets.isEmpty() ) {
                for( ArrayBitSet set : n.sets ) {
                    final boolean eqSuccess = set.find( qValue );
                    trace( set.equal, query, result, buffer.cloneWith( n.dimension, set.bitSet.stream(),
                        set.operation.operationType, eqSuccess ), success && eqSuccess );
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
        } else {
            final HashNode n = ( HashNode ) node;
            trace( n.any, query, result, buffer.clone(), success );

            final long[] qValue = query[n.dimension];

            final Dimension dimension = dimensions.get( n.dimension );

            if( qValue == ANY_AS_ARRAY ) {
                for( TreeNode<T> s : n.hash ) {
                    trace( s, query, result, buffer.clone(), false );
                }
            } else {
                for( int i = 0; i < n.hash.length; i++ ) {
                    final boolean contains = ArrayUtils.contains( qValue, i );
                    trace( n.hash[i], query, result, buffer.cloneWith( n.dimension, i, dimension.operationType, contains ), success && contains );
                }
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
        } else if( node instanceof Tree.Node ) {
            final Node n = ( Node ) node;
            findMaxDepth( n.left, maxDepth, currentDepth + 1 );
            findMaxDepth( n.right, maxDepth, currentDepth + 1 );
            findMaxDepth( n.any, maxDepth, currentDepth + 1 );
            findMaxDepth( n.equal, maxDepth, currentDepth + 1 );

            for( ArrayBitSet abs : n.sets ) {
                findMaxDepth( abs.equal, maxDepth, currentDepth + 1 );
            }
        } else {
            final HashNode n = ( HashNode ) node;

            findMaxDepth( n.any, maxDepth, currentDepth + 1 );
            for( int i = 0; i < n.hash.length; i++ ) {
                findMaxDepth( n.hash[i], maxDepth, currentDepth + 1 );
            }
        }
    }

    private void print( String prefix, boolean isTail, TreeNode<T> node, StringBuilder out, String type ) {
        out.append( prefix ).append( isTail ? "└── " : "├── " ).append( type ).append( ":" );
        if( node != null ) {
            node.print( out );
            out.append( "\n" );

            var children = Lists.filter( node.children(), p -> p._2 != null );

            for( int i = 0; i < children.size(); i++ ) {
                final Pair<String, TreeNode<T>> child = children.get( i );
                final String name = child._1;
                final TreeNode<T> value = child._2;

                if( value != null )
                    print( prefix + ( isTail ? "    " : "│   " ), i + 1 >= children.size(), value, out, name );

            }
        }
    }

    public enum ArrayOperation {
        OR( CONTAINS ), AND( CONTAINS_ALL ), NOT( NOT_CONTAINS );

        public final OperationType operationType;

        ArrayOperation( OperationType operationType ) {
            this.operationType = operationType;
        }
    }

    private interface TreeNode<T> {
        List<Pair<String, TreeNode<T>>> children();

        void print( StringBuilder out );
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static class Array extends ArrayList<Object> {
        public final ArrayOperation operation;

        public Array( Collection<?> c, ArrayOperation operation ) {
            super( c );
            this.operation = operation;
        }
    }

    public static class ValueData<T> {
        public final List<?> data;
        public final T value;

        public ValueData( List<?> data, T value ) {
            this.data = data;
            this.value = value;
        }

        public ValueData<T> cloneWith( int index, Object item ) {
            var data = new ArrayList<Object>( this.data );
            data.set( index, item );
            return new ValueData<>( data, value );
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

    public static class TreeArrayStatistic {
        public final HashMap<ArrayOperation, HashMap<Long, AtomicInteger>> counts = new HashMap<>();
        public final HashMap<ArrayOperation, HashMap<Long, AtomicInteger>> size = new HashMap<>();

        public void update( ArrayOperation operationType, long count ) {
            if( count > 0 )
                counts
                    .computeIfAbsent( operationType, op -> new HashMap<>() )
                    .computeIfAbsent( count, ( i ) -> new AtomicInteger() )
                    .incrementAndGet();
        }

        public void updateSize( ArrayOperation operationType, long count ) {
            size
                .computeIfAbsent( operationType, op -> new HashMap<>() )
                .computeIfAbsent( count, ( i ) -> new AtomicInteger() )
                .incrementAndGet();
        }
    }

    @ToString
    private class ArrayBitSet {
        private final BitSet bitSet;
        private final ArrayOperation operation;
        private final TreeNode<T> equal;

        public ArrayBitSet( BitSet bitSet, ArrayOperation operation, TreeNode<T> equal ) {
            this.bitSet = bitSet;
            this.operation = operation;
            this.equal = equal;
        }

        public final boolean find( long[] qValue ) {
            switch( operation ) {
                case OR:
                    for( long value : qValue ) {
                        if( bitSet.get( ( int ) value ) ) return true;
                    }

                    return false;
                case AND:
                    var newBitSet = ( BitSet ) bitSet.clone();
                    for( long value : qValue ) {
                        newBitSet.clear( ( int ) value );
                    }

                    return newBitSet.isEmpty();
                case NOT:
                    for( long value : qValue ) {
                        if( bitSet.get( ( int ) value ) ) return false;
                    }

                    return true;
                default:
                    throw new IllegalStateException( "Unknown Operation type " + operation.name() );
            }
        }
    }

    private class SplitDimension {
        private final List<ValueData<T>> left;
        private final List<ValueData<T>> right;
        private final List<ValueData<T>> equal;
        private final List<ValueData<T>> any;
        private final List<ValueData<T>> sets;
        private final List<ValueData<T>> hash;
        private final int dimension;
        private final long value;

        private SplitDimension(
            int dimension,
            long value,
            List<ValueData<T>> left,
            List<ValueData<T>> right,
            List<ValueData<T>> equal,
            List<ValueData<T>> any,
            List<ValueData<T>> sets,
            List<ValueData<T>> hash
        ) {
            this.dimension = dimension;
            this.value = value;

            this.left = left;
            this.right = right;
            this.equal = equal;
            this.any = any;
            this.sets = sets;
            this.hash = hash;
        }
    }

    @ToString
    class HashNode implements TreeNode<T> {
        final TreeNode<T>[] hash;
        final int dimension;
        final TreeNode<T> any;

        public HashNode( int dimension, TreeNode<T>[] hash, TreeNode<T> any ) {
            this.hash = hash;
            this.dimension = dimension;
            this.any = any;
        }

        @Override
        public List<Pair<String, TreeNode<T>>> children() {
            final ArrayList<Pair<String, TreeNode<T>>> result = new ArrayList<>();
            result.add( __( "a", any ) );

            for( int i = 0; i < hash.length; i++ ) {
                final TreeNode<T> heq = hash[i];
                result.add( __( "h" + i, heq ) );
            }

            return result;
        }

        @Override
        public void print( StringBuilder out ) {
            final Dimension dimension = dimensions.get( this.dimension );
            out.append( "kdh|" )
                .append( "d:" )
                .append( dimension.name ).append( '/' ).append( this.dimension );
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

            for( ArrayBitSet set : sets )
                result.add( __( ( set.operation.name() + ":" ) + bitSetToData( set.bitSet ), set.equal ) );

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
