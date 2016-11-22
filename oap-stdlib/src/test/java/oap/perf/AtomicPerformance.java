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

package oap.perf;

import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by igor.petrenko on 26.10.2016.
 */
@Test( enabled = false )
public class AtomicPerformance extends AbstractPerformance {
    private final AtomicLong al = new AtomicLong( 0 );
    private final AtomicInteger ai = new AtomicInteger( 0 );
    private volatile long l = 0;
    private volatile long l2 = 0;

    @Test
    public void testAtomicLong() {
        final int threads = 1024;
        final int experiments = 5;
        final int samples = 100000000;

        benchmark( "atomic-long", samples, experiments, threads, ( i ) -> {
            al.incrementAndGet();
        } );

        benchmark( "atomic-integer", samples, experiments, threads, ( i ) -> {
            ai.incrementAndGet();
        } );

        benchmark( "long", samples, experiments, threads, ( i ) -> {
            l++;
        } );

        benchmark( "long-synchronized", samples, experiments, threads, ( i ) -> {
            synchronized( AtomicPerformance.class ) {
                l2++;
            }
        } );

        System.out.println( "al:" + al.get() + " vs l:" + l + " vs ls:" + l2 );
    }
}
