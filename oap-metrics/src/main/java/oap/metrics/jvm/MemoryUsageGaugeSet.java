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

package oap.metrics.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

public class MemoryUsageGaugeSet implements MetricSet {
    private final MemoryMXBean mxBean;

    public MemoryUsageGaugeSet() {
        mxBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put( "total.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit() +
            mxBean.getNonHeapMemoryUsage().getInit() );

        gauges.put( "total.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed() +
            mxBean.getNonHeapMemoryUsage().getUsed() );

        gauges.put( "total.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax() +
            mxBean.getNonHeapMemoryUsage().getMax() );

        gauges.put( "total.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted() +
            mxBean.getNonHeapMemoryUsage().getCommitted() );


        gauges.put( "heap.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit() );

        gauges.put( "heap.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed() );

        gauges.put( "heap.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax() );

        gauges.put( "heap.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted() );

        gauges.put( "heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getHeapMemoryUsage();
                return Ratio.of( usage.getUsed(), usage.getMax() );
            }
        } );

        gauges.put( "non-heap.init", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getInit() );

        gauges.put( "non-heap.used", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getUsed() );

        gauges.put( "non-heap.max", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getMax() );

        gauges.put( "non-heap.committed", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getCommitted() );

        gauges.put( "non-heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getNonHeapMemoryUsage();
                return Ratio.of( usage.getUsed(), usage.getMax() );
            }
        } );

        return gauges;
    }
}
