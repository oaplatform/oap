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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j( topic = "UncaughException" )
public class ThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor {
    public ThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue ) {
        super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue );
    }

    public ThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory ) {
        super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory );
    }

    public ThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler ) {
        super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler );
    }

    public ThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler ) {
        super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler );
    }

    @Override
    protected void afterExecute( Runnable r, Throwable t ) {
        super.afterExecute( r, t );
        Throwable throwable = t;
        if( throwable == null && r instanceof Future<?> ) try {
            ( ( Future<?> ) r ).get();
        } catch( CancellationException e ) {
            throwable = e;
        } catch( ExecutionException e ) {
            throwable = e.getCause();
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt(); // ignore/reset
        }
        if( throwable != null ) log.error( throwable.getMessage(), throwable );
    }
}
