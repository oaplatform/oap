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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;

@Slf4j
public class PrometheusExporter implements HttpHandler {
    public static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );
    private static long metricCount = 0L;

    static {
        Metrics.addRegistry( prometheusRegistry );

        Metrics.gauge( "system_metrics_total", prometheusRegistry, pmr -> metricCount );
    }

    public PrometheusExporter( NioHttpServer server ) {
        server.bind( "/metrics", this );
    }

    public PrometheusExporter( NioHttpServer server, String port ) {
        server.bind( "/metrics", this, port );
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        metricCount = getMetricCount();

        var response = prometheusRegistry.scrape();
        exchange.responseOk( response, TextFormat.CONTENT_TYPE_004 );
    }

    private static long getMetricCount() {
        long count = 0;
        var en = prometheusRegistry.getPrometheusRegistry().metricFamilySamples();
        while( en.hasMoreElements() ) {
            var s = en.nextElement();
            count += s.samples.size();
        }
        return count;
    }
}
