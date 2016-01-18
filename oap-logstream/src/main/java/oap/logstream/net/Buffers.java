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
import oap.metrics.Metrics;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

@EqualsAndHashCode( exclude = "closed" )
@ToString
@Slf4j
public class Buffers implements Closeable {
    private final Path location;
    private final int bufferSize;
    private boolean closed;
    private final Map<String, Buffer> currentBuffers = new HashMap<>();
    ReadyQueue readyBuffers = new ReadyQueue();
    BufferCache cache;

    public Buffers( Path location, int bufferSize ) {
        this.location = location;
        this.bufferSize = bufferSize;
        this.cache = new BufferCache( bufferSize );
        try {
            if( location.toFile().exists() )
                readyBuffers = Files.readObject( location );
            log.debug( "unsent buffers: {}", readyBuffers.size() );
        } catch( Exception e ) {
            log.warn( e.getMessage() );
        }
        location.toFile().delete();
    }

    public void put( String key, byte[] buffer ) {
        put( key, buffer, 0, buffer.length );
    }

    public void put( String selector, byte[] buffer, int offset, int length ) {
        synchronized( currentBuffers ) {
            if( closed ) throw new IllegalStateException( "current buffers already closed" );
            if( length > bufferSize )
                throw new IllegalArgumentException( "buffer size is too big: " + length + " for buffer of " + bufferSize );
            Buffer b = currentBuffers.computeIfAbsent( selector, k -> cache.get( selector ) );
            if( !b.available( length ) ) {
                readyBuffers.ready( b );
                currentBuffers.put( selector, b = cache.get( selector ) );
            }
            b.put( buffer, offset, length );
        }
    }


    private void flush() {
        synchronized( currentBuffers ) {
            Iterator<Map.Entry<String, Buffer>> iterator = currentBuffers.entrySet().iterator();
            while( iterator.hasNext() ) {
                Map.Entry<String, Buffer> entry = iterator.next();
                Buffer buffer = entry.getValue();
                if( !buffer.isEmpty() ) {
                    readyBuffers.ready( buffer );
                    iterator.remove();
                }
            }
        }
    }

    public boolean isEmpty() {
        return readyBuffers.isEmpty();
    }


    @Override
    public synchronized void close() {
        if( closed ) throw new IllegalStateException( "already closed" );
        closed = true;
        flush();
        log.info( "writing {} unsent buffers to {}", readyBuffers.size(), location );
        Files.writeObject( location, readyBuffers );
    }

    public synchronized void forEachReadyData( Predicate<Buffer> consumer ) {
        flush();
        Metrics.measureHistogram( Metrics.name( "logging_buffers_count" ), readyBuffers.size() );
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

    int readyBuffers() {
        return readyBuffers.size();
    }

    public static class BufferCache {
        Queue<Buffer> cache = new LinkedList<>();
        private int bufferSize;

        public BufferCache( int bufferSize ) {
            this.bufferSize = bufferSize;
        }

        private synchronized Buffer get( String selector ) {
            if( cache.isEmpty() ) return new Buffer( bufferSize, selector );
            else {
                Buffer buffer = cache.poll();
                buffer.reset( selector );
                return buffer;
            }
        }

        private synchronized void release( Buffer buffer ) {
            cache.offer( buffer );
        }

        public int size() {
            return cache.size();
        }
    }

    static class ReadyQueue implements Serializable {
        private Queue<Buffer> buffers = new ConcurrentLinkedQueue<>();
        static volatile long digestionIds = DateTimeUtils.currentTimeMillis();

        public synchronized void ready( Buffer buffer ) {
            buffer.close( digestionIds++ );
            buffers.offer( buffer );
        }

        public Iterator<Buffer> iterator() {
            return buffers.iterator();
        }

        public int size() {
            return buffers.size();
        }

        public boolean isEmpty() {
            return buffers.isEmpty();
        }
    }
}
