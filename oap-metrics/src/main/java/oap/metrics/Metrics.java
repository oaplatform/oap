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

import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Timer;
import oap.util.PairStream;

import java.util.List;
import java.util.function.Supplier;

public class Metrics {
    final MetricRegistry registry = new MetricRegistry();

    private static Snapshot toSnapshot( String name, Metric value ) {
        Snapshot snapshot = new Snapshot( name );
        if( value instanceof Sampling )
            snapshot.mean = ((Sampling) value).getSnapshot().getMean();
        if( value instanceof Metered )
            snapshot.meanRate = ((Metered) value).getMeanRate();
        if( value instanceof Counting )
            snapshot.count = ((Counting) value).getCount();
        return snapshot;
    }

    public void measureTimer( Name metric, Runnable code ) {
        try( Timer.Context ignored = registry.timer( metric.line ).time() ) {
            code.run();
        }
    }

    public <T> void measureGauge( String metric, Supplier<T> get ) {
        registry.register( metric, (Gauge<T>) get::get );
    }

    public <T> T measureTimer( String metric, Supplier<T> code ) {
        return measureTimer( name( metric ), code );
    }

    public <T> T measureTimer( Name metric, Supplier<T> code ) {
        try( Timer.Context ignored = registry.timer( metric.line ).time() ) {
            return code.get();
        }
    }

    public Timer.Context measureTimerCodehale( String metric ) {
        return registry.timer( name( metric ).line ).time();
    }

    public void measureMeter( Name metric ) {
        registry.meter( MetricRegistry.name( metric.line ) ).mark();
    }

    public void measureHistogram( String metric, long count ) {
        measureHistogram( name( metric ), count );
    }

    public void measureHistogram( Name metric, long count ) {
        registry.histogram( metric.line ).update( count );
    }

    public void measureCounterIncrement( Name metric ) {
        registry.counter( metric.line ).inc();
    }

    public void measureCounterDecrement( Name metric ) {
        registry.counter( metric.line ).dec();
    }

    public static Name name( String measurement ) {
        return new Name( measurement );
    }

    public void reset( Name metric ) {
        registry.remove( metric.line );
    }

    public void resetAll() {
        registry.removeMatching( MetricFilter.ALL );
    }

    public Snapshot snapshot( Name name ) {
        return snapshot( name.line );
    }

    public Snapshot snapshot( String name ) {
        Metric metric = registry.getMetrics().get( name );
        return metric != null ? toSnapshot( name, metric ) : new Snapshot( name );
    }

    public List<Snapshot> snapshots() {
        return PairStream.of( registry.getMetrics() ).mapToObj( Metrics::toSnapshot ).toList();
    }

    public void unregister( String metric ) {
        registry.remove( metric );
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
