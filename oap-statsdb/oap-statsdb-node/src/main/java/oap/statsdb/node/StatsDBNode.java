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

package oap.statsdb.node;

import lombok.extern.slf4j.Slf4j;
import oap.statsdb.IStatsDB;
import oap.statsdb.Node;
import oap.statsdb.NodeId;
import oap.statsdb.NodeSchema;
import oap.statsdb.RemoteStatsDB;
import oap.util.Cuid;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class StatsDBNode extends IStatsDB implements Runnable, Closeable {
    public final ConcurrentHashMap<NodeId, Node> nodes = new ConcurrentHashMap<>();
    protected final NodeSchema schema;
    private final StatsDBTransport transport;
    private final Cuid timestamp;
    public boolean lastSyncSuccess = false;

    public StatsDBNode( NodeSchema schema, StatsDBTransport transport ) {
        this( schema, transport, Cuid.UNIQUE );
    }

    public StatsDBNode( NodeSchema schema, StatsDBTransport transport, Cuid timestamp ) {
        this.schema = schema;
        this.transport = transport;
        this.timestamp = timestamp;
    }

    public synchronized void sync() {
        try {
            ArrayList<RemoteStatsDB.Sync.NodeIdNode> snapshot = snapshot();
            if( !snapshot.isEmpty() ) {
                var sync = new RemoteStatsDB.Sync( snapshot, timestamp.next() );
                transport.sendAsync( sync );
            }

            lastSyncSuccess = true;
        } catch( Exception e ) {
            lastSyncSuccess = false;
            log.error( e.getMessage(), e );
        }
    }

    private ArrayList<RemoteStatsDB.Sync.NodeIdNode> snapshot() {
        var ret = new ArrayList<RemoteStatsDB.Sync.NodeIdNode>();
        for( var entry : new ArrayList<>( nodes.entrySet() ) ) {
            ret.add( new RemoteStatsDB.Sync.NodeIdNode( entry.getKey(), entry.getValue() ) );
            nodes.remove( entry.getKey() );
        }

        return ret;
    }

    @Override
    public void run() {
        sync();
    }

    @Override
    public synchronized void removeAll() {
        nodes.clear();
    }

    @Override
    protected <V extends Node.Value<V>> void update( String[] keys, Consumer<V> update ) {
        nodes.compute( new NodeId( keys ), ( nid, n ) -> {
            Node newNode = n;
            if( newNode == null ) newNode = new Node( schema.get( keys.length - 1 ).newInstance() );
            newNode.updateValue( update );

            return newNode;
        } );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <V extends Node.Value<V>> V get( String... key ) {
        var node = nodes.get( new NodeId( key ) );
        return node != null ? ( V ) node.v : null;
    }

    @Override
    public void close() {
        log.info( "close" );
        sync();
    }
}
