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

import java.util.function.BiConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import static oap.util.function.Functions.empty.noop;

@ToString( exclude = "code" )
public final class Benchmark {
    public static final int DEFAULT_WARMING = 1000;
    public static final int WARMING_EXPERIMENT = -1;
    public static final Function<Integer, String> DEFAULT_EXPERIMENT_FORMAT = x -> "" + x;
    public int experiments = 5;
    public Period period = Period.seconds( 1 );
    String name;
    int warming = DEFAULT_WARMING;
    int samples;
    int precision = 4;
    BiConsumer<Integer, Integer> code;
    AbstractRunner runner = SingleThreadRunner.INSTANCE;
    Runnable beforeExperiment = noop();
    Runnable afterExperiment = noop();
    DoubleFunction<String> rateToString = rate -> String.format( "%." + precision + "f action/${PERIOD}", rate );
    Function<Integer, String> formatExperiment = DEFAULT_EXPERIMENT_FORMAT;

    private Benchmark( String name, int samples, Try.ThrowingBiConsumer<Integer, Integer> code ) {
        this.name = name;
        this.samples = samples;
        this.code = code.asConsumer();
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingIntConsumer code ) {
        return new Benchmark( Reflect.caller( 1 ).getSimpleName() + "#" + name, samples, ( x, s ) -> code.accept( s ) );
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingBiConsumer<Integer, Integer> code ) {
        return new Benchmark( Reflect.caller( 1 ).getSimpleName() + "#" + name, samples, code );
    }

    public static Benchmark benchmark( String name, int samples, Try.ThrowingRunnable code ) {
        return new Benchmark( Reflect.caller( 1 ).getSimpleName() + "#" + name, samples, ( x, s ) -> code.run() );
    }

    private static double getRate( int samples, Period period, long total ) {
        return samples / ( ( double ) total / period.toStandardDuration().getMillis() / 1000000d );
    }

    @Deprecated
    public Benchmark inThreads( int threads ) {
        return threads( threads );
    }

    @Deprecated
    public Benchmark inThreads( int threads, int warming ) {
        return threads( threads ).warming( warming );
    }

    public Benchmark threads( int threads ) {
        this.runner = threads > 1 ? new MultiThreadRunner( threads ) : SingleThreadRunner.INSTANCE;
        return this.formatExperiment == DEFAULT_EXPERIMENT_FORMAT
            ? this.formatExperiment( x -> x + "(" + threads + "thds)" ) : this;
    }

    public Benchmark warming( int warming ) {
        this.warming = warming;
        return this;
    }

    public Benchmark experiments( int experiments ) {
        this.experiments = experiments;
        return this;
    }

    public Benchmark precision( int precision ) {
        this.precision = precision;

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

    public Benchmark formatExperiment( Function<Integer, String> formatter ) {
        this.formatExperiment = formatter;
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

    private String rateToString( double avgRate ) {
        return rateToString.apply( avgRate ).replace( "${PERIOD}", getPeriod() );
    }

    void printResult( Result result ) {
        System.out.format( "benchmarking %s: experiment %s, %d samples, %d usec, avg time %d usec, rate %s\n",
            name, result.experiment, samples, result.total / 1000, result.time, rateToString( result.rate ) );
    }

    void printAverageResult( Result result ) {
        System.out.format( "benchmarking %s: avg time %d usec, avg rate %s\n",
            name, result.time, rateToString( result.rate ) );

    }

    Result toResult( int experiment, long total ) {
        return new Result( formatExperiment.apply( experiment ), total, total / samples / 1000, getRate( samples, period, total ) );
    }

    public Result run() {
        return runner.run( this );
    }
}
