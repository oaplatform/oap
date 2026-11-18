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

package oap.logstream.disk;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * The shards of one (LogId, codec) key: up to {@code maxShards} {@link RowBinaryWriter}s, each with its own lock and
 * file ({@code -s<k>} suffix, shard 0 has none). A write takes a free shard of the active ones (round-robin) and waits
 * only if all of them are busy; the number of active shards is adjusted once per window by {@link ShardController}.
 * A shard taken out of use gets no new writes, its file is closed on the next rotation.
 */
@Slf4j
public class WriterShards implements Closeable {
    private static final Timer LOCK_WAIT = Metrics.timer( "logstream_logging_disk_lock_wait" );
    private static final Counter GROW = Metrics.counter( "logstream_logging_disk_shard_changes", "direction", "grow" );
    private static final Counter SHRINK = Metrics.counter( "logstream_logging_disk_shard_changes", "direction", "shrink" );

    private final String name;
    private final AtomicReferenceArray<RowBinaryWriter> shards;
    private final IntFunction<RowBinaryWriter> factory;
    private final SyncMode syncMode;
    private final AtomicInteger active = new AtomicInteger( 1 );
    private final AtomicInteger nextShard = new AtomicInteger();
    private final LongAdder windowAppends = new LongAdder();
    private final LongAdder windowWaitNanos = new LongAdder();
    private final AtomicInteger held = new AtomicInteger();
    private final LongAccumulator windowPeak = new LongAccumulator( Math::max, 0 );
    /**
     * guarded by this
     */
    private final ShardController controller;
    /**
     * guarded by this
     */
    private long windowStart = System.nanoTime();
    /**
     * guarded by shards
     */
    private boolean closed;

    /**
     * @param factory shard index -&gt; its writer; shard 0 is created at once, the others when first used
     */
    public WriterShards( String name, int maxShards, SyncMode syncMode, ShardController controller, IntFunction<RowBinaryWriter> factory ) {
        Preconditions.checkArgument( maxShards >= 1, "maxShards must be positive: %s", maxShards );
        this.name = name;
        this.shards = new AtomicReferenceArray<>( maxShards );
        this.syncMode = syncMode;
        this.controller = controller;
        this.factory = factory;
        shard( 0 );
    }

    /**
     * @return the file the buffer is written to
     */
    public String write( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        RowBinaryWriter shard = acquire();
        RowBinaryWriter.Pending pending;
        windowPeak.accumulate( held.incrementAndGet() );
        try {
            pending = shard.append( protocolVersion, buffer, offset, length );
        } finally {
            held.decrementAndGet();
            shard.lock.unlock();
            windowAppends.increment();
        }
        // outside the shard lock: group commit
        if( syncMode == SyncMode.ALWAYS ) shard.commit( pending );
        return pending.filename();
    }

    /**
     * @return a shard with its lock taken: a free one of the active shards, otherwise waits for the next one in turn
     */
    private RowBinaryWriter acquire() {
        int n = Math.min( active.get(), shards.length() );
        int first = n == 1 ? 0 : Math.floorMod( nextShard.getAndIncrement(), n );
        for( int i = 0; i < n; i++ ) {
            RowBinaryWriter shard = shard( ( first + i ) % n );
            if( shard.lock.tryLock() ) {
                LOCK_WAIT.record( 0, NANOSECONDS );
                return shard;
            }
        }

        RowBinaryWriter shard = shard( first );
        long start = System.nanoTime();
        shard.lock.lock();
        long waited = System.nanoTime() - start;
        windowWaitNanos.add( waited );
        LOCK_WAIT.record( waited, NANOSECONDS );
        return shard;
    }

    private RowBinaryWriter shard( int k ) {
        RowBinaryWriter shard = shards.get( k );
        if( shard != null ) return shard;

        synchronized( shards ) {
            if( closed ) throw new LoggerException( name + " is closed" );
            shard = shards.get( k );
            if( shard == null ) {
                shard = factory.apply( k );
                shards.set( k, shard );
            }
            return shard;
        }
    }

    /**
     * the writer of the shard k, null - not created yet
     */
    @Nullable
    RowBinaryWriter shardWriter( int k ) {
        return shards.get( k );
    }

    /**
     * Ends the statistics window: the active shard count for the next one, see {@link ShardController}.
     * Called once per window ({@link DiskLoggerBackend#refreshPeriod}).
     */
    public synchronized void adjust() {
        long now = System.nanoTime();
        int peak = ( int ) windowPeak.getThenReset();
        // a shard held across the boundary
        windowPeak.accumulate( held.get() );
        ShardController.Window window = new ShardController.Window( windowAppends.sumThenReset(), windowWaitNanos.sumThenReset(), peak, now - windowStart );
        windowStart = now;

        int current = active.get();
        int next = controller.next( current, window );
        if( next != current ) {
            active.set( next );
            ( next > current ? GROW : SHRINK ).increment();
            log.info( "{}: shards {} -> {} ({})", name, current, next, window );
        }
    }

    public int activeShards() {
        return active.get();
    }

    public void refresh( boolean forceSync ) {
        forEach( "refresh", shard -> shard.refresh( forceSync ) );
    }

    /**
     * fdatasync of the data written without sync ({@link SyncMode#PERIODIC}, {@link SyncMode#ON_CLOSE})
     */
    public void sync() {
        forEach( "sync", AbstractWriter::sync );
    }

    @Override
    public void close() {
        synchronized( shards ) {
            closed = true;
        }
        forEach( "close", AbstractWriter::close );
    }

    private void forEach( String action, Consumer<RowBinaryWriter> consumer ) {
        for( int i = 0; i < shards.length(); i++ ) {
            RowBinaryWriter shard = shards.get( i );
            if( shard == null ) continue;
            try {
                consumer.accept( shard );
            } catch( Exception e ) {
                log.error( "Cannot {} {}", action, shard, e );
            }
        }
    }

    /**
     * the files open for writing in all the shards
     */
    public Set<Path> currentFiles() {
        Set<Path> files = new LinkedHashSet<>();
        for( int i = 0; i < shards.length(); i++ ) {
            RowBinaryWriter shard = shards.get( i );
            if( shard == null ) continue;
            Path file = shard.currentFile();
            if( file != null ) files.add( file );
        }
        return files;
    }

    /**
     * the file open for writing in shard 0, null - none
     */
    @Nullable
    public LogFile getLogFile() {
        return shards.get( 0 ).getLogFile();
    }

    /**
     * the files open for writing in all the shards
     */
    public List<LogFile> getLogFiles() {
        List<LogFile> files = new ArrayList<>();
        for( int i = 0; i < shards.length(); i++ ) {
            RowBinaryWriter shard = shards.get( i );
            if( shard == null ) continue;
            LogFile logFile = shard.getLogFile();
            if( logFile != null ) files.add( logFile );
        }
        return files;
    }

    /**
     * the current file pattern of shard 0
     */
    public String currentPattern() {
        return shards.get( 0 ).currentPattern();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + name + "[" + active.get() + "/" + shards.length() + "]";
    }
}
