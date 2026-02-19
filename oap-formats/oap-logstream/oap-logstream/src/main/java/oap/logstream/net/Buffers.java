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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@EqualsAndHashCode( exclude = "closed" )
@ToString
@Slf4j
public class Buffers implements Closeable {

    public final ReentrantLock lock = new ReentrantLock();
    //    private final int bufferSize;
    private final ConcurrentHashMap<LogId, Buffer> currentBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LogId, BufferConfiguration> configurationForSelector = new ConcurrentHashMap<>();
    private final BufferConfigurationMap configurations;
    public BufferCache cache;
    ReadyQueue readyBuffers = new ReadyQueue();
    private volatile boolean closed;

    public Buffers( BufferConfigurationMap configurations ) {
        this.configurations = configurations;
        this.cache = new BufferCache();
    }

    public final void put( LogId key, ProtocolVersion protocolVersion, byte[] buffer ) {
        put( key, protocolVersion, buffer, 0, buffer.length );
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    public final void put( LogId id, ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "current buffer is already closed" );

        BufferConfiguration conf = configurationForSelector.computeIfAbsent( id, this::findConfiguration );
        int bufferSize = conf.bufferSize;

        currentBuffers.compute( id, ( _, b ) -> {
            if( b == null ) {
                b = cache.get( id, protocolVersion, bufferSize );
            }

            if( bufferSize - b.headerLength() < length )
                throw new IllegalArgumentException( "buffer size is too big: " + length + " for buffer of " + bufferSize + "; headers = " + b.headerLength() );

            if( !b.available( length ) ) {
                readyBuffers.ready( b );
                b = cache.get( id, protocolVersion, bufferSize );
            }

            b.put( buffer, offset, length );

            return b;
        } );
    }

    private BufferConfiguration findConfiguration( LogId id ) {
        for( var conf : configurations.entrySet() ) {
            if( conf.getValue().pattern.matcher( id.logType ).find() ) return conf.getValue();
        }
        throw new IllegalStateException( "Pattern for " + id + " not found" );
    }

    public void flush() {
        for( LogId internSelector : currentBuffers.keySet() ) {
            Buffer buffer = currentBuffers.remove( internSelector );
            if( buffer != null && !buffer.isEmpty() ) {
                readyBuffers.ready( buffer );
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
            Iterator<Buffer> iterator = readyBuffers.iterator();
            while( iterator.hasNext() ) {
                Buffer buffer = iterator.next();
                consumer.accept( buffer );
                iterator.remove();
                cache.release( buffer );
            }
        } finally {
            lock.unlock();
        }
    }

    public void report() {
        report( readyBuffers.buffers, "true" );
        report( currentBuffers.values(), "false" );
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

    public static class BufferCache {
        private final ReentrantLock lock = new ReentrantLock();

        private final HashMap<Integer, Queue<Buffer>> cache = new HashMap<>();

        private Buffer get( LogId id, ProtocolVersion protocolVersion, int bufferSize ) {
            lock.lock();
            try {
                Queue<Buffer> list = cache.computeIfAbsent( bufferSize, bs -> new LinkedList<>() );

                if( list.isEmpty() ) {
                    return new Buffer( bufferSize, id, protocolVersion );
                } else {
                    Buffer buffer = list.poll();
                    buffer.reset( id );
                    return buffer;
                }
            } finally {
                lock.unlock();
            }
        }

        private void release( Buffer buffer ) {
            lock.lock();
            try {
                Queue<Buffer> list = cache.get( buffer.length() );
                if( list != null ) list.offer( buffer );
            } finally {
                lock.unlock();
            }
        }

        public final int size( int bufferSize ) {
            Queue<Buffer> list = cache.get( bufferSize );
            return list != null ? list.size() : 0;
        }
    }

    static class ReadyQueue implements Serializable {
        static Cuid digestionIds = Cuid.UNIQUE;

        private final ReentrantLock lock = new ReentrantLock();
        private final ConcurrentLinkedQueue<Buffer> buffers = new ConcurrentLinkedQueue<>();

        public final void ready( Buffer buffer ) {
            lock.lock();
            try {
                buffer.close( digestionIds.nextLong() );
                buffers.offer( buffer );
            } finally {
                lock.unlock();
            }
        }

        public final Iterator<Buffer> iterator() {
            return buffers.iterator();
        }

        public final int size() {
            return buffers.size();
        }

        public final boolean isEmpty() {
            return buffers.isEmpty();
        }
    }
}
