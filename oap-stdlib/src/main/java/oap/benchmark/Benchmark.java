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
import oap.reflect.Reflect;
import oap.util.function.Try;
import org.joda.time.Period;

import java.util.function.IntConsumer;
import java.util.function.LongFunction;

import static oap.util.function.Functions.empty.noop;

@ToString( exclude = "code" )
public final class Benchmark {
    public int experiments = 5;
    public Period period = Period.seconds( 1 );
    String name;
    int samples;
    IntConsumer code;
    AbstractRunner runner = SingleThreadRunner.INSTANCE;
    Runnable beforeExperiment = noop();
    Runnable afterExperiment = noop();
    LongFunction<String> rateToString = rate -> rate + " action/${PERIOD}";

    private Benchmark( String name, int samples, Try.ThrowingIntConsumer code ) {
        this.name = name;
        this.samples = samples;
        this.code = code.asConsumer();
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingIntConsumer code ) {
        return new Benchmark( Reflect.caller( 1 ).getSimpleName() + "#" + name, samples, code );
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingRunnable code ) {
        return new Benchmark( Reflect.caller( 1 ).getSimpleName() + "#" + name, samples, i -> code.run() );
    }

    private static long getRate( int samples, Period period, long total ) {
        return ( long ) ( samples / ( total / period.toStandardDuration().getMillis() / 1000000f ) );
    }

    public Benchmark inThreads( int threads ) {
        return inThreads( threads, 1000 );
    }

    public Benchmark inThreads( int threads, int warming ) {
        this.runner = threads > 1 ? new MultiThreadRunner( threads, warming ) : SingleThreadRunner.INSTANCE;
        return this;
    }

    public Benchmark experiments( int experiments ) {
        this.experiments = experiments;
        return this;
    }

    public Benchmark beforeExpetriment( Try.CatchingRunnable before ) {
        this.beforeExperiment = before;
        return this;
    }

    public Benchmark afterExperiment( Try.CatchingRunnable after ) {
        this.afterExperiment = after;
        return this;
    }

    public Benchmark period( Period period ) {
        this.period = period;
        return this;
    }

    private String getPeriod() {
        final long millis = period.toStandardDuration().getMillis();
        if( millis == 1 ) return "ms";
        else if( millis == 1000 ) return "s";
        else if( millis == 1000 * 60 ) return "m";
        else if( millis == 1000 * 60 * 60 ) return "h";
        else return period.toString();
    }

    private String rateToString( long avgRate ) {
        return rateToString.apply( avgRate ).replace( "${PERIOD}", getPeriod() );
    }

    void printResult( long totalTime, Result result ) {
        System.out.format(
            "benchmarking %s: %d samples, %d usec, avg time %d usec, rate %s\n",
            name, samples, totalTime / 1000, result.time, rateToString( result.rate ) );
    }

    void printAverageResult( Result result ) {
        System.out.format( "benchmarking %s: avg time %d usec, avg rate %s\n",
            name, result.time, rateToString( result.rate ) );

    }

    Result toResult( long total ) {
        return new Result( total / samples / 1000, getRate( samples, period, total ) );
    }

    public void run() {
        runner.run( this );
    }
}
