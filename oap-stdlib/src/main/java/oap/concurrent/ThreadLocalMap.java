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

package oap.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;


@Slf4j
public class ThreadLocalMap<T extends Closeable> implements Closeable, AutoCloseable {
    private final Supplier<T> init;
    private final ConcurrentMap<Thread, T> map = new ConcurrentHashMap<>();

    public ThreadLocalMap() {
        this( null );
    }

    public ThreadLocalMap( Supplier<T> init ) {
        this.init = init;
    }

    /**
     * @see ThreadLocalMap#ThreadLocalMap(Supplier)
     */
    @Deprecated
    public static <T extends Closeable> ThreadLocalMap<T> withInitial( Supplier<T> init ) {
        return new ThreadLocalMap<>( requireNonNull( init ) );
    }

    @Override
    public void close() throws IOException {
        IOException lastE = null;
        for( var v : map.values() )
            try {
                v.close();
            } catch( IOException e ) {
                log.trace( "Cannot close after '{}'", lastE, e );
                lastE = e;
            }

        if( lastE != null ) throw lastE;
    }

    public T get() {
        var currentThread = Thread.currentThread();
        if( init != null )
            return map.computeIfAbsent( currentThread, t -> init.get() );

        return map.get( currentThread );
    }
}
