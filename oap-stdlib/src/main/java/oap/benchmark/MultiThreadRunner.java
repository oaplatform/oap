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

import lombok.ToString;
import oap.testng.Teamcity;
import oap.util.Try;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@ToString
class MultiThreadRunner extends Runner {
    private int threads;
    private int warming;

    public MultiThreadRunner( int threads, int warming ) {
        this.threads = threads;
        this.warming = warming;
    }

    @Override
    public Result run( Benchmark benchmark ) {
        return Teamcity.progress( benchmark.name + "...", Try.supply( () -> {
            System.out.println( "pool threads = " + threads );

            var factory = new ForkJoinPool.ForkJoinWorkerThreadFactory() {
                @Override
                public ForkJoinWorkerThread newThread( ForkJoinPool pool ) {
                    var worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread( pool );
                    worker.setName( benchmark.name + "-" + worker.getPoolIndex() );
                    return worker;
                }
            };

            var pool = new ForkJoinPool( threads, factory, null, false );
            if( warming > 0 ) {
                System.out.println( "warming up..." );
                pool.submit( () -> {
                    IntStream
                        .range( 0, warming )
                        .parallel()
                        .forEach( i -> pool.submit( () -> benchmark.code.accept( 0 ) ) );
                } ).get( 1, TimeUnit.HOURS );
            }
            System.out.println( "starting test..." );

            List<Result> results = IntStream
                .range( 0, benchmark.experiments )
                .mapToObj( x -> Teamcity.progress( benchmark.name + " e=" + x + "...", Try.supply( () -> {

                        benchmark.beforeExperiment.run();

                        int samplesPerThread = benchmark.samples / threads;

                        long start = System.nanoTime();
                        pool.submit( () -> {
                            IntStream
                                .range( 0, threads )
                                .parallel()
                                .forEach( t -> pool.submit( () -> IntStream
                                    .range( t * samplesPerThread, ( t + 1 ) * samplesPerThread )
                                    .forEach( benchmark.code )

                                ) );
                        } ).get( 5, TimeUnit.HOURS );
                        long total = System.nanoTime() - start;
                        Result result = benchmark.toResult( total );
                        benchmark.printResult( total, result );
                        benchmark.afterExperiment.run();

                        return result;
                    } ) )
                )
                .collect( toList() );

            Result avg = Result.average( results, benchmark.experiments );
            benchmark.printAverageResult( avg );

            Teamcity.performance( benchmark.name, avg.rate );

            return avg;
        } ) );

    }
}
