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

package oap.statsdb;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class StatsDB extends IStatsDB {
    protected final NodeSchema schema;
    protected volatile ConcurrentHashMap<String, Node> db = new ConcurrentHashMap<>();

    public StatsDB( NodeSchema schema ) {
        this.schema = schema;
    }

    @SuppressWarnings( "unchecked" )
    protected static void updateAggregates( Node mnode ) {
        for( var node : mnode.db.values() ) {
            updateAggregates( node );
        }

        var value = mnode.v;
        if( value instanceof Node.Container ) {
            ( ( Node.Container ) value ).aggregate( mnode.db.values().stream()
                .map( n -> n.v )
                .filter( Objects::nonNull )
                .collect( toList() )
            );
        }
    }

    protected <V extends Node.Value<V>> void update( String[] key, Consumer<V> update ) {
        assert key != null;
        assert key.length > 0;

        var rootKey = key[0];

        db.compute( rootKey, ( k, n ) -> {
            Node newNode = n;
            if( newNode == null ) {
                newNode = new Node( schema.get( 0 ).newInstance() );
            }

            updateNode( key, update, newNode, schema );

            return newNode;
        } );
    }

    protected <V extends Node.Value<V>>
    void update( String key1, Consumer<V> update ) {
        update( new String[] { key1 }, update );
    }

    protected <V extends Node.Value<V>>
    void update( String key1, String key2, Consumer<V> update ) {
        update( new String[] { key1, key2 }, update );
    }

    protected <V extends Node.Value<V>>
    void update( String key1, String key2, String key3, Consumer<V> update ) {
        update( new String[] { key1, key2, key3 }, update );
    }

    protected <V extends Node.Value<V>>
    void update( String key1, String key2, String key3, String key4, Consumer<V> update ) {
        update( new String[] { key1, key2, key3, key4 }, update );
    }

    protected <V extends Node.Value<V>>
    void update( String key1, String key2, String key3, String key4, String key5, Consumer<V> update ) {
        update( new String[] { key1, key2, key3, key4, key5 }, update );
    }

    @SuppressWarnings( "unchecked" )
    public <V extends Node.Value<V>> V get( String... key ) {
        var node = getNode( key );
        return node != null ? ( V ) node.v : null;
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    public Node _getNode( String[] key, int position, Node node ) {
        if( node == null ) return null;
        if( position >= key.length ) return node;

        return _getNode( key, position + 1, node.db.get( key[position] ) );
    }

    protected Node getNode( String... key ) {
        if( key.length == 0 ) return null;

        return _getNode( key, 1, db.get( key[0] ) );
    }

    public <V extends Node.Value<V>> Stream<V> children( String... key ) {
        if( key.length == 0 ) return Stream.empty();

        return _children( key, 1, db.get( key[0] ) );
    }

    @SuppressWarnings( { "unchecked", "checkstyle:MethodName" } )
    private <V extends Node.Value<V>> Stream<V> _children( String[] key, int position, Node node ) {
        if( node == null ) return Stream.empty();
        if( position >= key.length ) return node.db.values().stream().map( n -> ( V ) n.v );

        return _children( key, position + 1, node.db.get( key[position] ) );
    }

    public <N extends Node, V extends Node.Value<V>> N updateNode( String[] key,
                                                                   Consumer<V> update,
                                                                   N node,
                                                                   NodeSchema schema ) {
        Node tNode = node;

        for( int i = 1; i < key.length; i++ ) {
            var keyItem = key[i];
            var finalI = i;
            tNode = tNode.db.computeIfAbsent( keyItem, k -> new Node( schema.get( finalI ).newInstance() ) );
        }

        tNode.updateValue( update );

        return node;
    }

    public synchronized void removeAll() {
        db.clear();
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>> Stream<Select2<T1, T2>> select2() {
        return
            db.entrySet().stream()
                .flatMap( e1 -> e1.getValue().db.entrySet().stream().map(
                    e2 -> new Select2<>( e1.getKey(), ( T1 ) e1.getValue().v, e2.getKey(), ( T2 ) e2.getValue().v ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>> Stream<Select3<T1, T2, T3>> select3() {
        return
            db.entrySet().stream()
                .flatMap( e1 -> e1.getValue().db.entrySet().stream().flatMap(
                    e2 -> e2.getValue().db.entrySet().stream().map(
                        e3 -> new Select3<>( e1.getKey(), ( T1 ) e1.getValue().v,
                            e2.getKey(), ( T2 ) e2.getValue().v,
                            e3.getKey(), ( T3 ) e3.getValue().v ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>> Stream<Select4<T1, T2, T3, T4>> select4() {
        return
            db.entrySet().stream()
                .flatMap( e1 -> e1.getValue().db.entrySet().stream().flatMap(
                    e2 -> e2.getValue().db.entrySet().stream().flatMap(
                        e3 -> e3.getValue().db.entrySet().stream().map(
                            e4 -> new Select4<>( e1.getKey(), ( T1 ) e1.getValue().v,
                                e2.getKey(), ( T2 ) e2.getValue().v,
                                e3.getKey(), ( T3 ) e3.getValue().v,
                                e4.getKey(), ( T4 ) e4.getValue().v ) ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>, T5 extends Node.Value<T5>> Stream<Select5<T1, T2, T3, T4, T5>> select5() {
        return
            db.entrySet().stream()
                .flatMap( e1 -> e1.getValue().db.entrySet().stream().flatMap(
                    e2 -> e2.getValue().db.entrySet().stream().flatMap(
                        e3 -> e3.getValue().db.entrySet().stream().flatMap(
                            e4 -> e4.getValue().db.entrySet().stream().map(
                                e5 -> new Select5<>( e1.getKey(), ( T1 ) e1.getValue().v,
                                    e2.getKey(), ( T2 ) e2.getValue().v,
                                    e3.getKey(), ( T3 ) e3.getValue().v,
                                    e4.getKey(), ( T4 ) e4.getValue().v,
                                    e5.getKey(), ( T5 ) e5.getValue().v ) ) ) ) ) );
    }

    @ToString
    @AllArgsConstructor
    public static class Select2<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>> {
        public final String id1;
        public final T1 v1;
        public final String id2;
        public final T2 v2;
    }

    @ToString
    @AllArgsConstructor
    public static class Select3<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>> implements Serializable {
        private static final long serialVersionUID = 3812951337765151702L;

        public final String id1;
        public final T1 v1;
        public final String id2;
        public final T2 v2;
        public final String id3;
        public final T3 v3;
    }

    @ToString
    @AllArgsConstructor
    public static class Select4<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>> implements Serializable {
        private static final long serialVersionUID = 7466796137360157099L;

        public final String id1;
        public final T1 v1;
        public final String id2;
        public final T2 v2;
        public final String id3;
        public final T3 v3;
        public final String id4;
        public final T4 v4;
    }

    @ToString
    @AllArgsConstructor
    public static class Select5<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>, T5 extends Node.Value<T5>> implements Serializable {
        private static final long serialVersionUID = -8184723490764842795L;

        public final String id1;
        public final T1 v1;
        public final String id2;
        public final T2 v2;
        public final String id3;
        public final T3 v3;
        public final String id4;
        public final T4 v4;
        public final String id5;
        public final T5 v5;
    }
}
