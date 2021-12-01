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

package oap.http.server.nio;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.conduits.InflatingStreamSourceConduit;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.encoding.RequestEncodingHandler;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.health.HealthHttpHandler;
import org.xnio.Options;

import java.net.InetSocketAddress;

@Slf4j
public class NioHttpServer {
    public final int port;
    private final PathHandler pathHandler;

    public int backlog = -1;
    public long idleTimeout = -1;
    public boolean tcpNodelay = true;
    public int ioThreads = -1; // min(2, Runtime.getRuntime().availableProcessors())
    public int workerThreads = -1; // Runtime.getRuntime().availableProcessors() * 2
    public long maxEntitySize = -1;
    public int maxParameters = -1;
    public int maxHeaders = -1;
    public int maxHeaderSize = -1;
    public boolean statistics = false;
    public Undertow server;
    public boolean compressionSupport = true;
    protected HealthHttpHandler healthHttpHandler;

    public NioHttpServer( int port ) {
        this.port = port;

        pathHandler = new PathHandler();
    }

    public void start() {
        Undertow.Builder builder = Undertow.builder()
            .addHttpListener( port, "0.0.0.0" )

            .setSocketOption( Options.REUSE_ADDRESSES, true )
            .setSocketOption( Options.TCP_NODELAY, tcpNodelay )

            .setServerOption( UndertowOptions.ALWAYS_SET_KEEP_ALIVE, true )
            .setServerOption( UndertowOptions.RECORD_REQUEST_START_TIME, true );

        if( backlog > 0 ) builder.setSocketOption( Options.BACKLOG, backlog );
        if( ioThreads > 0 ) builder.setIoThreads( ioThreads );
        if( workerThreads > 0 ) builder.setWorkerThreads( workerThreads );
        if( idleTimeout > 0 ) builder.setServerOption( UndertowOptions.IDLE_TIMEOUT, ( int ) idleTimeout );
        if( maxEntitySize > 0 ) builder.setServerOption( UndertowOptions.MAX_ENTITY_SIZE, maxEntitySize );
        if( maxParameters > 0 ) builder.setServerOption( UndertowOptions.MAX_PARAMETERS, maxParameters );
        if( maxHeaders > 0 ) builder.setServerOption( UndertowOptions.MAX_HEADERS, maxHeaders );
        if( maxHeaderSize > 0 ) builder.setServerOption( UndertowOptions.MAX_HEADER_SIZE, maxHeaderSize );
        if( statistics ) builder.setServerOption( UndertowOptions.ENABLE_STATISTICS, true );

        if( statistics ) {
            for( var listenerInfo : server.getListenerInfo() ) {
                var sa = ( InetSocketAddress ) listenerInfo.getAddress();
                var port = String.valueOf( sa.getPort() );

                ConnectorStatistics connectorStatistics = listenerInfo.getConnectorStatistics();

                Metrics.gauge( "nio_requests", Tags.of( "port", port, "type", "total" ), connectorStatistics, ConnectorStatistics::getRequestCount );
                Metrics.gauge( "nio_requests", Tags.of( "port", port, "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveRequests );
                Metrics.gauge( "nio_requests", Tags.of( "port", port, "type", "errors" ), connectorStatistics, ConnectorStatistics::getErrorCount );

                Metrics.gauge( "nio_connections", Tags.of( "port", port, "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveConnections );

                Metrics.gauge( "nio_pool_size", Tags.of( "port", port, "name", "worker", "type", "active" ), server, server -> server.getWorker().getMXBean().getWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", port, "name", "worker", "type", "core" ), server, server -> server.getWorker().getMXBean().getCoreWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", port, "name", "worker", "type", "max" ), server, server -> server.getWorker().getMXBean().getMaxWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", port, "name", "worker", "type", "busy" ), server, server -> server.getWorker().getMXBean().getBusyWorkerThreadCount() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", port, "name", "worker", "type", "queue" ), server, server -> server.getWorker().getMXBean().getWorkerQueueSize() );

            }
        }

        io.undertow.server.HttpHandler handler = pathHandler;
        if( compressionSupport ) {
            ContentEncodingRepository contentEncodingRepository = new ContentEncodingRepository();
            contentEncodingRepository.addEncodingHandler( "gzip", new GzipEncodingProvider(), 100 );
            contentEncodingRepository.addEncodingHandler( "deflate", new DeflateEncodingProvider(), 100 );
            handler = new EncodingHandler( handler, contentEncodingRepository );
            handler = new RequestEncodingHandler( handler )
                .addEncoding( "gzip", GzipStreamSourceConduit.WRAPPER )
                .addEncoding( "deflate", InflatingStreamSourceConduit.WRAPPER );
        }

        handler = new BlockingHandler( handler );

        builder.setHandler( handler );
        if( healthHttpHandler != null )
            bind( "/health", healthHttpHandler );

        server = builder.build();
        server.start();

        log.info( "port {} statistics {} ioThreads {} workerThreads {}",
            port, statistics,
            server.getWorker().getMXBean().getIoThreadCount(),
            server.getWorker().getMXBean().getMaxWorkerPoolSize()
        );
    }

    public void bind( String prefix, HttpHandler handler ) {
        pathHandler.addPrefixPath( prefix,
            exchange -> handler.handleRequest( new HttpServerExchange( exchange ) )
        );
    }

    public void unbind( String prefix ) {
        pathHandler.removePrefixPath( prefix );
    }

    public void preStop() {
        server.stop();
    }
}
