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
import oap.io.Files;
import oap.metrics.Metrics;
import oap.util.Pair;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;

import static oap.util.Pair.__;
import static org.slf4j.LoggerFactory.getLogger;

@EqualsAndHashCode( exclude = "closed" )
@ToString
public class Buffers implements Closeable {
    private final Path location;
    private final int bufferSize;
    private boolean closed;
    private static final Logger logger = getLogger( Buffers.class );
    private State state = new State();

    @EqualsAndHashCode
    @ToString
    private static class State implements Serializable {
        Map<String, Buffer> currentBuffers = new ConcurrentHashMap<>();
        Queue<Pair<String, byte[]>> readyBuffers = new ConcurrentLinkedQueue<>();
    }

    public Buffers( Path location, int bufferSize ) {
        this.location = location;
        this.bufferSize = bufferSize;
        try {
            if( location.toFile().exists() ) state = Files.readObject( location );
        } catch( Exception e ) {
            logger.error( "cannot read " + location + ". Ignoring...." );
        }
    }

    public void put( String key, byte[] buffer ) {
        put( key, buffer, 0, buffer.length );
    }

    public void put( String key, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "currentBuffers already closed" );
        synchronized( key.intern() ) {
            Buffer b = state.currentBuffers.computeIfAbsent( key, k -> new Buffer( bufferSize ) );
            if( !b.available( length ) ) {
                state.readyBuffers.offer( __( key.intern(), b.data() ) );
                b.reset();
            }
            b.put( buffer, offset, length );
        }
    }

    private void flush() {
        state.currentBuffers.forEach( ( key, b ) -> {
            synchronized( key.intern() ) {
                if( !b.isEmpty() ) {
                    state.readyBuffers.offer( __( key.intern(), b.data() ) );
                    b.reset();
                }
            }
        } );
    }

    @Override
    public synchronized void close() {
        if( closed ) throw new IllegalStateException( "already closed" );
        closed = true;
        Files.writeObject( location, state );
    }

    public synchronized void forEachReadyData( BiPredicate<String, byte[]> consumer ) {
        flush();
        Metrics.measureHistogram( Metrics.name( "logging_buffers_count" ), state.readyBuffers.size() );
        Iterator<Pair<String, byte[]>> iterator = state.readyBuffers.iterator();
        while( iterator.hasNext() ) {
            Pair<String, byte[]> next = iterator.next();
            if( consumer.test( next._1, next._2 ) ) iterator.remove();
            else break;
        }
    }
}
