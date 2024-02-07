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
import oap.statsdb.RemoteStatsDB.Sync;
import oap.util.Lists;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class StatsDBMaster extends StatsDB implements Closeable, Runnable {
    private final StatsDBStorage storage;

    public StatsDBMaster( NodeSchema schema, StatsDBStorage storage ) {
        super( schema );
        this.storage = storage;

        db.putAll( storage.load( schema ) );
        init( db.values() );
    }

    private void merge( String key, Node masterNode, Node rNode, List<List<String>> retList, int level ) {
        if( log.isTraceEnabled() )
            log.trace( "merge {}/{}[{}]::{}", schema.get( level ).key, schema.get( level ).clazz, level, key );
        assert Objects.equals( masterNode.v.getClass(), rNode.v.getClass() )
            : "[" + level + "]/" + key + "::" + masterNode.v.getClass() + " vs " + rNode.v.getClass();

        var list = merge( masterNode.db, rNode.db, retList, level );
        list.forEach( l -> l.add( 0, key ) );

        retList.addAll( list );

        var ret = masterNode.merge( rNode );
        if( !ret ) {
            var k = new ArrayList<String>();
            k.add( key );
            retList.add( k );
        }
    }

    private List<List<String>> merge( Map<String, Node> masterDB, Map<String, Node> remoteDB, List<List<String>> retList, int level ) {
        for( var entry : remoteDB.entrySet() ) {
            var key = entry.getKey();
            var rNode = entry.getValue();

            var masterNode = masterDB.computeIfAbsent( key, k -> new Node( schema.get( level + 1 ).newInstance() ) );

            merge( key, masterNode, rNode, retList, level + 1 );
        }

        return retList;
    }

    private List<List<String>> merge( ArrayList<Sync.NodeIdNode> remoteDB ) {
        assert remoteDB != null;

        var retList = new ArrayList<List<String>>();

        var remoteDbTree = toTree( remoteDB );

        remoteDbTree.forEach( ( key, rnode ) -> {
            var mnode = db.computeIfAbsent( key, k -> new Node( schema.get( 0 ).newInstance() ) );

            merge( key, mnode, rnode, retList, 0 );
            updateAggregates( mnode );
        } );

        return retList;
    }

    private Map<String, Node> toTree( ArrayList<Sync.NodeIdNode> remoteDB ) {
        var ret = new HashMap<String, Node>();

        for( var nodeIdNode : remoteDB ) {
            var node = nodeIdNode.node;
            var nodeId = nodeIdNode.nodeId;

            Node treeNode = null;
            for( var i = 0; i < nodeId.size(); i++ ) {
                var key = nodeId.get( i );
                var finalI = i;
                treeNode = ( treeNode != null ? treeNode.db
                    : ret ).computeIfAbsent( key, k -> new Node( schema.get( finalI ).newInstance() ) );
            }

            treeNode.set( node );
        }

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    private void init( Collection<Node> nodes ) {
        nodes.forEach( node -> {
            if( node.v instanceof Node.Container ) {
                init( node.db.values() );
                ( ( Node.Container ) node.v ).aggregate( Lists.map( node.db.values(), b -> b.v ) );
            }
        } );
    }

    public boolean update( Sync sync, String host ) {
        assert sync != null;
        assert sync.data != null;

        synchronized( host.intern() ) {
            var failedKeys = merge( sync.data );

            if( !failedKeys.isEmpty() ) {
                log.error( "failed keys:" );
                failedKeys.forEach( key -> log.error( "[{}]: {}", host, key ) );
            }

            return true;
        }
    }

    public void reset() {
        removeAll();
        storage.removeAll();
    }

    @Override
    public void close() {
        storage.store( schema, db );
    }

    @Override
    public void run() {
        storage.store( schema, db );
    }
}
