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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

@EqualsAndHashCode( exclude = "closed" )
@ToString
@Slf4j
public class Buffers implements Closeable {
    private final Path location;
    private final int bufferSize;
    private boolean closed;
    Map<String, Buffer> currentBuffers = new ConcurrentHashMap<>();
    Queue<Bucket> readyBuffers = new ConcurrentLinkedQueue<>();
    private static long idseed = DateTimeUtils.currentTimeMillis();
    BufferCache cache;

    @EqualsAndHashCode
    static class Bucket implements Serializable {
        String selector;
        long id = idseed++;
        Buffer buffer;

        public Bucket( String selector, Buffer buffer ) {
            this.selector = selector;
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            return "(" + id + ", " + selector + ", " + buffer.length() + ")";
        }
    }

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

    public void put( String key, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "current buffers already closed" );
        if( length > bufferSize )
            throw new IllegalArgumentException( "buffer size is too big: " + length + " for buffer of " + bufferSize );
        String keyInterned = key.intern();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized( keyInterned ) {
            Buffer b = currentBuffers.computeIfAbsent( keyInterned, k -> cache.get() );
            if( !b.available( length ) ) {
                readyBuffers.offer( new Bucket( keyInterned, b ) );
                currentBuffers.put( keyInterned, b = cache.get() );
            }
            b.put( buffer, offset, length );
        }
    }


    private synchronized void flush() {
        Iterator<Map.Entry<String, Buffer>> iterator = currentBuffers.entrySet().iterator();
        while( iterator.hasNext() ) {
            Map.Entry<String, Buffer> entry = iterator.next();
            String keyInterned = entry.getKey().intern();
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized( keyInterned ) {
                if( !entry.getValue().isEmpty() ) {
                    readyBuffers.offer( new Bucket( keyInterned, entry.getValue() ) );
                    iterator.remove();
                }
            }
        }
    }

    public boolean isEmpty() {
        return readyBuffers.isEmpty();
    }


    @Override
    public void close() {
        if( closed ) throw new IllegalStateException( "already closed" );
        closed = true;
        synchronized( this ) {
            flush();
            log.info( "writing unsent buffers " + readyBuffers.size() );
            Files.writeObject( location, readyBuffers );
        }
    }

    public synchronized void forEachReadyData( Predicate<Bucket> consumer ) {
        flush();
        Metrics.measureHistogram( Metrics.name( "logging_buffers_count" ), readyBuffers.size() );
        log.debug( "buffers to go " + readyBuffers.size() );
        Iterator<Bucket> iterator = readyBuffers.iterator();
        while( iterator.hasNext() && !closed ) {
            Bucket bucket = iterator.next();
            if( consumer.test( bucket ) ) {
                iterator.remove();
                cache.release( bucket.buffer );
            } else break;
        }
    }

    public int size() {
        return readyBuffers.size();
    }

    public static class BufferCache {
        Queue<Buffer> cache = new LinkedList<>();
        private int bufferSize;

        public BufferCache( int bufferSize ) {
            this.bufferSize = bufferSize;
        }

        private synchronized Buffer get() {
            return cache.isEmpty() ? new Buffer( bufferSize ) : cache.poll();
        }

        private synchronized void release( Buffer buffer ) {
            buffer.reset();
            cache.offer( buffer );
        }

        public int size() {
            return cache.size();
        }
    }
}
