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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.net.BufferConfigurationMap.BufferConfiguration;
import oap.util.Cuid;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@EqualsAndHashCode( exclude = { "closed", "generation", "configurationLock" } )
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
    private volatile boolean closed;

    public Buffers( BufferConfigurationMap configurations ) {
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

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    public final void put( LogId id, ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "current buffer is already closed" );

        currentBuffers.compute( id, ( _, slot ) -> {
            int currentGeneration = generation;
            if( slot == null ) {
                slot = new Slot( findConfiguration( id ), Buffer.encodeHeader( id ), currentGeneration );
            } else if( slot.generation != currentGeneration ) {
                slot.conf = findConfiguration( id );
                slot.generation = currentGeneration;
                Buffer current = slot.buffer;
                if( current != null && current.capacity() != slot.conf.bufferSize ) {
                    if( current.isEmpty() ) cache.release( current );
                    else readyBuffers.ready( current );
                    slot.buffer = null;
                }
            }
            int bufferSize = slot.conf.bufferSize;

            Buffer b = slot.buffer;
            if( b == null ) {
                b = cache.get( id, protocolVersion, bufferSize, slot.header );
            }

            if( bufferSize - b.headerLength() < length )
                throw new IllegalArgumentException( "buffer size is too big: " + length + " for buffer of " + bufferSize + "; headers = " + b.headerLength() );

            if( !b.available( length ) ) {
                readyBuffers.ready( b );
                b = cache.get( id, protocolVersion, bufferSize, slot.header );
            }

            b.put( buffer, offset, length );
            slot.buffer = b;

            return slot;
        } );
    }

    private BufferConfiguration findConfiguration( LogId id ) {
        for( var conf : configurations.entrySet() ) {
            if( conf.getValue().pattern.matcher( id.logType ).find() ) return conf.getValue();
        }
        throw new IllegalStateException( "Pattern for " + id + " not found" );
    }

    public void flush() {
        for( LogId id : currentBuffers.keySet() ) {
            currentBuffers.computeIfPresent( id, ( _, slot ) -> {
                if( slot.buffer != null && !slot.buffer.isEmpty() ) {
                    readyBuffers.ready( slot.buffer );
                    slot.buffer = null;
                }
                return slot;
            } );
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
            flush();
            closed = true;
        } finally {
            lock.unlock();
        }
    }

    public final void forEachReadyData( Consumer<Buffer> consumer ) {
        lock.lock();
        try {
            flush();
            report();
            log.trace( "buffers to go {}", readyBuffers.size() );
            // single consumer under lock, ready() only appends to the tail -> poll() removes the peeked buffer
            Buffer buffer;
            while( ( buffer = readyBuffers.peek() ) != null ) {
                consumer.accept( buffer );
                readyBuffers.poll();
                cache.release( buffer );
            }
        } finally {
            lock.unlock();
        }
    }

    public void report() {
        report( readyBuffers.buffers, "true" );
        ArrayList<Buffer> current = new ArrayList<>();
        for( Slot slot : currentBuffers.values() ) {
            Buffer buffer = slot.buffer;
            if( buffer != null ) current.add( buffer );
        }
        report( current, "false" );
    }

    private void report( Collection<Buffer> in, String ready ) {
        ArrayList<Buffer> buffers = new ArrayList<>( in );

        HashMap<String, MutableLong> map = new HashMap<String, MutableLong>();
        for( Buffer buffer : buffers ) {
            String logType = buffer.id.logType;
            map.computeIfAbsent( logType, lt -> new MutableLong() ).increment();
        }

        map.forEach( ( type, count ) -> Metrics.summary( "logstream_logging_buffers", "type", type, "ready", ready ).record( count.getValue() ) );
    }

    public final int readyBuffers() {
        return readyBuffers.size();
    }

    /**
     * buffer configuration, serialized buffer header + current buffer for a LogId; mutated only inside currentBuffers.compute*
     */
    @ToString( exclude = "header" )
    @EqualsAndHashCode
    private static final class Slot {
        final byte[] header;
        BufferConfiguration conf;
        int generation;
        Buffer buffer;

        Slot( BufferConfiguration conf, byte[] header, int generation ) {
            this.conf = conf;
            this.header = header;
            this.generation = generation;
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
                Queue<Buffer> list = cache.computeIfAbsent( bufferSize, bs -> new LinkedList<>() );

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
                if( sizes.contains( capacity ) ) cache.computeIfAbsent( capacity, bs -> new LinkedList<>() ).offer( buffer );
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
