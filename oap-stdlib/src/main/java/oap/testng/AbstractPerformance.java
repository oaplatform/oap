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
package oap.testng;

import oap.util.Try;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.testng.Assert;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractPerformance extends AbstractTest {

    public static final int WARMING = 1000;
    protected final static Consumer<Integer> none = ( i ) -> {
    };
    private final static Function<Long, String> actions_s = ( rate ) -> rate + " action/s";
    public static RandomDataGenerator random = new RandomDataGenerator();

    public static void benchmark( String name, int samples, Try.ThrowingConsumer<Integer> code ) {
        benchmark( name, samples, 5, code, none, none, actions_s );
    }

    public static BenchmarkResult benchmark( String name, int samples, int experiments,
        Try.ThrowingConsumer<Integer> code ) {
        return benchmark( name, samples, experiments, code, none, none, actions_s );
    }

    public static BenchmarkResult benchmark( String name, int samples, int experiments,
        Try.ThrowingConsumer<Integer> code,
        Consumer<Integer> initExperiment, Consumer<Integer> doneExperiment ) {
        return benchmark( name, samples, experiments, code, initExperiment, doneExperiment, actions_s );
    }

    public static BenchmarkResult benchmark( String name, int samples, int experiments,
        Try.ThrowingConsumer<Integer> code,
        Consumer<Integer> initExperiment, Consumer<Integer> doneExperiment, Function<Long, String> rateToString ) {
        return Teamcity.progress( name + "...", () -> {
            List<BenchmarkResult> results = IntStream.range( 0, experiments )
                .mapToObj( x -> {
                        initExperiment.accept( x );

                        BenchmarkResult progress = Teamcity.progress( name + " e=" + x + "...", () -> {
                                long total = IntStream.range( 0, samples ).mapToLong( time -> {
                                    long start = System.nanoTime();
                                    code.asConsumer().accept( time );
                                    return System.nanoTime() - start;
                                } ).sum();
                                long avg = total / samples / 1000;
                                long rate = (long) (samples / (total / 1000000000f));
                                System.out.format(
                                    "benchmarking %s: %d samples, %d usec, avg time %d usec, rate %s\n",
                                    name, samples, total / 1000, avg, rateToString.apply( rate ) );
                                return new BenchmarkResult( avg, rate );
                            }
                        );

                        doneExperiment.accept( x );

                        return progress;
                    }
                )
                .collect( toList() );
            long avgTime = results.stream()
                .skip( 1 )
                .mapToLong( r -> r.time )
                .sum() / (experiments - 1);
            long avgRate = results.stream()
                .skip( 1 )
                .mapToLong( r -> r.rate )
                .sum() / (experiments - 1);
            System.out.format( "benchmarking %s : avg time %d usec, avg rate %s\n",
                name, avgTime, rateToString.apply( avgRate ) );

            Teamcity.performance( name, avgRate );

            return new BenchmarkResult( avgTime, avgRate );
        } );
    }

    public static BenchmarkResult benchmark( String name, int samples, int experiments, int threads,
        Try.ThrowingConsumer<Integer> code ) {
        return benchmark( name, samples, experiments, threads, code, WARMING );
    }

    public static BenchmarkResult benchmark( String name, int samples, int experiments, int threads,
        Try.ThrowingConsumer<Integer> code, int warming ) {

        return Teamcity.progress( name + "...", () -> {
            System.out.println( "pool threads = " + threads );
            ExecutorService pool = Executors.newFixedThreadPool( threads );
            if( warming > 0 ) {
                System.out.println( "warming up..." );
                IntStream
                    .range( 0, warming )
                    .mapToObj( i -> pool.submit( () -> code.asConsumer().accept( 0 ) ) )
                    .forEach( Try.consume( Future::get ) );
            }
            System.out.println( "starting test..." );

            List<BenchmarkResult> results = IntStream
                .range( 0, experiments )
                .mapToObj( x -> Teamcity.progress( name + " e=" + x + "...", () -> {

                        int threadSamles = samples / threads;

                        long start = System.nanoTime();
                        IntStream
                            .range( 0, threads )
                            .mapToObj( t -> pool.submit( () -> IntStream
                                .range( t * threadSamles, (t + 1) * threadSamles )
                                .forEach( code.asConsumer()::accept )

                            ) )
                            .collect( toList() )
                            .stream()
                            .forEach( Try.consume( Future::get ) );

                        long total = System.nanoTime() - start;

                        long avg = total / samples / 1000;
                        long rate = (long) (samples / (total / 1000000000f));
                        System.out.format(
                            "benchmarking %s: %d samples, %d usec, avg time %d usec, rate %d actions/s\n",
                            name, samples, total / 1000, avg, rate );
                        return new BenchmarkResult( avg, rate );
                    } )
                )
                .collect( toList() );

            long avgTime = results.stream()
                .skip( 1 )
                .mapToLong( r -> r.time )
                .sum() / (experiments - 1);
            long avgRate = results.stream()
                .skip( 1 )
                .mapToLong( r -> r.rate )
                .sum() / (experiments - 1);
            System.out.format( "benchmarking %s : avg time %d usec, avg rate %d actions/s\n",
                name, avgTime, avgRate );

            Teamcity.performance( name, avgRate );

            return new BenchmarkResult( avgTime, avgRate );
        } );
    }

    public static <T> T time( String name, Supplier<T> code ) {
        long start = System.nanoTime();
        T result = code.get();
        System.out.println( name + " took " + ((System.nanoTime() - start) / 1000) + " usec" );
        return result;
    }

    public static class BenchmarkResult {
        public long rate;
        public long time;

        public BenchmarkResult( long time, long rate ) {
            this.time = time;
            this.rate = rate;
        }

        public void assertPerformace( long time, long rate ) {
            Assert.assertTrue( this.time < time, "expected time < " + time + " but was " + this.time + "." );
            Assert.assertTrue( this.rate > rate, "expected rate > " + rate + " but was " + this.rate + "." );
        }
    }
}
