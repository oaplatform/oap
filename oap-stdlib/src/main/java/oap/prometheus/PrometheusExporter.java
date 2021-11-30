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
import lombok.extern.slf4j.Slf4j;
import oap.http.server.undertow.UndertowHttpServer;
import org.apache.http.entity.ContentType;

import java.io.Closeable;

@Slf4j
public class PrometheusExporter implements Closeable {
    public static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );

    static {
        Metrics.addRegistry( prometheusRegistry );
    }

    private final UndertowHttpServer server;

    public PrometheusExporter( int port ) {
        this( port, "/metrics" );
    }

    public PrometheusExporter( int port, String path ) {
        log.info( "Prometheus metrics port/path {}/{}", port, path );

        server = new UndertowHttpServer( port );
        server.ioThreads = 2;
        server.workerThreads = 4;
        server.bind( path, exchange -> {
            var response = prometheusRegistry.scrape();
            exchange.ok( response, ContentType.TEXT_PLAIN.getMimeType() );
        } );
    }

    public void start() {
        server.start();
    }

    public void preStop() {
        server.preStop();
    }

    @Override
    public void close() {
        server.preStop();
    }
}
