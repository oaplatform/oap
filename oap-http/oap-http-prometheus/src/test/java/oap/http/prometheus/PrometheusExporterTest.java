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

import io.prometheus.metrics.model.snapshots.Unit;
import oap.http.Client;
import oap.http.server.nio.NioHttpServer;
import oap.metrics.Metrics;
import oap.metrics.MetricsFixture;
import oap.testng.Fixtures;
import oap.testng.Ports;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusExporterTest extends Fixtures {
    public PrometheusExporterTest() {
        fixture( new MetricsFixture() );
    }

    @Test
    public void server() throws IOException {
        var port = Ports.getFreePort( getClass() );
        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            var exporter = new PrometheusExporter( server );

            var metric1 = Metrics.counter( "test1" );
            var metric2 = Metrics.histogram( "test2", Unit.SECONDS );

            server.start();

            metric1.inc( 2 );
            metric2.observe( 2 );

            var response = Client.DEFAULT.get( "http://localhost:" + port + "/metrics" ).contentString();
            assertThat( response ).contains( """
                # TYPE test1_total counter
                test1_total 2.0
                """ );
            assertThat( response ).contains( "test2_seconds_count 1" );
            assertThat( response ).contains( "test2_seconds_sum 2.0" );
            assertThat( response ).contains( "system_metrics 3.0" );
        }
    }
}
