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

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class PeriodicScheduled extends Scheduled implements Runnable {
    private final AtomicLong lastTimeExecuted = new AtomicLong( 0 );
    Scheduled scheduled;
    private final Class<?> owner;
    private final long safePeriod;
    private final Consumer<Long> job;

    public PeriodicScheduled( Class<?> owner, long safePeriod, Consumer<Long> job ) {
        this.owner = owner;
        this.safePeriod = safePeriod;
        this.job = job;
    }

    public Class<?> getOwner() {
        return owner;
    }

    public void run() {
        log.trace( "executing {}", scheduled );
        long current = DateTimeUtils.currentTimeMillis() - safePeriod;
        this.job.accept( lastTimeExecuted.get() );
        lastTimeExecuted.set( current );
        log.trace( "executed {}", scheduled );
    }

    @Override
    public void cancel() {
        Scheduled.cancel( scheduled );
    }

    @Override
    public void triggerNow() {
        this.scheduled.triggerNow();
    }

    public long lastExecuted() {
        return lastTimeExecuted.get();
    }

}
