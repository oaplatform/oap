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
package oap.metrics;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Timer;
import oap.util.BiStream;
import oap.util.Functions;
import oap.util.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Metrics {
    static final MetricRegistry registry = new MetricRegistry();

    private static Snapshot toSnapshot( String name, Metric value ) {
        Snapshot snapshot = new Snapshot( name );
        if( value instanceof Sampling )
            snapshot.mean = ( ( Sampling ) value ).getSnapshot().getMean();
        if( value instanceof Metered )
            snapshot.meanRate = ( ( Metered ) value ).getMeanRate();
        if( value instanceof Counting )
            snapshot.count = ( ( Counting ) value ).getCount();
        if( value instanceof Gauge )
            snapshot.count = ( ( Number ) ( ( Gauge ) value ).getValue() ).longValue();
        return snapshot;
    }

    public static void measureTimer( Name metric, Runnable code ) {
        try( Timer.Context ignored = registry.timer( metric.line ).time() ) {
            code.run();
        }
    }

    public static void measureTimer( Name metric, long duration, TimeUnit unit ) {
        registry.timer( metric.line ).update( duration, unit );
    }

    public static <T> void measureGauge( String metric, Supplier<T> get ) {
        registry.register( metric, ( Gauge ) get::get );
    }

    public static <T> void measureCachedGauge( String metric, long timeout, TimeUnit timeUnit, Supplier<T> get ) {
        registry.register( metric, new CachedGauge<T>( timeout, timeUnit ) {
            @Override
            protected T loadValue() {
                return get.get();
            }
        } );
    }

    public static <T> Name measureGauge( Name metric, Supplier<T> get ) {
        measureGauge( metric.line, get );
        return metric;
    }

    public static <T> Name measureCachedGauge( Name metric, long timeout, TimeUnit timeUnit, Supplier<T> get ) {
        registry.register( metric.line, new CachedGauge<T>( timeout, timeUnit ) {
            @Override
            protected T loadValue() {
                return get.get();
            }
        } );
        return metric;
    }

    public static <T> T measureTimer( String metric, Supplier<T> code ) {
        return measureTimer( name( metric ), code );
    }

    public static <T> T measureTimer( Name metric, Supplier<T> code ) {
        try( Timer.Context ignored = registry.timer( metric.line ).time() ) {
            return code.get();
        }
    }

    @Deprecated
    public static Timer.Context measureTimerCodehale( String metric ) {
        return registry.timer( name( metric ).line ).time();
    }

    public static void measureMeter( Name metric ) {
        registry.meter( MetricRegistry.name( metric.line ) ).mark();
    }

    public static void measureMeter( Name metric, long n ) {
        registry.meter( MetricRegistry.name( metric.line ) ).mark( n );
    }

    public static void measureHistogram( String metric, long value ) {
        measureHistogram( name( metric ), value );
    }

    public static void measureHistogram( Name metric, long value ) {
        registry.histogram( metric.line ).update( value );
    }

    @Deprecated
    public static <T extends Metric> T register( Name name, T metric ) {
        return registry.register( MetricRegistry.name( name.line ), metric );
    }

    public static void measureCounterIncrement( Name metric ) {
        registry.counter( metric.line ).inc();
    }

    public static void measureCounterIncrement( Name metric, long count ) {
        registry.counter( metric.line ).inc( count );
    }

    public static void measureCounterDecrement( Name metric ) {
        registry.counter( metric.line ).dec();
    }

    public static Name name( String measurement ) {
        return new Name( measurement );
    }

    public static void reset( Name metric ) {
        registry.remove( metric.line );
    }

    public static void resetAll() {
        registry.removeMatching( MetricFilter.ALL );
    }

    public static Snapshot snapshot( Name name ) {
        return snapshot( name.line );
    }

    public static Snapshot snapshot( String name ) {
        Metric metric = registry.getMetrics().get( name );
        return metric != null ? toSnapshot( name, metric ) : new Snapshot( name );
    }

    public static List<Snapshot> snapshots() {
        return snapshots( Functions.empty.accept() );
    }

    public static List<Snapshot> snapshots( Predicate<Pair<String, Metric>> filter ) {
        return BiStream.of( registry.getMetrics() )
            .filter( filter )
            .mapToObj( Metrics::toSnapshot )
            .toList();
    }

    public static void unregister( String metric ) {
        registry.remove( metric );
    }

    public static void unregister( Name metric ) {
        unregister( metric.line );
    }

    public static class Snapshot {
        public final String name;
        public double mean;
        public long count;
        public double meanRate;

        public Snapshot( String name ) {
            this.name = name;
        }
    }

}
