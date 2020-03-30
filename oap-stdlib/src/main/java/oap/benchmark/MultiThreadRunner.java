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
            if( warming > 0 ) {
                System.out.println( "warming up..." );
                var pool = oap.concurrent.Executors.newFixedThreadPool( threads, new ThreadFactoryBuilder().setNameFormat( "name-%d" ).build() );
                for( var i = 0; i < warming; i++ ) {
                    pool.execute( () -> benchmark.code.accept( 0 ) );
                }

                pool.shutdown();
                pool.awaitTermination( 5, TimeUnit.HOURS );
            }
            System.out.println( "starting test..." );

            List<Result> results = IntStream
                .range( 0, benchmark.experiments )
                .mapToObj( x -> Teamcity.progress( benchmark.name + " e=" + x + "...", Try.supply( () -> {

                        benchmark.beforeExperiment.run();

                        int samplesPerThread = benchmark.samples / threads;

                        var rpool = oap.concurrent.Executors.newFixedThreadPool( threads, new ThreadFactoryBuilder().setNameFormat( "name-%d" ).build() );

                        long start = System.nanoTime();
                        for( var t = 0; t < threads; t++ ) {
                            int finalT = t;
                            rpool.execute( () -> IntStream
                                .range( finalT * samplesPerThread, ( finalT + 1 ) * samplesPerThread )
                                .forEach( benchmark.code ) );

                        }
                        rpool.shutdown();
                        rpool.awaitTermination( 5, TimeUnit.HOURS );

                        long total = System.nanoTime() - start;
                        Result result = benchmark.toResult( total );
                        benchmark.printResult( total, result );
                        benchmark.afterExperiment.run();

                        return result;
                    } )
                ) )
                .collect( toList() );

            Result avg = Result.average( results, benchmark.experiments );
            benchmark.printAverageResult( avg );

            Teamcity.performance( benchmark.name, avg.rate );

            return avg;
        } ) );

    }
}
