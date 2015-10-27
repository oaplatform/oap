/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static oap.testng.Asserts.assertEventually;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SchedulerTest extends AbstractTest {

    static Runnable getLambda( AtomicInteger counter ) {
        return () -> {
            System.out.println( "counter = " + counter );
            counter.incrementAndGet();
        };
    }

    @Test
    public void lambdaIsolation() throws InterruptedException {
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        Runnable lambda1 = getLambda( c1 );
        Runnable lambda2 = getLambda( c2 );
        assertEquals( lambda1.getClass(), lambda2.getClass() );
        try( Scheduled ignored = Scheduler.scheduleWithFixedDelay( 10, TimeUnit.MILLISECONDS, lambda1 );
             Scheduled ignored2 = Scheduler.scheduleWithFixedDelay( 10, TimeUnit.MILLISECONDS, lambda2 ) ) {
            assertEventually( 50, 30, () -> {
                assertTrue( c1.get() > 8 );
                assertTrue( c2.get() > 8 );
            } );
        }
    }
}
