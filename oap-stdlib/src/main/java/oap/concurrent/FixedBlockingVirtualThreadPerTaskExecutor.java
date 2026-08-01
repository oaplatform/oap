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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Virtual-thread-per-task executor bounded to a fixed number of concurrently running tasks.
 * {@code newVirtualThreadPerTaskExecutor()} spawns an unbounded number of threads, so a
 * {@link Semaphore} gates task start: {@code execute}/{@code submit}/{@code invokeAll}/{@code invokeAny}
 * block while {@code threads} tasks are already running, and unblock as tasks complete.
 */
class FixedBlockingVirtualThreadPerTaskExecutor extends AbstractExecutorService {
    private final ExecutorService delegate = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore semaphore;

    FixedBlockingVirtualThreadPerTaskExecutor( int threads ) {
        this.semaphore = new Semaphore( threads );
    }

    @Override
    public void execute( Runnable command ) {
        semaphore.acquireUninterruptibly();
        try {
            delegate.execute( () -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            } );
        } catch( RuntimeException | Error e ) {
            semaphore.release();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
        return delegate.awaitTermination( timeout, unit );
    }
}
