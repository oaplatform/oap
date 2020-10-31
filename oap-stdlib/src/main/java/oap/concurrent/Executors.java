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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oap.concurrent.scheduler.ScheduledExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class Executors {
    public static ExecutorService newFixedThreadPool( int nThreads ) {
        return new ThreadPoolExecutor( nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>() );
    }

    public static ExecutorService newFixedThreadPool( int nThreads, ThreadFactory threadFactory ) {
        return new ThreadPoolExecutor( nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory );
    }

    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor( 0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>() );
    }

    public static ExecutorService newCachedThreadPool( ThreadFactory threadFactory ) {
        return new ThreadPoolExecutor( 0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            threadFactory );
    }

    public static BlockingExecutor newFixedBlockingThreadPool( int nThreads ) {
        return new BlockingExecutor( nThreads );
    }

    public static BlockingExecutor newFixedBlockingThreadPool( int nThreads, ThreadFactory threadFactory ) {
        return new BlockingExecutor( nThreads, threadFactory );
    }

    public ScheduledExecutorService newScheduledThreadPool( int corePoolSize, String threadPrefix ) {
        return new ScheduledExecutorService( java.util.concurrent.Executors.newScheduledThreadPool(
            corePoolSize,
            new ThreadFactoryBuilder().setNameFormat( threadPrefix + "-%d" ).build() ) );
    }

    public static class BlockingExecutor implements Executor {
        private final Semaphore semaphore;
        private final ThreadPoolExecutor threadPoolExecutor;

        private BlockingExecutor( int concurrentTasksLimit, ThreadFactory threadFactory ) {
            semaphore = new Semaphore( concurrentTasksLimit );
            this.threadPoolExecutor = new ThreadPoolExecutor(
                concurrentTasksLimit, concurrentTasksLimit,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory );
        }

        private BlockingExecutor( int concurrentTasksLimit ) {
            semaphore = new Semaphore( concurrentTasksLimit );
            this.threadPoolExecutor = new ThreadPoolExecutor(
                concurrentTasksLimit, concurrentTasksLimit,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>() );
        }

        @Override
        public void execute( Runnable command ) {
            try {
                semaphore.acquire();
            } catch( InterruptedException e ) {
                e.printStackTrace();
                return;
            }

            final Runnable wrapped = () -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            };

            threadPoolExecutor.execute( wrapped );
        }

        public void shutdown() {
            threadPoolExecutor.shutdown();
        }
    }
}
