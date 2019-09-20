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

package oap.concurrent.scheduler;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.atomic.AtomicReference;

@DisallowConcurrentExecution
@Slf4j
public class RunnableJob implements Job, InterruptableJob {
    final JobDetail jobDetail;
    private final Runnable runnable;
    private final AtomicReference<Thread> runningThread = new AtomicReference<>();


    public RunnableJob( JobDetail jobDetail, Runnable runnable ) {
        this.jobDetail = jobDetail;
        this.runnable = runnable;
    }

    @Override
    public synchronized void execute( JobExecutionContext context ) throws JobExecutionException {
        try {
            log.trace( "executing {}", jobDetail );
            runningThread.set( Thread.currentThread() );
            if( !Thread.interrupted() ) {
                runnable.run();
            }
        } catch( Exception e ) {
            throw new JobExecutionException( e );
        } finally {
            runningThread.set( null );
            this.notifyAll();
        }
    }

    @Override
    public void interrupt() {
        Thread thread = runningThread.getAndSet( null );
        if( thread != null ) thread.interrupt();
    }

    @SneakyThrows
    public void triggerNow() {
        synchronized( this ) {
            log.trace( "forcefully triggering job {}", jobDetail );
            Scheduler.scheduler.triggerJob( jobDetail.getKey() );
            this.wait();
        }
    }

    @Override
    public String toString() {
        return jobDetail.toString();
    }
}
