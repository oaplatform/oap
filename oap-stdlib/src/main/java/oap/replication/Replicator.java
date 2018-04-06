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

package oap.replication;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.replication.Replication.ReplicationMaster;
import oap.replication.Replication.ReplicationSlave;
import oap.util.Lists;
import oap.util.Optionals;
import oap.util.Stream;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Replicator<M> implements Closeable {
    private final ReplicationSlave<M> slave;
    private final ReplicationMaster<M> master;
    private final Scheduled scheduled;
    protected int batchSize = 10;

    public Replicator( ReplicationSlave<M> slave, ReplicationMaster<M> master, long interval, long safeModificationTime ) {
        this.slave = slave;
        this.master = master;
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), interval, safeModificationTime, this::replicate );
    }

    public Replicator( Replication<M> slave, Replication<M> master, long interval, long safeModificationTime ) {
        this( slave.slave(), master.master(), interval, safeModificationTime );
    }

    public Replicator( Replication<M> slave, Replication<M> master, long interval ) {
        this( slave.slave(), master.master(), interval );
    }

    public Replicator( ReplicationSlave<M> slave, ReplicationMaster<M> master, long interval ) {
        this( slave, master, interval, 1000 );
    }

    public synchronized void replicate( long last ) {
        List<M> newUpdates = Lists.empty();
        for( int b = 0; b < 100000; b++ ) {
            int offset = b * batchSize;
            List<M> updates = master.updatedSince( last, batchSize, offset );
            log.trace( "replicate {} to {} last: {}, size {}, batch {}, offset {}",
                master, slave, last, updates.size(), batchSize, offset );
            if( updates.isEmpty() ) break;
            newUpdates.addAll( updates );
        }
        log.trace( "updated objects {}", newUpdates.size() );

        val newObjects = new ArrayList<M>();
        val updatedObjects = new ArrayList<M>();

        for( M metadata : newUpdates ) {
            log.trace( "replicate {}", metadata );
            val id = slave.getIdFor( metadata );
            if( slave.putMetadata( id, metadata ) ) {
                updatedObjects.add( metadata );
            } else {
                newObjects.add( metadata );
            }
        }
        if( !newObjects.isEmpty() ) slave.fireUpdated( newObjects, true );
        if( !updatedObjects.isEmpty() ) slave.fireUpdated( updatedObjects, false );

        val ids = master.ids();
        log.trace( "master ids {}", ids );
        List<M> deletedObjects = Stream.of( slave.keys() )
            .filter( id -> !ids.contains( id ) )
            .flatMap( id -> Optionals.toStream( slave.deleteObject( id ) ) )
            .toList();
        log.trace( "deleted {}", deletedObjects );
        slave.fireDeleted( deletedObjects );

    }

    @Override
    public void close() {
        Scheduled.cancel( scheduled );
    }
}
