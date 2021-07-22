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

package oap.concurrent.pool;

import oap.benchmark.Benchmark;
import org.testng.annotations.Test;

public class StringBuilderPoolPerformance {
    private static final int THREADS = 64;
    private static final int SAMPLES = 100;
    private static final int ITERATIONS = 100;

    @Test
    public void test() {
        var threads = new Thread[THREADS];

        var pool = new StringBuilderPool( 16,
            SAMPLES * THREADS, SAMPLES * THREADS, Integer.MAX_VALUE, Integer.MAX_VALUE );

        Benchmark.benchmark( "FastObjectPool", SAMPLES, () -> {
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1] = new Thread( () -> {
                    for( var i = 0; i < ITERATIONS; i++ ) {
                        try( var sbp1 = pool.borrowObject( true ) ) {
                            sbp1.getObject().append( "test" );
                        }
                    }
                } );
            }
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1].start();
            }
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1].join();
            }
        } ).experiments( 5 ).run();

        System.out.println( pool.getSize() );

        Benchmark.benchmark( "Fast", SAMPLES, () -> {
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1] = new Thread( () -> {
                    for( var i = 0; i < ITERATIONS; i++ ) {
                        var sb = new StringBuilder();
                        sb.append( "test" );
                    }
                } );
            }
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1].start();
            }
            for( var i1 = 0; i1 < THREADS; i1++ ) {
                threads[i1].join();
            }
        } ).experiments( 5 ).run();

        System.out.println( pool.getSize() );
    }

}
