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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.storage.Storage;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public static class Database implements Serializable {
        private static final long serialVersionUID = 20816260507748956L;

        public ConcurrentHashMap<String, Node> db;

        public Database() {
        }

        public Database( ConcurrentHashMap<String, Node> db ) {
            this.db = db;
        }
    }
}
