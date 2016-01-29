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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import oap.testng.AbstractTest;
import org.influxdb.dto.Point;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;

public class InfluxDBReporterTest extends AbstractTest {

    @Test
    public void testReport() throws Exception {
        DateTimeUtils.setCurrentMillisFixed( 1454055727921L );

        final MockInfluxDB influxDB = new MockInfluxDB();
        final MetricRegistry registry = new MetricRegistry();
        final InfluxDBReporter reporter = new InfluxDBReporter(
            influxDB,
            "database",
            emptyMap(),
            registry,
            "name",
            new ReporterFilter( emptyList(), emptyList() ),
            Arrays.asList( "test.*", "test2.test2.*" ),
            TimeUnit.DAYS,
            TimeUnit.DAYS
        );

        registry.counter( "test.name1" ).inc();
        registry.counter( "test.name2" ).inc( 2 );

        registry.register( "test2.test2.g1", ( Gauge ) () -> 10 );
        registry.register( "test2.test2.g2", ( Gauge ) () -> 10 );

        registry.histogram( "test.h1" ).update( 10 );
        registry.histogram( "test.h2" ).update( 20 );

        registry.meter( "test2.test2.m1" ).mark();
        registry.meter( "test2.test2.m2" ).mark();

        registry.timer( "test.t1" ).update( 10, TimeUnit.DAYS );
        registry.timer( "test.t2" ).update( 10, TimeUnit.HOURS );

        reporter.report(
            registry.getGauges(),
            registry.getCounters(),
            registry.getHistograms(),
            registry.getMeters(),
            registry.getTimers()
        );

        assertEquals( influxDB.writes.stream().map( Point::lineProtocol ).collect( joining( "\n" ) ),
            "test h1=10.00,h2=20.00,name1=1,name2=2,t1=10.00,t2=0.42 1454055727921000000\n" +
                "test2.test2 g1=10.00,g2=10.00,m1=0.00,m2=0.00 1454055727921000000" );
    }
}