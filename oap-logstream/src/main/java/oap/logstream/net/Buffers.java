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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.logstream.LogId;
import oap.logstream.net.BufferConfigurationMap.BufferConfiguration;
import oap.metrics.Metrics;
import oap.metrics.Metrics2;
import oap.util.Cuid;

import java.io.Closeable;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

@EqualsAndHashCode( exclude = "closed" )
@ToString
@Slf4j
public class Buffers implements Closeable {
    private final Path location;
    //    private final int bufferSize;
    private final ConcurrentHashMap<String, Buffer> currentBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LogId, BufferConfiguration> configurationForSelector = new ConcurrentHashMap<>();
    private final BufferConfigurationMap configurations;
    ReadyQueue readyBuffers = new ReadyQueue();
    BufferCache cache;
    private boolean closed;

    public Buffers( Path location, BufferConfigurationMap configurations ) {
        this.location = location;
        this.configurations = configurations;
        this.cache = new BufferCache();
        try {
            if( java.nio.file.Files.exists( location ) )
                readyBuffers = Files.readObject( location );
            log.info( "unsent buffers: {}", readyBuffers.size() );
        } catch( Exception e ) {
            log.warn( e.getMessage() );
        }
        Files.delete( location );
    }

    public final void put( LogId key, byte[] buffer ) {
        put( key, buffer, 0, buffer.length );
    }

    public final void put( LogId id, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "current buffers already closed" );

        BufferConfiguration conf = configurationForSelector.computeIfAbsent( id, this::findConfiguration );

        final int bufferSize = conf.bufferSize;
        String intern = id.lock();
        synchronized( intern ) {
            Buffer b = currentBuffers.computeIfAbsent( intern, k -> cache.get( id, bufferSize ) );
            if( bufferSize - b.headerLength() < length )
                throw new IllegalArgumentException( "buffer size is too big: " + length + " for buffer of " + bufferSize + "; headers = " + b.headerLength() );
            if( !b.available( length ) ) {
                readyBuffers.ready( b );
                currentBuffers.put( intern, b = cache.get( id, bufferSize ) );
            }
            b.put( buffer, offset, length );
        }
    }

    private BufferConfiguration findConfiguration( LogId id ) {
        for( var conf : configurations.entrySet() ) {
            if( conf.getValue().pattern.matcher( id.logType ).find() ) return conf.getValue();
        }
        throw new IllegalStateException( "Pattern for " + id + " not found" );
    }

    private void flush() {
        for( String internSelector : currentBuffers.keySet() ) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized( internSelector ) {
                Buffer buffer = currentBuffers.remove( internSelector );
                if( buffer != null && !buffer.isEmpty() ) readyBuffers.ready( buffer );
            }
        }

    }

    public final boolean isEmpty() {
        return readyBuffers.isEmpty();
    }


    @Override
    public final synchronized void close() {
        if( closed ) throw new IllegalStateException( "already closed" );
        closed = true;
        flush();
        log.info( "writing {} unsent buffers to {}", readyBuffers.size(), location );
        Files.writeObject( location, readyBuffers );
    }

    public final synchronized void forEachReadyData( Predicate<Buffer> consumer ) {
        flush();
        Metrics2.measureHistogram( Metrics.name( "logging.buffers_count" ), readyBuffers.size() );
        log.debug( "buffers to go " + readyBuffers.size() );
        Iterator<Buffer> iterator = readyBuffers.iterator();
        while( iterator.hasNext() && !closed ) {
            Buffer buffer = iterator.next();
            if( consumer.test( buffer ) ) {
                iterator.remove();
                cache.release( buffer );
            } else break;
        }
    }

    final int readyBuffers() {
        return readyBuffers.size();
    }

    public static class BufferCache {
        private final HashMap<Integer, Queue<Buffer>> cache = new HashMap<>();

        private synchronized Buffer get( LogId id, int bufferSize ) {
            final Queue<Buffer> list = cache.computeIfAbsent( bufferSize, ( bs ) -> new LinkedList<>() );

            if( list.isEmpty() ) return new Buffer( bufferSize, id );
            else {
                Buffer buffer = list.poll();
                buffer.reset( id );
                return buffer;
            }
        }

        private synchronized void release( Buffer buffer ) {
            final Queue<Buffer> list = cache.get( buffer.length() );
            if( list != null ) list.offer( buffer );
        }

        public final int size( int bufferSize ) {
            return Optional.ofNullable( cache.get( bufferSize ) ).map( Collection::size ).orElse( 0 );
        }
    }

    static class ReadyQueue implements Serializable {
        static Cuid digestionIds = Cuid.UNIQUE;
        private final ConcurrentLinkedQueue<Buffer> buffers = new ConcurrentLinkedQueue<>();

        public final synchronized void ready( Buffer buffer ) {
            buffer.close( digestionIds.nextLong() );
            buffers.offer( buffer );
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
