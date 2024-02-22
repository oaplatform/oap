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

import io.micrometer.core.instrument.Metrics;
import oap.http.Client;
import oap.http.server.nio.NioHttpServer;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusExporterTest extends Fixtures {
    private final EnvFixture envFixture;

    public PrometheusExporterTest() {
        envFixture = fixture( new EnvFixture() );
    }

    @Test
    public void server() throws IOException {
        var port = envFixture.portFor( "prometheus" );
        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            var exporter = new PrometheusExporter( server );

            var metric1 = Metrics.counter( "test1" );
            var metric2 = Metrics.timer( "test2" );

            server.start();

            metric1.increment( 2 );
            metric2.record( 2, TimeUnit.SECONDS );

            var response = Client.DEFAULT.get( "http://localhost:" + port + "/metrics" ).contentString();
            assertThat( response ).contains( """
                # HELP test1_total \s
                # TYPE test1_total counter
                test1_total 2.0
                """ );
            assertThat( response ).contains( "test2_seconds_count 1.0" );
            assertThat( response ).contains( "test2_seconds_max 2.0" );
            assertThat( response ).contains( "system_metrics_total 5.0" );
        }
    }

    @AfterMethod
    public void beforeMethod() {
        PrometheusExporter.prometheusRegistry.getPrometheusRegistry().clear();
    }

    @AfterMethod
    public void afterMethod() {
        PrometheusExporter.prometheusRegistry.getPrometheusRegistry().clear();
    }
}
