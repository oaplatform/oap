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

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.metrics.Metrics;

@Slf4j
public class PrometheusExporter implements HttpHandler {
    private static long metricCount = 0L;

    static {
        Metrics.gaugeWithCallback( "system_metrics", callback -> callback.call( metricCount ) );
    }

    public PrometheusExporter( NioHttpServer server ) {
        server.bind( "/metrics", this );
    }

    public PrometheusExporter( NioHttpServer server, String port ) {
        server.bind( "/metrics", this, port );
    }

    private static long getMetricCount() {
        long count = 0;

        MetricSnapshots scrape = PrometheusRegistry.defaultRegistry.scrape();
        for( var a : scrape ) {
            count += a.getDataPoints().size();
        }

        return count;
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        metricCount = getMetricCount();

        exchange.exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, Http.ContentType.TEXT_PLAIN );

        new PrometheusTextFormatWriter( false )
            .write( exchange.getOutputStream(), PrometheusRegistry.defaultRegistry.scrape() );

        exchange.endExchange();
    }
}
