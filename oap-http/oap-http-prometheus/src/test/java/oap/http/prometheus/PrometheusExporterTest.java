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

package oap.http.prometheus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.testng.Ports;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static oap.http.test.HttpAsserts.assertGet;

public class PrometheusExporterTest extends Fixtures {
    private static void clear() {
        for( Meter meter : Search.in( PrometheusExporter.prometheusRegistry ).meters() ) {
            PrometheusExporter.prometheusRegistry.remove( meter );
        }
    }

    @Test
    public void server() throws Exception {
        int port = Ports.getFreePort( getClass() );
        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            var exporter = new PrometheusExporter( server );

            Counter metric1 = Metrics.counter( "test1" );
            Timer metric2 = Metrics.timer( "test2" );

            server.start();

            metric1.increment( 2 );
            metric2.record( 2, TimeUnit.SECONDS );

            assertGet( "http://localhost:" + port + "/metrics" )
                .body()
                .contains( """
                    # HELP test1_total \s
                    # TYPE test1_total counter
                    test1_total 2.0
                    """ )
                .contains( "test2_seconds_count 1" )
                .contains( "test2_seconds_max 2.0" )
                .contains( "system_metrics 5" );
        }
    }

    @AfterMethod
    public void beforeMethod() {
        clear();
    }

    @AfterMethod
    public void afterMethod() {
        clear();
    }
}
