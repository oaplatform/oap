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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 08.09.2017.
 */
@Slf4j
public class StatsDBMaster extends StatsDB<StatsDBMaster.MasterDatabase> implements RemoteStatsDB, Closeable {
    private final ConcurrentHashMap<String, String> hosts = new ConcurrentHashMap<>();

    public StatsDBMaster( KeySchema schema, Storage<Node> storage ) {
        super( schema, storage );
    }

    private static List<List<String>> merge( Storage<Node> storage, Map<String, Node> remoteDB ) {
        val retList = new ArrayList<List<String>>();

        remoteDB.forEach( ( key, rnode ) -> {
            storage.update( key,
                mnode -> {
                    merge( key, mnode, rnode, retList );
                    updateAggregates( mnode );
                },
                () -> {
                    final Node mnode = new Node( rnode.name );
                    merge( key, mnode, rnode, retList );
                    updateAggregates( mnode );
                    return mnode;
                } );
        } );

        return retList;
    }

    private static List<List<String>> merge( Map<String, Node> masterDB, Map<String, Node> remoteDB, List<List<String>> retList ) {
        for( val entry : remoteDB.entrySet() ) {
            val key = entry.getKey();
            val rNode = entry.getValue();

            val masterNode = masterDB.computeIfAbsent( key, ( k ) -> new Node( rNode.name ) );

            merge( key, masterNode, rNode, retList );
        }

        return retList;
    }

    private static void merge( String key, Node masterNode, Node rNode, List<List<String>> retList ) {
        val list = merge( masterNode.db, rNode.db, retList );
        list.forEach( l -> l.add( 0, key ) );

        retList.addAll( list );

        val ret = masterNode.merge( rNode );
        if( !ret ) {
            val k = new ArrayList<String>();
            k.add( key );
            retList.add( k );
        }
    }

    public void start() {
        init( storage.select() );
    }

    @SuppressWarnings( "unchecked" )
    private void init( Stream<Node> nodes ) {
        nodes.forEach( node -> {
            if( node.value instanceof Node.Container ) {
                init( node.db.values().stream() );
                ( ( Node.Container ) node.value ).aggregate( node.db.values().stream().map( b -> b.value ) );
            }
        } );
    }

    @Override
    protected MasterDatabase toDatabase( ConcurrentHashMap<String, Node> db ) {
        return new MasterDatabase( db, hosts );
    }

    @Override
    protected void start( MasterDatabase database ) {
        super.start( database );

        if( database.hosts != null )
            hosts.putAll( database.hosts );
    }

    @Override
    public boolean update( RemoteStatsDB.Sync sync, String host ) {
        synchronized( host.intern() ) {
            val lastId = hosts.getOrDefault( host, "" );
            if( sync.id.compareTo( lastId ) <= 0 ) {
                log.warn( "[{}] diff ({}) already merged. Last merged diff is ({})", host, sync.id, lastId );
                return true;
            }

            hosts.put( host, sync.id );

            val failedKeys = merge( storage, sync.data );

            if( !failedKeys.isEmpty() ) {
                log.error( "failed keys:" );
                failedKeys.forEach( key -> log.error( "[{}]: {}", host, key ) );
            }

            return true;
        }
    }

    @Override
    public void close() {
        storage.fsync();
    }

    public static class MasterDatabase extends StatsDB.Database {
        private static final long serialVersionUID = -5047870798844607693L;

        public ConcurrentHashMap<String, String> hosts;

        public MasterDatabase() {
        }

        public MasterDatabase( ConcurrentHashMap<String, Node> db, ConcurrentHashMap<String, String> hosts ) {
            super( db );
            this.hosts = hosts;
        }
    }
}
