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
package oap.logstream.net;

import io.micrometer.core.instrument.Metrics;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.net.BufferConfigurationMap.BufferConfiguration;
import oap.util.Cuid;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import static dev.khbd.interp4j.core.Interpolations.s;

@ToString
@Slf4j
public class Buffers implements Closeable {

    public final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<LogId, Slot> currentBuffers = new ConcurrentHashMap<>();
    @ToString.Exclude
    private final ReentrantLock configurationLock = new ReentrantLock();
    /**
     * own copy, replaced as a whole on a runtime change (copy-on-write); iteration order = order of the given map
     */
    private volatile Map<String, BufferConfiguration> configurations;
    /**
     * incremented on a configuration change, a slot re-resolves its configuration when its generation differs
     */
    private volatile int generation;
    public BufferCache cache;
    ReadyQueue readyBuffers = new ReadyQueue();
    /**
     * max number of buffers (stripes) of a LogId, a power of 2
     */
    public final int stripes;
    private volatile boolean closed;

    public Buffers( BufferConfigurationMap configurations ) {
        this( configurations, 0 );
    }

    /**
     * @param stripes max number of concurrently filled buffers of a LogId, relative to the number of cores
     *                ( {@link Threads#count(double, int)}, 0 - the number of cores ), rounded up to a power of 2;
     *                a LogId starts with one buffer, the number grows ( up to stripes ) only when the producers contend
     *                for the buffer of the LogId
     */
    public Buffers( BufferConfigurationMap configurations, double stripes ) {
        int count = Threads.count( stripes, Runtime.getRuntime().availableProcessors() );
        this.stripes = count <= 1 ? 1 : Integer.highestOneBit( count - 1 ) << 1;
        log.debug( "stripes {} -> {}", stripes, this.stripes );

        this.configurations = Collections.unmodifiableMap( new LinkedHashMap<>( configurations ) );
        this.cache = new BufferCache();
        this.cache.retainSizes( sizes( this.configurations ) );
    }

    private static Set<Integer> sizes( Map<String, BufferConfiguration> configurations ) {
        HashSet<Integer> sizes = new HashSet<>();
        for( BufferConfiguration conf : configurations.values() ) sizes.add( conf.bufferSize );
        return sizes;
    }

    /**
     * configuration name -> buffer size
     */
    public Map<String, Integer> bufferSizes() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        configurations.forEach( ( name, conf ) -> result.put( name, conf.bufferSize ) );
        return result;
    }

    /**
     * Runtime change of the buffer size of a configuration. The next buffer of every LogId of this configuration is
     * created with the new size; the current non-empty buffer of a LogId is made ready (sent) on its next put.
     */
    public void setBufferSize( String name, int bufferSize ) {
        changeBufferSize( name, bufferSize );
    }

    /**
     * {@link #setBufferSize(String, int)} for all configurations
     */
    public void setBufferSize( int bufferSize ) {
        changeBufferSize( null, bufferSize );
    }

    private void changeBufferSize( String name, int bufferSize ) {
        if( bufferSize <= 0 ) throw new IllegalArgumentException( "bufferSize must be > 0, but " + bufferSize );

        configurationLock.lock();
        try {
            Map<String, BufferConfiguration> current = configurations;
            if( name != null && !current.containsKey( name ) )
                throw new IllegalArgumentException( "unknown buffer configuration '" + name + "', known " + current.keySet() );

            LinkedHashMap<String, BufferConfiguration> updated = new LinkedHashMap<>();
            current.forEach( ( n, conf ) -> {
                if( name == null || name.equals( n ) ) {
                    if( conf.bufferSize != bufferSize ) log.info( "buffer configuration '{}' bufferSize {} -> {}", n, conf.bufferSize, bufferSize );
                    updated.put( n, new BufferConfiguration( bufferSize, conf.pattern ) );
                } else {
                    updated.put( n, conf );
                }
            } );

            configurations = Collections.unmodifiableMap( updated );
            cache.retainSizes( sizes( updated ) );
            generation++;
        } finally {
            configurationLock.unlock();
        }
    }

    public final void put( LogId key, ProtocolVersion protocolVersion, byte[] buffer ) {
        put( key, protocolVersion, buffer, 0, buffer.length );
    }

    public final void put( LogId id, ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        // fast-fail without a lock
        if( closed ) throw new IllegalStateException( "current buffer is already closed" );

        // lock-free; identity match for a cached LogId
        Slot slot = currentBuffers.get( id );
        if( slot == null ) {
            slot = currentBuffers.computeIfAbsent( id, k -> {
                int slotGeneration = generation;
                return new Slot( findConfiguration( k ), Buffer.encodeHeader( k ), slotGeneration, stripes );
            } );
        }

        Stripe stripe = lock( slot );
        try {
            // under the stripe lock: close() sets closed before flush(), so no buffer is left in a slot after the last flush
            if( closed ) throw new IllegalStateException( "current buffer is already closed" );

            int currentGeneration = generation;
            if( stripe.generation != currentGeneration ) {
                stripe.conf = findConfiguration( id );
                stripe.generation = currentGeneration;
                Buffer current = stripe.buffer;
                if( current != null && current.capacity() != stripe.conf.bufferSize ) {
                    if( current.isEmpty() ) cache.release( current );
                    else readyBuffers.ready( current );
                    stripe.buffer = null;
                }
            }
            int bufferSize = stripe.conf.bufferSize;

            Buffer b = stripe.buffer;
            if( b == null ) {
                b = cache.get( id, protocolVersion, bufferSize, slot.header );
            }

            if( bufferSize - b.headerLength() < length )
                throw new IllegalArgumentException( s( "buffer size is too big: ${length} for buffer of ${bufferSize}; headers = ${b.headerLength()}" ) );

            if( !b.available( length ) ) {
                readyBuffers.ready( b );
                b = cache.get( id, protocolVersion, bufferSize, slot.header );
            }

            b.put( buffer, offset, length );
            stripe.buffer = b;
        } finally {
            stripe.lock.unlock();
        }
    }

    /**
     * the stripe of the current thread among the first slot.width stripes; a failed tryLock doubles the width
     * ( up to the number of stripes ), so only a LogId written concurrently gets more than one buffer
     */
    private static Stripe lock( Slot slot ) {
        Stripe[] stripes = slot.stripes;
        if( stripes.length == 1 ) {
            stripes[0].lock.lock();
            return stripes[0];
        }

        int probe = ( int ) Thread.currentThread().threadId();
        int width = slot.width;
        Stripe stripe = stripes[probe & ( width - 1 )];
        if( stripe.lock.tryLock() ) return stripe;

        if( width < stripes.length ) {
            Slot.WIDTH.compareAndSet( slot, width, width << 1 );
            stripe = stripes[probe & ( slot.width - 1 )];
        }
        stripe.lock.lock();
        return stripe;
    }

    private BufferConfiguration findConfiguration( LogId id ) {
        for( var conf : configurations.entrySet() ) {
            if( conf.getValue().pattern.matcher( id.logType ).find() ) return conf.getValue();
        }
        throw new IllegalStateException( "Pattern for " + id + " not found" );
    }

    public void flush() {
        for( Slot slot : currentBuffers.values() ) {
            for( Stripe stripe : slot.stripes ) {
                stripe.lock.lock();
                try {
                    if( stripe.buffer != null && !stripe.buffer.isEmpty() ) {
                        readyBuffers.ready( stripe.buffer );
                        stripe.buffer = null;
                    }
                } finally {
                    stripe.lock.unlock();
                }
            }
        }
    }

    public final boolean isEmpty() {
        return readyBuffers.isEmpty();
    }

    @Override
    public final void close() {
        lock.lock();
        try {
            if( closed ) throw new IllegalStateException( "already closed" );
            // before flush(): a put that passed the check under the slot lock is flushed, a later one fails
            closed = true;
            flush();
        } finally {
            lock.unlock();
        }
    }

    /**
     * sequential {@link #forEachReadyData(Executor, int, ObjIntConsumer)}
     */
    public final void forEachReadyData( Consumer<Buffer> consumer ) {
        forEachReadyData( Runnable::run, 1, ( buffer, worker ) -> consumer.accept( buffer ) );
    }

    /**
     * Flushes the current buffers and passes every ready buffer to the consumer, in parallel by up to workers threads:
     * worker 0 is the calling thread, the others run in the executor. A consumed buffer is removed and returned to the pool.
     * After the first exception of the consumer the workers stop taking buffers; the failed buffer and the not consumed ones
     * stay ready ( in their order ) for the next call, the exception is rethrown.
     *
     * @param consumer ( buffer, worker index 0..workers-1 ); a worker calls it sequentially - per-worker state needs no synchronization
     */
    public final void forEachReadyData( Executor executor, int workers, ObjIntConsumer<Buffer> consumer ) {
        if( workers <= 0 ) throw new IllegalArgumentException( s( "workers must be > 0, but ${workers}" ) );

        lock.lock();
        try {
            flush();
            report();

            // single consumer under lock, ready() only appends to the tail -> the first n buffers are stable
            int n = readyBuffers.size();
            log.trace( "buffers to go {}", n );
            if( n == 0 ) return;

            Batch batch = new Batch( new Buffer[n], consumer );
            Iterator<Buffer> iterator = readyBuffers.buffers.iterator();
            for( int i = 0; i < n; i++ ) batch.buffers[i] = iterator.next();

            int parallelism = Math.min( workers, n );
            CompletableFuture<?>[] futures = new CompletableFuture<?>[parallelism - 1];
            for( int w = 1; w < parallelism; w++ ) {
                int worker = w;
                try {
                    futures[w - 1] = CompletableFuture.runAsync( () -> batch.run( worker ), executor );
                } catch( RejectedExecutionException e ) {
                    // the buffers of this worker are taken by the others
                    futures[w - 1] = CompletableFuture.completedFuture( null );
                }
            }
            batch.run( 0 );
            CompletableFuture.allOf( futures ).join();

            // remove the consumed buffers, the failed / not consumed ones keep their position
            iterator = readyBuffers.buffers.iterator();
            for( int i = 0; i < n; i++ ) {
                Buffer buffer = iterator.next();
                if( batch.consumed[i] ) {
                    iterator.remove();
                    readyBuffers.size.decrementAndGet();
                    cache.release( buffer );
                }
            }

            Throwable error = batch.error;
            if( error instanceof RuntimeException e ) throw e;
            if( error instanceof Error e ) throw e;
            if( error != null ) throw new IllegalStateException( error );
        } finally {
            lock.unlock();
        }
    }

    public void report() {
        ArrayList<String> ready = new ArrayList<>();
        for( Buffer buffer : readyBuffers.buffers ) ready.add( buffer.id.logType );
        report( ready, "true" );

        ArrayList<String> current = new ArrayList<>();
        for( Slot slot : currentBuffers.values() ) {
            for( Stripe stripe : slot.stripes ) {
                stripe.lock.lock();
                try {
                    Buffer buffer = stripe.buffer;
                    if( buffer != null ) current.add( buffer.id.logType );
                } finally {
                    stripe.lock.unlock();
                }
            }
        }
        report( current, "false" );
    }

    private void report( Collection<String> logTypes, String ready ) {
        HashMap<String, MutableLong> map = new HashMap<String, MutableLong>();
        for( String logType : logTypes ) {
            map.computeIfAbsent( logType, lt -> new MutableLong() ).increment();
        }

        map.forEach( ( type, count ) -> Metrics.summary( "logstream_logging_buffers", "type", type, "ready", ready ).record( count.getValue() ) );
    }

    public final int readyBuffers() {
        return readyBuffers.size();
    }

    /**
     * serialized buffer header + the stripes ( current buffers ) of a LogId
     */
    @ToString( exclude = { "header" } )
    private static final class Slot {
        static final AtomicIntegerFieldUpdater<Slot> WIDTH = AtomicIntegerFieldUpdater.newUpdater( Slot.class, "width" );

        final byte[] header;
        final Stripe[] stripes;
        /**
         * number of the stripes in use, a power of 2; grows on contention
         */
        volatile int width = 1;

        Slot( BufferConfiguration conf, byte[] header, int generation, int stripes ) {
            this.header = header;
            this.stripes = new Stripe[stripes];
            for( int i = 0; i < stripes; i++ ) this.stripes[i] = new Stripe( conf, generation );
        }
    }

    /**
     * buffer configuration + current buffer; mutated only under Stripe.lock
     */
    @ToString( exclude = { "lock" } )
    private static final class Stripe {
        final ReentrantLock lock = new ReentrantLock();
        BufferConfiguration conf;
        int generation;
        Buffer buffer;

        Stripe( BufferConfiguration conf, int generation ) {
            this.conf = conf;
            this.generation = generation;
        }
    }

    /**
     * the ready buffers of one forEachReadyData call, the workers take them by index
     */
    private static final class Batch {
        final Buffer[] buffers;
        /**
         * written by the workers, read by the caller after CompletableFuture.join() ( happens-before )
         */
        final boolean[] consumed;
        final ObjIntConsumer<Buffer> consumer;
        final AtomicInteger next = new AtomicInteger();
        volatile Throwable error;

        Batch( Buffer[] buffers, ObjIntConsumer<Buffer> consumer ) {
            this.buffers = buffers;
            this.consumed = new boolean[buffers.length];
            this.consumer = consumer;
        }

        void run( int worker ) {
            int i;
            while( error == null && ( i = next.getAndIncrement() ) < buffers.length ) {
                try {
                    consumer.accept( buffers[i], worker );
                    consumed[i] = true;
                } catch( Throwable e ) {
                    synchronized( this ) {
                        if( error == null ) error = e;
                        else error.addSuppressed( e );
                    }
                }
            }
        }
    }

    public static class BufferCache {
        private final ReentrantLock lock = new ReentrantLock();

        private final HashMap<Integer, Queue<Buffer>> cache = new HashMap<>();
        /**
         * configured buffer sizes; a released buffer of another size is dropped
         */
        private Set<Integer> sizes = Set.of();

        private Buffer get( LogId id, ProtocolVersion protocolVersion, int bufferSize, byte[] header ) {
            lock.lock();
            try {
                Queue<Buffer> list = cache.computeIfAbsent( bufferSize, bs -> new ArrayDeque<>() );

                if( list.isEmpty() ) {
                    return new Buffer( bufferSize, id, protocolVersion, header );
                } else {
                    Buffer buffer = list.poll();
                    buffer.reset( id, header );
                    return buffer;
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * pooled by capacity (the configured size); a buffer of a size no longer configured is dropped
         */
        private void release( Buffer buffer ) {
            lock.lock();
            try {
                int capacity = buffer.capacity();
                if( sizes.contains( capacity ) ) cache.computeIfAbsent( capacity, bs -> new ArrayDeque<>() ).offer( buffer );
            } finally {
                lock.unlock();
            }
        }

        private void retainSizes( Set<Integer> sizes ) {
            lock.lock();
            try {
                this.sizes = Set.copyOf( sizes );
                cache.keySet().retainAll( this.sizes );
            } finally {
                lock.unlock();
            }
        }

        public final int size( int bufferSize ) {
            lock.lock();
            try {
                Queue<Buffer> list = cache.get( bufferSize );
                return list != null ? list.size() : 0;
            } finally {
                lock.unlock();
            }
        }

        /**
         * number of pooled buffers of all sizes
         */
        public final int size() {
            lock.lock();
            try {
                int size = 0;
                for( Queue<Buffer> list : cache.values() ) size += list.size();
                return size;
            } finally {
                lock.unlock();
            }
        }
    }

    static class ReadyQueue implements Serializable {
        static Cuid digestionIds = Cuid.UNIQUE;

        private final ReentrantLock lock = new ReentrantLock();
        private final ConcurrentLinkedQueue<Buffer> buffers = new ConcurrentLinkedQueue<>();
        /**
         * ConcurrentLinkedQueue.size() is O(n)
         */
        private final AtomicInteger size = new AtomicInteger();

        public final void ready( Buffer buffer ) {
            lock.lock();
            try {
                buffer.close( digestionIds.nextLong() );
                buffers.offer( buffer );
                size.incrementAndGet();
            } finally {
                lock.unlock();
            }
        }

        public final Buffer peek() {
            return buffers.peek();
        }

        public final Buffer poll() {
            Buffer buffer = buffers.poll();
            if( buffer != null ) size.decrementAndGet();
            return buffer;
        }

        public final int size() {
            return size.get();
        }

        public final boolean isEmpty() {
            return buffers.isEmpty();
        }
    }
}
