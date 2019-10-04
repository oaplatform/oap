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

import lombok.val;

import java.lang.management.ManagementFactory;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 05.04.2019.
 * <p>
 * filters: jmx-memory.*, jmx-gc.*
 */
public class JvmMetrics {
    private static final Pattern WHITESPACE = Pattern.compile( "[\\s]+" );

    public JvmMetrics() {
        val mxBean = ManagementFactory.getMemoryMXBean();
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_init" ),
            () -> mxBean.getHeapMemoryUsage().getInit() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_used" ),
            () -> mxBean.getHeapMemoryUsage().getUsed() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_max" ),
            () -> mxBean.getHeapMemoryUsage().getMax() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_committed" ),
            () -> mxBean.getHeapMemoryUsage().getCommitted() );

        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_init" ),
            () -> mxBean.getNonHeapMemoryUsage().getInit() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_used" ),
            () -> mxBean.getNonHeapMemoryUsage().getUsed() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_max" ),
            () -> mxBean.getNonHeapMemoryUsage().getMax() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_committed" ),
            () -> mxBean.getNonHeapMemoryUsage().getCommitted() );

        val garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
        for( val gc : garbageCollectors ) {
            val name = WHITESPACE.matcher( gc.getName() ).replaceAll( "_" );

            Metrics.measureGauge( Metrics.name( "jmx-gc." + name + "_count" ), gc::getCollectionCount );
            Metrics.measureGauge( Metrics.name( "jmx-gc." + name + "_time" ), gc::getCollectionTime );
        }
    }
}
