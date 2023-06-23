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
import io.prometheus.client.CollectorRegistry;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import org.apache.http.entity.ContentType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@EqualsAndHashCode
public class PrometheusExporter implements HttpHandler {
    private static final Map<String, PrometheusExporter> registeredInstances = new ConcurrentHashMap<>();
    public PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );

    static {
        CollectorRegistry.defaultRegistry.clear();
    }

    public PrometheusExporter( NioHttpServer server ) {
        server.bind( "/metrics", this );
        registeredInstances.put( UUID.randomUUID().toString(), this );
        Metrics.addRegistry( prometheusRegistry );
    }

    public PrometheusExporter( NioHttpServer server, int port ) {
        server.bind( "/metrics", this, port );
        registeredInstances.put( UUID.randomUUID().toString(), this );
        Metrics.addRegistry( prometheusRegistry );
    }

    public static void recreate() {
        CollectorRegistry.defaultRegistry.clear();
        registeredInstances.forEach( ( key, value ) -> {
            Metrics.removeRegistry( value.prometheusRegistry );
            value.prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );
            Metrics.addRegistry( value.prometheusRegistry );
        } );
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        var response = prometheusRegistry.scrape();
        exchange.responseOk( response, ContentType.TEXT_PLAIN.getMimeType() );
    }
}
