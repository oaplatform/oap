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

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.PoolConfig;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.util.Dates;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
/**
 * @see https://github.com/DanielYWoo/fast-object-pool
 */
public class Pool<T> implements Closeable {
    public static final int MAX_WAIT_MILLISECONDS = ( int ) Dates.s( 5 );
    public static final int MAX_IDLE_MILLISECONDS = ( int ) Dates.m( 5 );
    public static final int SCAVENGE_INTERVAL_MILLISECONDS = ( int ) Dates.m( 2 );
    private final Executors.BlockingExecutor threadPool;
    private DisruptorObjectPool<T> objectPool;

    public Pool( int size, ObjectFactory<T> objectFactory, ThreadFactory threadFactory ) {
        this( size, size, 1, MAX_WAIT_MILLISECONDS, MAX_IDLE_MILLISECONDS, 0, objectFactory, threadFactory );
    }

    public Pool( int minSize, int maxSize, int partitionSize, ObjectFactory<T> objectFactory, ThreadFactory threadFactory ) {
        this( minSize, maxSize, partitionSize, MAX_WAIT_MILLISECONDS, MAX_IDLE_MILLISECONDS,
            minSize == maxSize ? 0 : SCAVENGE_INTERVAL_MILLISECONDS, objectFactory, threadFactory );
    }

    /**
     * @param minSize
     * @param maxSize                      the maximum number of threads to allow in the pool
     * @param partitionSize
     * @param maxWaitMilliseconds          when pool is full, wait at most maxWaitMilliseconds, then throw an exception
     * @param maxIdleMilliseconds          objects idle for maxIdleMilliseconds will be destroyed to shrink the pool size
     * @param scavengeIntervalMilliseconds set it to zero if you don't want to automatically shrink your pool.
     * @param objectFactory
     */
    public Pool( int minSize, int maxSize, int partitionSize, int maxWaitMilliseconds, int maxIdleMilliseconds, int scavengeIntervalMilliseconds,
                 ObjectFactory<T> objectFactory, ThreadFactory threadFactory ) {
        var config = new PoolConfig()
            .setMinSize( minSize )
            .setMinSize( maxSize )
            .setPartitionSize( partitionSize )
            .setMaxWaitMilliseconds( maxWaitMilliseconds )
            .setMaxIdleMilliseconds( maxIdleMilliseconds )
            .setScavengeIntervalMilliseconds( scavengeIntervalMilliseconds );

        objectPool = new DisruptorObjectPool<T>( config, objectFactory );
        threadPool = Executors.newFixedBlockingThreadPool( minSize, maxSize, threadFactory );
    }

    public <TResult> CompletableFuture<TResult> supply( Function<T, TResult> func ) {
        var obj = objectPool.borrowObject( true );
        try {
            return CompletableFuture
                .supplyAsync( () -> func.apply( obj.getObject() ), threadPool )
                .whenComplete( ( r, e ) -> objectPool.returnObject( obj ) );
        } catch( Exception e ) {
            objectPool.returnObject( obj );
            throw e;
        }
    }

    public CompletableFuture<Void> run( Consumer<T> func ) {
        var obj = objectPool.borrowObject( true );
        try {
            return CompletableFuture
                .runAsync( () -> func.accept( obj.getObject() ), threadPool )
                .whenComplete( ( r, e ) -> objectPool.returnObject( obj ) );
        } catch( Exception e ) {
            objectPool.returnObject( obj );
            throw e;
        }
    }

    public void shutdownNow() {
        threadPool.shutdownNow();
        try {
            objectPool.shutdown();
        } catch( InterruptedException e ) {
            log.warn( e.getMessage() );
        }
    }

    @Override
    public void close() {
        try {
            threadPool.shutdown();
            threadPool.awaitTermination( 1, TimeUnit.HOURS );
        } catch( InterruptedException e ) {
            log.warn( e.getMessage() );
        }

        try {
            objectPool.shutdown();
        } catch( InterruptedException e ) {
            log.warn( e.getMessage() );
        }

    }
}
