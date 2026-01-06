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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.storage.Storage.DataListener.IdObject;
import oap.util.Cuid;
import oap.util.Pair;

import java.io.Closeable;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static oap.storage.Storage.DataListener.IdObject.__io;
import static oap.storage.TransactionLog.ReplicationResult.ReplicationStatusType.CHANGES;
import static oap.storage.TransactionLog.ReplicationResult.ReplicationStatusType.FULL_SYNC;
import static oap.util.Pair.__;

/**
 * Replicator works on the MemoryStorage internals. It's intentional.
 *
 * @param <T>
 */
@Slf4j
public class Replicator<I, T> implements Closeable {
    final AtomicLong replicatorCounterFullSync = new AtomicLong();
    final AtomicLong replicatorCounterPartialSync = new AtomicLong();
    final AtomicLong replicatorSizeFullSync = new AtomicLong();
    final AtomicLong replicatorSizePartialSync = new AtomicLong();
    final ReentrantLock lock = new ReentrantLock();
    private final MemoryStorage<I, T> slave;
    private final ReplicationMaster<I, T> master;
    private final Scheduled scheduled;
    private String uniqueName = Cuid.UNIQUE.next();
    private transient long timestamp = -1L;
    private transient long hash = -1L;

    public Replicator( MemoryStorage<I, T> slave, ReplicationMaster<I, T> master, long interval ) {
        Preconditions.checkArgument( slave.transactionLog instanceof TransactionLogZero );

        this.slave = slave;
        this.master = master;

        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), interval, i -> {
            Pair<Long, Long> newTimestamp = replicate( timestamp );
            log.trace( "[{}] newTimestamp {}, lastModified {}", uniqueName, newTimestamp, timestamp );

            timestamp = newTimestamp._1;
            hash = newTimestamp._2;
        } );
    }

    public void start() {
        Metrics.gauge( "replicator", Tags.of( "name", uniqueName, "type", FULL_SYNC.name() ), this, _ -> replicatorCounterFullSync.doubleValue() );
        Metrics.gauge( "replicator", Tags.of( "name", uniqueName, "type", CHANGES.name() ), this, _ -> replicatorCounterPartialSync.doubleValue() );

        Metrics.gauge( "replicator_size", Tags.of( "name", uniqueName, "type", FULL_SYNC.name() ), this, _ -> replicatorSizeFullSync.doubleValue() );
        Metrics.gauge( "replicator_size", Tags.of( "name", uniqueName, "type", CHANGES.name() ), this, _ -> replicatorSizePartialSync.doubleValue() );
    }

    public void replicateNow() {
        log.trace( "[{}] forcing replication...", uniqueName );
        scheduled.triggerNow();
    }

    public void replicateAllNow() {
        timestamp = -1L;
        replicateNow();
    }

    public Pair<Long, Long> replicate( long timestamp ) {
        lock.lock();
        try {
            log.trace( "replicate service {} timestamp {} hash {}", uniqueName, timestamp, hash );

            try {
                log.trace( "[{}] replicate {} to {} timestamp {} hash {}", uniqueName, master, slave, timestamp, hash );
                TransactionLog.ReplicationResult<I, Metadata<T>> updatedSince = master.updatedSince( timestamp, hash );
                log.trace( "[{}] type {} updated objects {}", uniqueName, updatedSince.type, updatedSince.data.size() );

                switch( updatedSince.type ) {
                    case FULL_SYNC -> {
                        replicatorCounterFullSync.incrementAndGet();
                        replicatorSizeFullSync.addAndGet( updatedSince.data.size() );
                    }
                    case CHANGES -> {
                        replicatorCounterPartialSync.incrementAndGet();
                        replicatorSizePartialSync.addAndGet( updatedSince.data.size() );
                    }
                    default -> throw new IllegalStateException( "Unknown replicator type: " + updatedSince.type );
                }

                ArrayList<IdObject<I, T>> added = new ArrayList<>();
                ArrayList<IdObject<I, T>> updated = new ArrayList<>();
                ArrayList<IdObject<I, T>> deleted = new ArrayList<>();

                long lastUpdate = updatedSince.timestamp;

                if( updatedSince.type == FULL_SYNC ) {
                    slave.memory.clear();
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
                            if( updatedSince.type == FULL_SYNC ) {
                                added.add( __io( id, metadata ) );
                            } else {
                                updated.add( __io( id, metadata ) );
                            }
                            slave.memory.put( id, metadata );
                        }
                        case DELETE -> {
                            deleted.add( __io( id, metadata ) );
                            slave.memory.delete( id );
                        }
                    }
                }

                if( !added.isEmpty() || !updated.isEmpty() || !deleted.isEmpty() ) {
                    slave.fireChanged( added, updated, deleted );
                }

                return __( lastUpdate, updatedSince.hash );
            } catch( UncheckedIOException e ) {
                log.error( e.getCause().getMessage() );
                return __( timestamp, hash );
            } catch( Exception e ) {
                if( e.getCause() instanceof SocketException ) {
                    log.error( e.getCause().getMessage() );
                    return __( timestamp, hash );
                }
                throw e;
            }
        } finally {
            lock.unlock();
        }
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
