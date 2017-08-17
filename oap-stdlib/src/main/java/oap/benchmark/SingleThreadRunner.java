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

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by razer on 6/9/17.
 */
@ToString
class SingleThreadRunner extends Runner {
    static SingleThreadRunner INSTANCE = new SingleThreadRunner();

    @Override
    public Result run( Benchmark benchmark ) {
        return Teamcity.progress( benchmark.name + "...", () -> {
            List<Result> results = IntStream.range( 0, benchmark.experiments )
                .mapToObj( x -> Teamcity.progress( benchmark.name + " e=" + x + "...", () -> {
                    benchmark.beforeExperiment.run();
                    long total = IntStream.range( 0, benchmark.samples )
                        .mapToLong( time -> {
                            long start = System.nanoTime();
                            benchmark.code.accept( time );
                            return System.nanoTime() - start;
                        } ).sum();

                    Result r = benchmark.toResult( total );
                    benchmark.printResult( total, r );
                    benchmark.afterExperiment.run();
                    return r;
                } ) )
                .collect( toList() );
            Result avg = Result.average( results, benchmark.experiments );
            benchmark.printAverageResult( avg );

            Teamcity.performance( benchmark.name, avg.rate );

            return avg;
        } );
    }

}
