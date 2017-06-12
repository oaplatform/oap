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
import lombok.ToString;
import oap.testng.Teamcity;
import oap.util.Try;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by razer on 6/9/17.
 */
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
        return Teamcity.progress( benchmark.name + "...", () -> {
            System.out.println( "pool threads = " + threads );
            ExecutorService pool = oap.concurrent.Executors.newFixedThreadPool( threads, new ThreadFactoryBuilder().setNameFormat( "name-%d" ).build() );
            if( warming > 0 ) {
                System.out.println( "warming up..." );
                IntStream
                    .range( 0, warming )
                    .mapToObj( i -> pool.submit( benchmark.code ) )
                    .collect( toList() )
                    .forEach( Try.consume( Future::get ) );
            }
            System.out.println( "starting test..." );

            List<Result> results = IntStream
                .range( 0, benchmark.experiments )
                .mapToObj( x -> Teamcity.progress( benchmark.name + " e=" + x + "...", () -> {

                        benchmark.beforeExperiment.run();

                        int samplesPerThread = benchmark.samples / threads;

                        long start = System.nanoTime();
                        IntStream
                            .range( 0, threads )
                            .mapToObj( t -> pool.submit( () -> IntStream
                                .range( t * samplesPerThread, ( t + 1 ) * samplesPerThread )
                                .forEach( i -> benchmark.code.run() )

                            ) )
                            .collect( toList() )
                            .forEach( Try.consume( Future::get ) );

                        long total = System.nanoTime() - start;
                        Result result = benchmark.toResult( total );
                        benchmark.printResult( total, result );
                        benchmark.afterExperiment.run();

                        return result;
                    } )
                )
                .collect( toList() );

            Result avg = Result.average( results, benchmark.experiments );
            benchmark.printAverageResult( avg );

            Teamcity.performance( benchmark.name, avg.rate );

            return avg;
        } );

    }
}
