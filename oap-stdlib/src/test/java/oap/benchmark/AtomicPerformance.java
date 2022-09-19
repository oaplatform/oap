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

package oap.benchmark;

import org.apache.commons.lang3.mutable.MutableLong;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static oap.benchmark.Benchmark.benchmark;

@Test
public class AtomicPerformance {
    private final MutableLong ml = new MutableLong();
    private final AtomicLong al = new AtomicLong( 0 );
    private final LongAdder la = new LongAdder();
    private final AtomicInteger ai = new AtomicInteger( 0 );
    private volatile long l = 0;
    private volatile long l2 = 0;

    @Test
    public void testAtomicLong() {
        final int threads = 1024;
        final int samples = 100000000;

        benchmark( "mutale-long", samples, ml::incrementAndGet ).run();

        benchmark( "atomic-long-one-thread", samples, al::incrementAndGet ).run();

        benchmark( "atomic-long", samples, al::incrementAndGet ).threads( threads ).run();

        benchmark( "long-adder", samples, la::increment ).threads( threads ).run();

        benchmark( "atomic-integer", samples, ai::incrementAndGet ).threads( threads ).run();

        benchmark( "long", samples, () -> l++ ).threads( threads ).run();

        benchmark( "long-synchronized", samples, () -> {
            synchronized( AtomicPerformance.class ) {
                l2++;
            }
        } ).threads( threads ).run();

        System.out.println( "al:" + al.get() + " vs l:" + l + " vs ls:" + l2 );
    }
}
