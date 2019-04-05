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

import oap.net.Inet;

import java.lang.management.ManagementFactory;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 05.04.2019.
 *
 * filters: jmx-memory.*, jmx-gc.*
 */
public class JvmMetrics {
    private static final Pattern WHITESPACE = Pattern.compile( "[\\s]+" );

    public JvmMetrics() {
        var mxBean = ManagementFactory.getMemoryMXBean();
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_init" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getHeapMemoryUsage().getInit() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_used" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getHeapMemoryUsage().getUsed() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_max" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getHeapMemoryUsage().getMax() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.heap_committed" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getHeapMemoryUsage().getCommitted() );

        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_init" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getNonHeapMemoryUsage().getInit() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_used" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getNonHeapMemoryUsage().getUsed() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_max" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getNonHeapMemoryUsage().getMax() );
        Metrics.measureGauge( Metrics.name( "jmx-memory.non_heap_committed" ).tag( "host", Inet.HOSTNAME ),
            () -> mxBean.getNonHeapMemoryUsage().getCommitted() );

        var garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
        for( var gc : garbageCollectors ) {
            var name = WHITESPACE.matcher( gc.getName() ).replaceAll( "-" );

            Metrics.measureGauge( Metrics.name( "jmx-gc." + name + "_count" ).tag( "host", Inet.HOSTNAME ),
                gc::getCollectionCount );

            Metrics.measureGauge( Metrics.name( "jmx-gc." + name + "_time" ).tag( "host", Inet.HOSTNAME ),
                gc::getCollectionTime );
        }
    }
}
