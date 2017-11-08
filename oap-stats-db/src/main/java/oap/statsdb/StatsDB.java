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
import lombok.val;
import oap.storage.Storage;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@Slf4j
public abstract class StatsDB<T extends StatsDB.Database> {
    protected final Storage<Node> storage;

    public StatsDB( Storage<Node> storage ) {
        this.storage = storage;
    }

    protected abstract T toDatabase( ConcurrentHashMap<String, Node> db );

    protected <TKey extends Iterable<String>, TValue extends Node.Value<TValue>> void update( KeySchema schema, TKey key, Consumer<TValue> update, Supplier<TValue> create ) {
        val iterator = key.iterator();
        val schemaIterator = schema.iterator();
        val schemaKey = schemaIterator.next();
        storage.update( iterator.next(),
            node -> updateNode( update, create, iterator, node, schemaIterator ),
            () -> updateNode( update, create, iterator, new Node( schemaKey ), schemaIterator )
        );
    }

    public <TKey extends Iterable<String>, TValue extends Node.Value<TValue>> TValue get( TKey key ) {
        val it = key.iterator();
        if( !it.hasNext() ) return null;

        val dbKey = it.next();

        return storage.get( dbKey ).map( node -> node.<TValue>get( it ) ).orElse( null );
    }

    public <TValue extends Node.Value<TValue>> Node updateNode(
        Consumer<TValue> update, Supplier<TValue> create, Iterator<String> iterator, final Node node, Iterator<String> schemaIterator ) {
        Node tNode = node;
        while( iterator.hasNext() ) {
            val keyItem = iterator.next();
            val schemaItem = schemaIterator.next();
            tNode = tNode.db.computeIfAbsent( keyItem, ( k ) -> new Node( schemaItem ) );
        }

        tNode.updateValue( update, create );

        return node;
    }

    protected void start( T database ) {

    }

    public synchronized void removeAll() {
        storage.deleteAll();
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>> Stream<Select2<T1, T2>> select2() {
        return
            storage.select()
                .flatMap( n1 -> n1.db.values().stream().map(
                    n2 -> new Select2<>( ( T1 ) n1.value, ( T2 ) n2.value ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>> Stream<Select3<T1, T2, T3>> select3() {
        return
            storage.select()
                .flatMap( n1 -> n1.db.values().stream().flatMap(
                    n2 -> n2.db.values().stream().map(
                        n3 -> new Select3<>( ( T1 ) n1.value, ( T2 ) n2.value, ( T3 ) n3.value ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>> Stream<Select4<T1, T2, T3, T4>> select4() {
        return
            storage.select()
                .flatMap( n1 -> n1.db.values().stream().flatMap(
                    n2 -> n2.db.values().stream().flatMap(
                        n3 -> n3.db.values().stream().map(
                            n4 -> new Select4<>( ( T1 ) n1.value, ( T2 ) n2.value, ( T3 ) n3.value, ( T4 ) n4.value ) ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>, T5 extends Node.Value<T5>> Stream<Select5<T1, T2, T3, T4, T5>> select5() {
        return
            storage.select()
                .flatMap( n1 -> n1.db.values().stream().flatMap(
                    n2 -> n2.db.values().stream().flatMap(
                        n3 -> n3.db.values().stream().flatMap(
                            n4 -> n4.db.values().stream().map(
                                n5 -> new Select5<>( ( T1 ) n1.value, ( T2 ) n2.value, ( T3 ) n3.value, ( T4 ) n4.value, ( T5 ) n5.value ) ) ) ) ) );
    }

    public static class Database implements Serializable {
        private static final long serialVersionUID = 20816260507748956L;

        public ConcurrentHashMap<String, Node> db;

        public Database() {
        }

        public Database( ConcurrentHashMap<String, Node> db ) {
            this.db = db;
        }
    }

    @ToString
    @AllArgsConstructor
    public static class Select2<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>> {
        public final T1 v1;
        public final T2 v2;
    }

    @ToString
    @AllArgsConstructor
    public static class Select3<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>> implements Serializable {
        private static final long serialVersionUID = 3812951337765151702L;

        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
    }

    @ToString
    @AllArgsConstructor
    public static class Select4<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>> implements Serializable {
        private static final long serialVersionUID = 7466796137360157099L;

        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        public final T4 v4;
    }

    @ToString
    @AllArgsConstructor
    public static class Select5<T1 extends Node.Value<T1>, T2 extends Node.Value<T2>, T3 extends Node.Value<T3>, T4 extends Node.Value<T4>, T5 extends Node.Value<T5>> implements Serializable {
        private static final long serialVersionUID = -8184723490764842795L;

        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        public final T4 v4;
        public final T5 v5;
    }
}
