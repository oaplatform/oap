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

package oap.prometheus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import oap.http.Client;
import oap.http.server.nio.NioHttpServer;
import oap.prometheus.PrometheusExporter;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusExporterTest extends Fixtures {
    private static final Counter TEST_1 = Metrics.counter( "test1" );
    private final EnvFixture envFixture;

    public PrometheusExporterTest() {
        envFixture = fixture( new EnvFixture() );
    }

    @Test
    public void server() throws IOException {
        var port = envFixture.portFor( "prometheus" );
        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            var exporter = new PrometheusExporter( server );
            server.start();

            TEST_1.increment( 2 );
            var response = Client.DEFAULT.get( "http://localhost:" + port + "/metrics" ).contentString();
            assertThat( response ).contains( """
                # HELP test1_total \s
                # TYPE test1_total counter
                test1_total 2.0
                """ );
        }
    }

}
