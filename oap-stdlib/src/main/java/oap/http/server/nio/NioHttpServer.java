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

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.conduits.InflatingStreamSourceConduit;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.BlockingReadTimeoutHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.encoding.RequestEncodingHandler;
import lombok.extern.slf4j.Slf4j;
import oap.util.Dates;
import org.jetbrains.annotations.NotNull;
import org.xnio.Options;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class NioHttpServer implements Closeable, AutoCloseable {
    private final int defaultPort;

    private final Map<Integer, PathHandler> pathHandler = new HashMap<>();
    private final Map<Integer, Undertow> servers = new HashMap<>();

    private final ContentEncodingRepository contentEncodingRepository;
    private final AtomicLong requestId = new AtomicLong();
    public int backlog = -1;
    public long idleTimeout = -1;
    public boolean tcpNodelay = true;
    public int ioThreads = -1; // default = max(2, Runtime.getRuntime().availableProcessors())
    public int workerThreads = -1; // default = Runtime.getRuntime().availableProcessors() * 8
    public long maxEntitySize = -1; // default = unlimited
    public int maxParameters = -1; // default = 1000
    public int maxHeaders = -1; // default = 200
    public int maxHeaderSize = -1; // default = 1024 * 1024
    public boolean statistics = false;
    public boolean forceCompressionSupport = false;
    public boolean alwaysSetDate = true;
    public boolean alwaysSetKeepAlive = true;
    public long readTimeout = Dates.s( 60 );

    public NioHttpServer( int port ) {
        this.defaultPort = port;

        pathHandler.put( defaultPort, new PathHandler() );

        contentEncodingRepository = new ContentEncodingRepository();
        contentEncodingRepository.addEncodingHandler( "gzip", new GzipEncodingProvider(), 100 );
        contentEncodingRepository.addEncodingHandler( "deflate", new DeflateEncodingProvider(), 50 );
    }

    public void start() {
        pathHandler.forEach( this::startNewPort );
    }

    private void startNewPort( int port, PathHandler portPathHandler ) {
        Preconditions.checkNotNull( portPathHandler );

        log.info( "starting server port {} with {} ...", port, portPathHandler.toString() );
        long time = System.currentTimeMillis();
        Undertow server = createUndertowServer( port, portPathHandler );
        server.start();
        servers.put( port, server );

        log.info( "server on port {}, statistics: {} ioThreads: {} workerThreads: {} started in {} ms",
            port, statistics,
            server.getWorker().getMXBean().getIoThreadCount(),
            server.getWorker().getMXBean().getMaxWorkerPoolSize(),
            System.currentTimeMillis() - time
        );

        if( statistics ) {
            addStats( server );
        }
    }

    private void addStats( Undertow server ) {
        for( var listenerInfo : server.getListenerInfo() ) {
            var sa = ( InetSocketAddress ) listenerInfo.getAddress();
            var sPort = String.valueOf( sa.getPort() );

            ConnectorStatistics connectorStatistics = listenerInfo.getConnectorStatistics();

            Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "type", "total" ), connectorStatistics, ConnectorStatistics::getRequestCount );
            Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveRequests );
            Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "type", "errors" ), connectorStatistics, ConnectorStatistics::getErrorCount );

            Metrics.gauge( "nio_connections", Tags.of( "port", sPort, "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveConnections );

            Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "name", "worker", "type", "active" ), server, s -> s.getWorker().getMXBean().getWorkerPoolSize() );
            Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "name", "worker", "type", "core" ), server, s -> s.getWorker().getMXBean().getCoreWorkerPoolSize() );
            Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "name", "worker", "type", "max" ), server, s -> s.getWorker().getMXBean().getMaxWorkerPoolSize() );
            Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "name", "worker", "type", "busy" ), server, s -> s.getWorker().getMXBean().getBusyWorkerThreadCount() );
            Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "name", "worker", "type", "queue" ), server, s -> s.getWorker().getMXBean().getWorkerQueueSize() );
        }
    }

    @NotNull
    private Undertow createUndertowServer( int port, PathHandler portPathHandler ) {
        Undertow.Builder builder = Undertow.builder()
            .setSocketOption( Options.REUSE_ADDRESSES, true )
            .setSocketOption( Options.TCP_NODELAY, tcpNodelay )
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

        builder.setServerOption( UndertowOptions.ALWAYS_SET_DATE, alwaysSetDate );
        builder.setServerOption( UndertowOptions.ALWAYS_SET_KEEP_ALIVE, alwaysSetKeepAlive );

        io.undertow.server.HttpHandler handler = portPathHandler;
        if( forceCompressionSupport ) {
            handler = new EncodingHandler( handler, contentEncodingRepository );
            handler = new RequestEncodingHandler( handler )
                .addEncoding( "gzip", GzipStreamSourceConduit.WRAPPER )
                .addEncoding( "deflate", InflatingStreamSourceConduit.WRAPPER );
        }

        if( readTimeout > 0 ) {
            handler = BlockingReadTimeoutHandler.builder()
                    .readTimeout( Duration.ofMillis( readTimeout ) )
                    .nextHandler( handler )
                    .build();
        }
        handler = new BlockingHandler( handler );
        handler = new GracefulShutdownHandler( handler );

        builder.addHttpListener( port, "0.0.0.0", handler );

        return builder.build();
    }

    public void bind( String prefix, HttpHandler handler, boolean compressionSupport ) {
        bind( prefix, handler, compressionSupport, this.defaultPort );
    }

    public void bind( String prefix, HttpHandler handler, boolean compressionSupport, int port ) {
        Preconditions.checkNotNull( prefix );
        Preconditions.checkArgument( !prefix.isEmpty() );

        log.debug( "binding {} on port {} ...", prefix, port );
        io.undertow.server.HttpHandler httpHandler = exchange -> handler.handleRequest( new HttpServerExchange( exchange, requestId.incrementAndGet() ) );

        if( !forceCompressionSupport && compressionSupport ) {
            httpHandler = new EncodingHandler( httpHandler, contentEncodingRepository );
            httpHandler = new RequestEncodingHandler( httpHandler )
                .addEncoding( "gzip", GzipStreamSourceConduit.WRAPPER )
                .addEncoding( "deflate", InflatingStreamSourceConduit.WRAPPER );
        }

        PathHandler assignedHandler = pathHandler.computeIfAbsent( port, p -> new PathHandler() );
        assignedHandler.addPrefixPath( prefix, httpHandler );

        if( servers.containsKey( port ) ) {
            log.info( "server on port: {} already started", port );
            return;
        }
        startNewPort( port, assignedHandler );
    }

    public void bind( String prefix, HttpHandler handler ) {
        bind( prefix, handler, this.defaultPort );
    }

    public void bind( String prefix, HttpHandler handler, int port ) {
        bind( prefix, handler, true, port );
    }

    public void preStop() {
        for( var server : servers.values() ) {
            try {
                server.stop();
            } catch( Exception ex ) {
               log.error( "Cannot stop server", ex );
            }
        }
        pathHandler.clear();
        servers.clear();
    }

    @Override
    public void close() throws IOException {
        preStop();
    }
}
