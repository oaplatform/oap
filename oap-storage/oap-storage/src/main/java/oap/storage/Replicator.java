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

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.storage.Storage.DataListener.IdObject;
import oap.util.Cuid;
import oap.util.Lists;
import oap.util.Pair;

import java.io.Closeable;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;
import static oap.storage.Storage.DataListener.IdObject.__io;
import static oap.util.Pair.__;

/**
 * Replicator works on the MemoryStorage internals. It's intentional.
 *
 * @param <T>
 */
@Slf4j
public class Replicator<I, T> implements Closeable {
    static final AtomicLong stored = new AtomicLong();
    static final AtomicLong deleted = new AtomicLong();
    private final MemoryStorage<I, T> slave;
    private final ReplicationMaster<I, T> master;
    private String uniqueName = Cuid.UNIQUE.next();
    private Scheduled scheduled;
    private transient Pair<Long, String> lastModified = __( -1L, "" );

    public Replicator( MemoryStorage<I, T> slave, ReplicationMaster<I, T> master, long interval ) {
        this.slave = slave;
        this.master = master;
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), interval, i -> {
            var newLastModified = replicate( lastModified );
            log.trace( "[{}] newLastModified = {}, lastModified = {}", uniqueName, newLastModified, lastModified );
            if( newLastModified._2.equals( lastModified._2 ) ) {
                lastModified = newLastModified.map( ( t, m ) -> __( t + 1, m ) );
            } else {
                lastModified = newLastModified;
            }
        } );
    }

    public static void reset() {
        stored.set( 0 );
        deleted.set( 0 );
    }

    public void replicateNow() {
        log.trace( "[{}] forcing replication...", uniqueName );
        scheduled.triggerNow();
    }

    public void replicateAllNow() {
        lastModified = __( -1L, "" );
        replicateNow();
    }

    public synchronized Pair<Long, String> replicate( Pair<Long, String> last ) {
        log.trace( "replicate service {} last {}", uniqueName, last );

        List<Metadata<T>> newUpdates;

        try( var updates = master.updatedSince( last._1 ) ) {
            log.trace( "[{}] replicate {} to {} last: {}", master, slave, last, uniqueName );
            newUpdates = updates.collect( toList() );
            log.trace( "[{}] updated objects {}", uniqueName, newUpdates.size() );
        } catch( UncheckedIOException e ) {
            log.error( e.getCause().getMessage() );
            return last;
        } catch( Exception e ) {
            if( e.getCause() instanceof SocketException ) {
                log.error( e.getCause().getMessage() );
                return last;
            }
            throw e;
        }

        var added = new ArrayList<IdObject<I, T>>();
        var updated = new ArrayList<IdObject<I, T>>();

        var lastUpdate = newUpdates.stream().mapToLong( m -> m.modified ).max().orElse( last._1 );

        var hasher = Hashing.murmur3_128().newHasher();

        var finalLastUpdate = lastUpdate;
        var list = newUpdates
            .stream()
            .filter( metadata -> metadata.modified == finalLastUpdate )
            .map( metadata -> slave.identifier.get( metadata.object ).toString() )
            .sorted()
            .toList();

        for( var id : list ) {
            hasher = hasher.putUnencodedChars( id );
        }
        var hash = hasher.hash().toString();


        if( lastUpdate != last._1 || !hash.equals( last._2 ) ) {
            for( var metadata : newUpdates ) {
                log.trace( "[{}] replicate {}", metadata, uniqueName );

                var id = slave.identifier.get( metadata.object );
                var unmodified = slave.memory.get( id ).map( m -> m.looksUnmodified( metadata ) ).orElse( false );
                if( unmodified ) {
                    log.trace( "[{}] skipping unmodified {}", uniqueName, id );
                    continue;
                }
                if( slave.memory.put( id, Metadata.from( metadata ) ) ) added.add( __io( id, metadata.object ) );
                else updated.add( __io( id, metadata.object ) );
            }
            slave.fireAdded( added );
            slave.fireUpdated( updated );

            stored.addAndGet( newUpdates.size() );
        }

        log.trace( "[{}] added {} updated {}", uniqueName, Lists.map( added, a -> a.id ), Lists.map( updated, a -> a.id ) );

        var ids = master.ids();
        log.trace( "[{}] master ids {}", uniqueName, ids );
        if( ids.isEmpty() ) {
            lastUpdate = -1;
        }

        List<IdObject<I, T>> deleted = slave.memory.selectLiveIds()
            .filter( id -> !ids.contains( id ) )
            .map( id -> slave.memory.removePermanently( id ).map( m -> __io( id, m.object ) ) )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .toList();
        log.trace( "[{}] deleted {}", uniqueName, deleted );
        slave.fireDeleted( deleted );
        if( !added.isEmpty() || !updated.isEmpty() || !deleted.isEmpty() ) {
            slave.fireChanged( added, updated, deleted );
        }

        Replicator.deleted.addAndGet( deleted.size() );

        return __( lastUpdate, hash );
    }

    public void preStop() {
        Scheduled.cancel( scheduled );
        scheduled = null;
    }

    @Override
    public void close() {
        try {
            Scheduled.cancel( scheduled );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
    }
}
