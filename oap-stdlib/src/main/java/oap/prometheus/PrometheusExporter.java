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

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
public class PrometheusExporter implements Closeable {
    public static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );

    static {
        Metrics.addRegistry( prometheusRegistry );
    }

    private final HttpServer server;

    public PrometheusExporter( int port ) throws IOException {
        this( port, "/metrics" );
    }

    public PrometheusExporter( int port, String path ) throws IOException {
        log.info( "Prometheus metrics port/path {}/{}", port, path );

        server = HttpServer.create( new InetSocketAddress( port ), 0 );
        server.createContext( path, httpExchange -> {
            var response = prometheusRegistry.scrape();
            httpExchange.sendResponseHeaders( 200, response.getBytes().length );
            try( var os = httpExchange.getResponseBody() ) {
                os.write( response.getBytes() );
            }
        } );
    }

    public void start() {
        server.start();
    }

    @Override
    public void close() throws IOException {
        server.stop( 1 );
    }
}
