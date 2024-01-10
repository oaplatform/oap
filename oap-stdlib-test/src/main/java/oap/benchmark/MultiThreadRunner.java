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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.ToString;
import oap.concurrent.Executors;

import static java.util.concurrent.TimeUnit.HOURS;

@ToString
class MultiThreadRunner extends AbstractRunner {
    private final int threads;

    MultiThreadRunner( int threads ) {
        this.threads = threads;
    }

    @SneakyThrows
    @Override
    public Result runExperiment( int experiment, Benchmark benchmark ) {
        var pool = Executors.newFixedThreadPool( threads, new ThreadFactoryBuilder().setNameFormat( "name-%d" ).build() );

        int range = benchmark.samples / threads;

        long start = System.nanoTime();
        for( var t = 0; t < threads; t++ ) {
            int finalT = t;
            pool.execute( () -> {
                for( int i = 0; i < range; i++ ) benchmark.code.accept( experiment, i + range * finalT );
            } );
        }
        pool.shutdown();
        pool.awaitTermination( 5, HOURS );

        return benchmark.toResult( experiment, System.nanoTime() - start );
    }
}
