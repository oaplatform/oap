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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.util.Dates;
import oap.util.Pair;
import org.xnio.Options;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static oap.util.Pair.__;

@Slf4j
public class NioHttpServer implements Closeable, AutoCloseable {
    public int port;
    public SSLConfiguration sslConfiguration = new SSLConfiguration();

    private final HashMap<Integer, Pair<PathHandler, Http.Schema>> pathHandler = new HashMap<>();
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
    public volatile HashMap<Integer, Pair<Undertow, Http.Schema>> servers;
    public boolean forceCompressionSupport = false;
    public boolean alwaysSetDate = true;
    public boolean alwaysSetKeepAlive = true;
    public long readTimeout = Dates.s( 60 );
    private SSLContext sslContext;

    public NioHttpServer() {
        this( -1 );
    }

    public NioHttpServer( int port ) {
        this.port = port;

        if( port > 0 ) {
            pathHandler.put( port, __( new PathHandler(), Http.Schema.HTTP ) );
        }

        contentEncodingRepository = new ContentEncodingRepository();
        contentEncodingRepository.addEncodingHandler( "gzip", new GzipEncodingProvider(), 100 );
        contentEncodingRepository.addEncodingHandler( "deflate", new DeflateEncodingProvider(), 100 );
    }

    public synchronized void start() {
        if( sslConfiguration.port > 0 ) getSSLContext();

        servers = new HashMap<>();
        pathHandler.forEach( ( p, v ) -> {
            startNewPort( p, v._2 );
        } );
    }

    private void startNewPort( int port, Http.Schema schema ) {
        var portPathHandler = pathHandler.get( port );

        Preconditions.checkNotNull( portPathHandler );
        Preconditions.checkArgument( portPathHandler._2 == schema );

        log.info( "bind {}", portPathHandler );

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

        io.undertow.server.HttpHandler handler = portPathHandler._1;
        if( forceCompressionSupport ) {
            handler = new EncodingHandler( handler, contentEncodingRepository );
            handler = new RequestEncodingHandler( handler )
                .addEncoding( "gzip", GzipStreamSourceConduit.WRAPPER )
                .addEncoding( "deflate", InflatingStreamSourceConduit.WRAPPER );
        }

        if( readTimeout > 0 )
            handler = BlockingReadTimeoutHandler.builder().readTimeout( Duration.ofMillis( readTimeout ) ).nextHandler( handler ).build();
        handler = new BlockingHandler( handler );
        handler = new GracefulShutdownHandler( handler );

        if( schema == Http.Schema.HTTP ) {
            builder.addHttpListener( port, "0.0.0.0", handler );
        }
        if( schema == Http.Schema.HTTPS ) {
            builder.addHttpsListener( port, "0.0.0.0", getSSLContext(), handler );
        }

        var server = builder.build();

        servers.put( port, __( server, schema ) );

        server.start();

        log.info( "port {} schema {} statistics {} ioThreads {} workerThreads {}",
            port, schema, statistics,
            server.getWorker().getMXBean().getIoThreadCount(),
            server.getWorker().getMXBean().getMaxWorkerPoolSize()
        );

        if( statistics ) {
            for( var listenerInfo : server.getListenerInfo() ) {
                var sa = ( InetSocketAddress ) listenerInfo.getAddress();
                var sPort = String.valueOf( sa.getPort() );

                ConnectorStatistics connectorStatistics = listenerInfo.getConnectorStatistics();

                Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "schema", schema.name(), "type", "total" ), connectorStatistics, ConnectorStatistics::getRequestCount );
                Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "schema", schema.name(), "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveRequests );
                Metrics.gauge( "nio_requests", Tags.of( "port", sPort, "schema", schema.name(), "type", "errors" ), connectorStatistics, ConnectorStatistics::getErrorCount );

                Metrics.gauge( "nio_connections", Tags.of( "port", sPort, "schema", schema.name(), "type", "active" ), connectorStatistics, ConnectorStatistics::getActiveConnections );

                Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "schema", schema.name(), "name", "worker", "type", "active" ), server, s -> s.getWorker().getMXBean().getWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "schema", schema.name(), "name", "worker", "type", "core" ), server, s -> s.getWorker().getMXBean().getCoreWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "schema", schema.name(), "name", "worker", "type", "max" ), server, s -> s.getWorker().getMXBean().getMaxWorkerPoolSize() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "schema", schema.name(), "name", "worker", "type", "busy" ), server, s -> s.getWorker().getMXBean().getBusyWorkerThreadCount() );
                Metrics.gauge( "nio_pool_size", Tags.of( "port", sPort, "schema", schema.name(), "name", "worker", "type", "queue" ), server, s -> s.getWorker().getMXBean().getWorkerQueueSize() );

            }
        }
    }

    private synchronized SSLContext getSSLContext() {
        if( sslContext != null ) return sslContext;

        try( InputStream inputStream = Files.newInputStream( sslConfiguration.jks ) ) {
            KeyStore ks = KeyStore.getInstance( "JKS" );
            KeyManagerFactory kmf = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
            sslContext = SSLContext.getInstance( "TLS" );

            ks.load( inputStream, sslConfiguration.password.toCharArray() );
            kmf.init( ks, sslConfiguration.password.toCharArray() );

            sslContext.init( kmf.getKeyManagers(), null, null );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        } catch( UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException |
                 KeyManagementException e ) {
            throw new RuntimeException( e );
        }

        return sslContext;
    }

    public void bind( String prefix, HttpHandler handler, boolean compressionSupport ) {
        Preconditions.checkArgument( port > 0 || sslConfiguration.port > 0 );

        if( this.port > 0 ) {
            bind( prefix, handler, compressionSupport, this.port, Http.Schema.HTTP );
        }
        if( this.sslConfiguration.port > 0 ) {
            bind( prefix, handler, compressionSupport, this.sslConfiguration.port, Http.Schema.HTTPS );
        }
    }

    public synchronized void bind( String prefix, HttpHandler handler, boolean compressionSupport, int port, Http.Schema schema ) {
        log.debug( "bind {}", prefix );

        Preconditions.checkNotNull( prefix );
        Preconditions.checkArgument( !prefix.isEmpty() );
        Preconditions.checkArgument( schema != Http.Schema.HTTPS || sslConfiguration.port > 0 );
        Preconditions.checkArgument( schema != Http.Schema.HTTPS || sslConfiguration.jks != null );
        Preconditions.checkArgument( schema != Http.Schema.HTTPS || sslConfiguration.password != null );

        io.undertow.server.HttpHandler httpHandler = exchange -> handler.handleRequest( new HttpServerExchange( exchange, requestId.incrementAndGet() ) );

        if( !forceCompressionSupport && compressionSupport ) {
            httpHandler = new EncodingHandler( httpHandler, contentEncodingRepository );
            httpHandler = new RequestEncodingHandler( httpHandler )
                .addEncoding( "gzip", GzipStreamSourceConduit.WRAPPER )
                .addEncoding( "deflate", InflatingStreamSourceConduit.WRAPPER );
        }

        pathHandler.computeIfAbsent( port, hp -> __( new PathHandler(), schema ) )._1.addPrefixPath( prefix, httpHandler );

        if( servers != null && !servers.containsKey( port ) ) {
            startNewPort( port, schema );
        }
    }

    public void bind( String prefix, HttpHandler handler, Http.Schema schema ) {
        bind( prefix, handler, schema == Http.Schema.HTTP ? port : this.sslConfiguration.port, schema );
    }

    public void bind( String prefix, HttpHandler handler, int port, Http.Schema schema ) {
        bind( prefix, handler, true, port, schema );
    }

    public synchronized void preStop() {
        if( servers != null ) {
            for( var server : servers.values() ) {
                try {
                    server._1.stop();
                } catch( Exception ex ) {
                    log.error( "Cannot stop server", ex );
                }
            }
            pathHandler.clear();
            servers = null;
        }
    }

    @Override
    public void close() throws IOException {
        preStop();
    }

    @ToString( exclude = "password" )
    public static class SSLConfiguration {
        public int port = -1;

        public Path jks;
        public String password;

        public SSLConfiguration() {
        }

    }
}
