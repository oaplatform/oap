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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Builder;
import oap.benchmark.Benchmark;
import oap.util.Try;
import org.joda.time.Period;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static oap.util.Functions.empty.consume;

public abstract class AbstractPerformance extends AbstractTest {

    public static final int WARMING = 1000;

    public static Benchmark benchmark( String name, int samples, Try.ThrowingRunnable code ) {
        return Benchmark.benchmark( name, samples, code );
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingIntConsumer code ) {
        return Benchmark.benchmark( name, samples, code );
    }

    @Deprecated
    public static BenchmarkConfiguration.BenchmarkConfigurationBuilder builder( String name ) {
        return BenchmarkConfiguration.builder().name( name );
    }

    @Deprecated
    private static BenchmarkResult benchmarkCallThread( BenchmarkConfiguration configuration, Try.ThrowingConsumer<Integer> code ) {
        return Teamcity.progress( configuration.name + "...", () -> {
            List<BenchmarkResult> results = IntStream.range( 0, configuration.experiments )
                .mapToObj( x -> {
                        configuration.initExperiment.accept( x );

                        BenchmarkResult progress = Teamcity.progress( configuration.name + " e=" + x + "...", () -> {
                                long total = IntStream.range( 0, configuration.samples ).mapToLong( time -> {
                                    long start = System.nanoTime();
                                    code.asConsumer().accept( time );
                                    return System.nanoTime() - start;
                                } ).sum();
                                long avg = total / configuration.samples / 1000;
                                final long rate = getRate( configuration, total );
                                System.out.format(
                                    "benchmarking %s: %d samples, %d usec, avg time %d usec, rate %s\n",
                                    configuration.name, configuration.samples, total / 1000, avg, rateToString( configuration, rate ) );
                                return new BenchmarkResult( avg, rate );
                            }
                        );

                        configuration.doneExperiment.accept( x );

                        return progress;
                    }
                )
                .collect( toList() );
            final long avgTime = results.stream()
                .skip( configuration.experiments > 1 ? 1 : 0 )
                .mapToLong( r -> r.time )
                .sum() / ( configuration.experiments > 1 ? configuration.experiments - 1 : configuration.experiments );
            long avgRate = results.stream()
                .skip( configuration.experiments > 1 ? 1 : 0 )
                .mapToLong( r -> r.rate )
                .sum() / ( configuration.experiments > 1 ? configuration.experiments - 1 : configuration.experiments );
            System.out.format( "benchmarking %s : avg time %d usec, avg rate %s\n",
                configuration.name, avgTime, rateToString( configuration, avgRate ) );

            Teamcity.performance( configuration.name, avgRate );

            return new BenchmarkResult( avgTime, avgRate );
        } );
    }

    private static String rateToString( BenchmarkConfiguration configuration, long avgRate ) {
        return configuration.rateToString.apply( avgRate ).replace( "${PERIOD}", configuration.getPeriod() );
    }

    /**
     * @see oap.benchmark.Benchmark
     */
    @Deprecated
    public static BenchmarkResult benchmark( BenchmarkConfiguration configuration, Try.ThrowingConsumer<Integer> code ) {
        if( configuration.threads <= 1 ) return benchmarkCallThread( configuration, code );

        return Teamcity.progress( configuration.name + "...", () -> {
            System.out.println( "pool threads = " + configuration.threads );
            ExecutorService pool = oap.concurrent.Executors.newFixedThreadPool( configuration.threads, new ThreadFactoryBuilder().setNameFormat( "name-%d" ).build() );
            if( configuration.warming > 0 ) {
                System.out.println( "warming up..." );
                IntStream
                    .range( 0, configuration.warming )
                    .mapToObj( i -> pool.submit( () -> code.asConsumer().accept( 0 ) ) )
                    .collect( toList() )
                    .forEach( Try.consume( Future::get ) );
            }
            System.out.println( "starting test..." );

            List<BenchmarkResult> results = IntStream
                .range( 0, configuration.experiments )
                .mapToObj( x -> Teamcity.progress( configuration.name + " e=" + x + "...", () -> {

                        configuration.initExperiment.accept( x );

                        int threadSamles = configuration.samples / configuration.threads;

                        long start = System.nanoTime();
                        IntStream
                            .range( 0, configuration.threads )
                            .mapToObj( t -> pool.submit( () -> IntStream
                                .range( t * threadSamles, ( t + 1 ) * threadSamles )
                                .forEach( code.asConsumer()::accept )

                            ) )
                            .collect( toList() )
                            .forEach( Try.consume( Future::get ) );

                        long total = System.nanoTime() - start;

                        long avg = total / configuration.samples / 1000;
                        final long rate = getRate( configuration, total );
                        System.out.format(
                            "benchmarking %s: %d samples, %d usec, avg time %d usec, rate %s\n",
                            configuration.name, configuration.samples, total / 1000, avg, rateToString( configuration, rate ) );

                        configuration.doneExperiment.accept( x );

                        return new BenchmarkResult( avg, rate );
                    } )
                )
                .collect( toList() );

            final long avgTime = results.stream()
                .skip( configuration.experiments > 1 ? 1 : 0 )
                .mapToLong( r -> r.time )
                .sum() / ( configuration.experiments > 1 ? configuration.experiments - 1 : configuration.experiments );
            final long avgRate = results.stream()
                .skip( configuration.experiments > 1 ? 1 : 0 )
                .mapToLong( r -> r.rate )
                .sum() / ( configuration.experiments > 1 ? configuration.experiments - 1 : configuration.experiments );
            System.out.format( "benchmarking %s : avg time %d usec, avg rate %s\n",
                configuration.name, avgTime, rateToString( configuration, avgRate ) );

            Teamcity.performance( configuration.name, avgRate );

            return new BenchmarkResult( avgTime, avgRate );
        } );
    }

    public static long getRate( BenchmarkConfiguration configuration, long total ) {
        return ( long ) ( configuration.samples / ( total / configuration.period.toStandardDuration().getMillis() / 1000000f ) );
    }

    public static <T> T time( String name, Supplier<T> code ) {
        long start = System.nanoTime();
        T result = code.get();
        System.out.println( name + " took " + ( ( System.nanoTime() - start ) / 1000 ) + " usec" );
        return result;
    }

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        Logger root = ( Logger ) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );
        root.setLevel( Level.INFO );

        super.beforeMethod();
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        Logger root = ( Logger ) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );
        root.setLevel( Level.TRACE );

        super.afterMethod();
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

    @Builder
    public static class BenchmarkConfiguration {
        private static final LongFunction<String> DEFAULT_RATE_TO_STRING = ( rate ) -> rate + " action/${PERIOD}";

        public final String name;
        public final int samples;
        public final int experiments;
        public final int threads;
        public final int warming;
        public final Period period;
        public final Consumer<Integer> initExperiment;
        public final Consumer<Integer> doneExperiment;
        public final LongFunction<String> rateToString;

        public String getPeriod() {
            final long millis = period.toStandardDuration().getMillis();
            if( millis == 1 ) return "ms";
            else if( millis == 1000 ) return "s";
            else if( millis == 1000 * 60 ) return "m";
            else if( millis == 1000 * 60 * 60 ) return "h";
            else return period.toString();
        }

        public static class BenchmarkConfigurationBuilder {
            public int experiments = 5;
            public Period period = Period.seconds( 1 );
            public int warming = WARMING;
            public Consumer<Integer> initExperiment = consume();
            public Consumer<Integer> doneExperiment = consume();
            public LongFunction<String> rateToString = DEFAULT_RATE_TO_STRING;
        }
    }
}
