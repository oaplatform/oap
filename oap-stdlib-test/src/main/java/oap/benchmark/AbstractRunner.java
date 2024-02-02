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

import lombok.extern.slf4j.Slf4j;
import oap.util.Lists;
import oap.util.function.Try;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static oap.benchmark.Benchmark.WARMING_EXPERIMENT;

@Slf4j
abstract class AbstractRunner {
    abstract Result runExperiment( int experiment, Benchmark benchmark );

    Result run( Benchmark benchmark ) {
        return Teamcity.progress( benchmark.name + "...", Try.supply( () -> {
            if( benchmark.warming > 0 ) {
                log.info( "warming up..." );
                for( var i = 0; i < benchmark.warming; i++ ) {
                    benchmark.code.accept( WARMING_EXPERIMENT, i );
                }
            }
            log.info( "starting test..." );

            List<Result> results = IntStream
                .range( 0, benchmark.experiments )
                .mapToObj( x -> Teamcity.progress( benchmark.name + " e=" + x + "...", Try.supply( () -> {

                        benchmark.beforeExperiment.run();

                        Result result = runExperiment( x, benchmark );

                        benchmark.printResult( result );
                        benchmark.afterExperiment.run();

                        return result;
                    } )
                ) )
                .collect( toList() );

            if( results.size() > 1 ) {
                System.out.println( "Experiment △" );
                double max = results.stream().mapToDouble( r -> r.rate ).max().orElseThrow();
                for( Result avgResult : Lists.reverse( results ) )
                    System.out.format( "%10s │" + " ".repeat(
                        max > 0 ? ( int ) ( avgResult.rate * results.size() / max ) : 0 ) + "○ %." + benchmark.precision + "f\n", avgResult.experiment, avgResult.rate );
                System.out.println( "           └" + "─".repeat( results.size() + 10 ) + "▷ Ops" );
            }

            Result avg = Result.average( results );
            benchmark.printAverageResult( avg );

            Teamcity.performance( benchmark.name, avg.rate );

            return avg;
        } ) );

    }
}
