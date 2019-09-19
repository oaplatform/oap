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

import oap.concurrent.Threads;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

public class SchedulerTest {

    static Runnable getLambda( AtomicInteger counter ) {
        return () -> {
            System.out.println( "counter = " + counter );
            counter.incrementAndGet();
        };
    }

    @Test
    public void lambdaIsolation() {
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        Runnable lambda1 = getLambda( c1 );
        Runnable lambda2 = getLambda( c2 );
        assertThat( lambda1.getClass() ).isEqualTo( lambda2.getClass() );
        try( Scheduled ignored = Scheduler.scheduleWithFixedDelay( 10, MILLISECONDS, lambda1 );
             Scheduled ignored2 = Scheduler.scheduleWithFixedDelay( 10, MILLISECONDS, lambda2 ) ) {
            assertEventually( 50, 30, () -> {
                assertTrue( c1.get() > 8 );
                assertTrue( c2.get() > 8 );
            } );
        }
    }

    @Test
    public void cancel() {
        AtomicInteger counter = new AtomicInteger( 0 );
        Scheduled scheduled = Scheduler.scheduleWithFixedDelay( 50, MILLISECONDS, () -> {
            counter.incrementAndGet();
            Threads.sleepSafely( 1000 );
            counter.incrementAndGet();
        } );
        Threads.sleepSafely( 500 );
        scheduled.cancel();
        int value = counter.get();
        assertThat( value ).isGreaterThan( 0 );
        Threads.sleepSafely( 500 );
        assertThat( counter.get() ).isEqualTo( value );
    }

    @Test
    public void triggerNow() {
        AtomicInteger counter = new AtomicInteger( 0 );
        var scheduled = Scheduler.scheduleWithFixedDelay( 500, SECONDS, () -> {
            Threads.sleepSafely( 100 );
            System.out.println( "executed..." );
            counter.incrementAndGet();
        } );
        scheduled.triggerNow();
        assertThat( counter.get() ).isEqualTo( 2 );
        scheduled.triggerNow();
        assertThat( counter.get() ).isEqualTo( 3 );
        scheduled.triggerNow();
        scheduled.triggerNow();
        scheduled.triggerNow();
        assertThat( counter.get() ).isEqualTo( 6 );

//        unreliable test
//        var threads = Executors.newFixedThreadPool( 10 );
//        var tasks = List.of(
//            threads.submit( scheduled::triggerNow ),
//            threads.submit( scheduled::triggerNow ),
//            threads.submit( scheduled::triggerNow ),
//            threads.submit( scheduled::triggerNow ),
//            threads.submit( scheduled::triggerNow ),
//            threads.submit( scheduled::triggerNow )
//        );
//        tasks.forEach( t -> Try.supply( t::get ).get() );
//        assertThat( counter.get() ).isEqualTo( 3 );
//        threads.shutdown();
        scheduled.cancel();
    }

}
