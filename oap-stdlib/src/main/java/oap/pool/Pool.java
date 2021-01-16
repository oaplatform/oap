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

package oap.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static oap.concurrent.Times.times;

@Slf4j
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Pool<T> implements AutoCloseable {
    private final Semaphore semaphore;
    private final Queue<Poolable<T>> free = new ConcurrentLinkedQueue<>();
    private boolean closed = false;
    private final int size;

    public Pool( int size ) {
        this.size = size;
        this.semaphore = new Semaphore( this.size, true );
    }

    public abstract T create();

    public boolean valid( T t ) {
        return true;
    }

    public void discarded( T t ) {
    }

    public Poolable<T> borrow( long timeout, TimeUnit unit ) {
        try {
            if( closed || !semaphore.tryAcquire( timeout, unit ) ) return Poolable.empty();
            return borrowOrCreate();
        } catch( InterruptedException e ) {
            return Poolable.empty();
        }
    }

    public Poolable<T> borrow() {
        try {
            if( closed ) return Poolable.empty();
            semaphore.acquire();
            return borrowOrCreate();
        } catch( InterruptedException e ) {
            return Poolable.empty();
        }
    }

    public Optional<Future<Void>> async( Consumer<T> action ) {
        Poolable<T> poolable = borrow();
        return poolable.isEmpty()
            ? Optional.empty()
            : Optional.of( CompletableFuture.runAsync( () -> poolable.than( action ).release() ) );
    }


    /**
     * @return never empty
     */
    private Poolable<T> borrowOrCreate() {
        Poolable<T> p;
        while( ( p = free.poll() ) != null ) {
            if( !valid( p.value ) ) discarded( p.value );
            else break;
        }
        return p == null ? Poolable.of( this, create() ) : p;
    }

    protected void release( Poolable<T> poolable ) {
        if( closed ) discarded( poolable.value );
        else free.add( poolable );
        semaphore.release();
    }

    @Override
    public void close() {
        closed = true;
        times( size, () -> {
            try {
                semaphore.acquire();
                Poolable<T> p = free.poll();
                if( p != null ) discarded( p.value );
            } catch( InterruptedException e ) {
                log.debug( "abnormal pool shutdown", e );
            }
        } );
    }

    @Override
    public String toString() {
        return "Pool(permits=" + semaphore.availablePermits() + ",free=" + free.size() + ",closed=" + closed + ")";
    }
}
