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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.ServiceName;
import oap.concurrent.scheduler.ScheduledExecutorService;
import oap.io.Closeables;
import oap.util.Dates;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static oap.concurrent.Threads.synchronizedOn;

@Slf4j
@ToString( of = { "tableName", "delay", "batchSize", "watch", "serviceName" } )
public abstract class AbstractPersistance<I, T> implements Closeable, AutoCloseable {

    public static final Path DEFAULT_CRASH_DUMP_PATH = Path.of( "/tmp/mongo-persistance-crash-dump" );
    public static final DateTimeFormatter CRASH_DUMP_PATH_FORMAT_MILLIS = DateTimeFormat
        .forPattern( "yyyy-MM-dd-HH-mm-ss-SSS" )
        .withZoneUTC();

    protected final Lock lock = new ReentrantLock();
    protected final MemoryStorage<I, T> storage;
    protected final String tableName;
    protected final long delay;
    protected final Path crashDumpPath;
    @ServiceName
    public String serviceName;
    public boolean watch = false;
    protected int batchSize = 100;
    protected final ExecutorService watchExecutor = Executors.newSingleThreadExecutor();
    protected final ScheduledExecutorService scheduler = oap.concurrent.Executors.newScheduledThreadPool( 1, serviceName );
    protected volatile long lastExecuted = -1;
    protected volatile boolean stopped = false;

    public AbstractPersistance( MemoryStorage<I, T> storage, String tableName, long delay, Path crashDumpPath ) {
        this.storage = storage;
        this.tableName = tableName;
        this.delay = delay;
        this.crashDumpPath = crashDumpPath.resolve( tableName );
    }

    public void preStart() {
        log.info( "collection = {}, fsync delay = {}, watch = {}, crashDumpPath = {}",
            tableName, Dates.durationToString( delay ), watch, crashDumpPath );

        synchronizedOn( lock, () -> {
            this.load();
            scheduler.scheduleWithFixedDelay( this::fsync, delay, delay, TimeUnit.MILLISECONDS );
        } );

        if( watch ) {
            CountDownLatch cdl = new CountDownLatch( 1 );
            watchExecutor.execute( () -> {
                if ( stopped ) return;
                processRecords( cdl );
            } );
            try {
                if ( !cdl.await( 1, TimeUnit.MINUTES ) ) {
                    log.error( "Could not process records within 1 min timeout" );
                }
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            }
        }
    }

    protected Optional<T> deleteById( String id ) {
        return storage.delete( storage.identifier.fromString( id ) );
    }

    protected abstract void load();

    protected abstract void processRecords( CountDownLatch cdl );

    @Override
    public void close() {
        log.debug( "closing {}...", this );
        synchronizedOn( lock, () -> {
            scheduler.shutdown( 5, TimeUnit.SECONDS );
            Closeables.close( scheduler ); // no more sync after that
            if( storage != null ) {
                fsync();
                log.debug( "closed {}...", this );
            } else log.debug( "this {} wasn't started or already closed", this );
            Closeables.close( watchExecutor );
            stopped = true;
            log.debug( "closed {}...", this );
        } );
    }

    public abstract void fsync();
}
