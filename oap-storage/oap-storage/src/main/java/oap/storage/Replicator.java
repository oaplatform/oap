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

package oap.storage;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.storage.Storage.DataListener.IdObject;
import oap.util.Cuid;

import java.io.Closeable;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.util.ArrayList;

import static oap.storage.Storage.DataListener.IdObject.__io;
import static oap.storage.TransactionLog.ReplicationResult.ReplicationStatusType.FULL_SYNC;

/**
 * Replicator works on the MemoryStorage internals. It's intentional.
 *
 * @param <T>
 */
@Slf4j
public class Replicator<I, T> implements Closeable {
    private final MemoryStorage<I, T> slave;
    private final ReplicationMaster<I, T> master;
    private String uniqueName = Cuid.UNIQUE.next();
    private Scheduled scheduled;
    private transient long timestamp = -1L;

    public Replicator( MemoryStorage<I, T> slave, ReplicationMaster<I, T> master, long interval ) {

        Preconditions.checkArgument( slave.transactionLog instanceof TransactionLogZero );

        this.slave = slave;
        this.master = master;
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), interval, i -> {
            long newTimestamp = replicate( timestamp );
            log.trace( "[{}] newTimestamp {}, lastModified {}", uniqueName, newTimestamp, timestamp );

            timestamp = newTimestamp;
        } );
    }

    public void replicateNow() {
        log.trace( "[{}] forcing replication...", uniqueName );
        scheduled.triggerNow();
    }

    public void replicateAllNow() {
        timestamp = -1L;
        replicateNow();
    }

    public synchronized long replicate( long timestamp ) {
        log.trace( "replicate service {} timestamp {}", uniqueName, timestamp );

        TransactionLog.ReplicationResult<I, Metadata<T>> updatedSince;

        try {
            log.trace( "[{}] replicate {} to {} timestamp: {}", master, slave, timestamp, uniqueName );
            updatedSince = master.updatedSince( timestamp );
            log.trace( "[{}] type {} updated objects {}", uniqueName, updatedSince.type, updatedSince.data.size() );
        } catch( UncheckedIOException e ) {
            log.error( e.getCause().getMessage() );
            return timestamp;
        } catch( Exception e ) {
            if( e.getCause() instanceof SocketException ) {
                log.error( e.getCause().getMessage() );
                return timestamp;
            }
            throw e;
        }

        ArrayList<IdObject<I, T>> added = new ArrayList<>();
        ArrayList<IdObject<I, T>> updated = new ArrayList<>();
        ArrayList<IdObject<I, T>> deleted = new ArrayList<>();

        long lastUpdate = updatedSince.timestamp;

        if( updatedSince.type == FULL_SYNC ) {
            slave.memory.removePermanently();
        }

        for( TransactionLog.Transaction<I, Metadata<T>> transaction : updatedSince.data ) {
            log.trace( "[{}] replicate {}", transaction, uniqueName );

            Metadata<T> metadata = transaction.object;
            I id = transaction.id;

            switch( transaction.operation ) {
                case INSERT -> {
                    added.add( __io( id, metadata ) );
                    slave.memory.put( id, metadata );
                }
                case UPDATE -> {
                    if( metadata.isDeleted() ) {
                        deleted.add( __io( id, metadata ) );
                        slave.memory.removePermanently( id );
                    } else {
                        if( updatedSince.type == FULL_SYNC ) {
                            added.add( __io( id, metadata ) );
                        } else {
                            updated.add( __io( id, metadata ) );
                        }
                        slave.memory.put( id, metadata );
                    }
                }
                case DELETE -> {
                    deleted.add( __io( id, metadata ) );
                    slave.memory.removePermanently( id );
                }
            }
        }

        if( !added.isEmpty() || !updated.isEmpty() || !deleted.isEmpty() ) {
            slave.fireChanged( added, updated, deleted );
        }

        return lastUpdate;
    }

    public void preStop() {
        try {
            Scheduled.cancel( scheduled );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
    }

    @Override
    public void close() {
        Closeables.close( scheduled );
    }
}
