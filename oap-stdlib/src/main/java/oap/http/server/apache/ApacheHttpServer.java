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
package oap.http.server.apache;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.ThreadPoolExecutor;
import oap.http.ClasspathResourceHandler;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;
import oap.http.cors.GenericCorsPolicy;
import oap.http.server.Handler;
import oap.http.server.HttpServer;
import oap.http.server.health.HealthHttpHandler;
import oap.io.Closeables;
import oap.net.Inet;
import oap.util.Dates;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;


@Slf4j
public class ApacheHttpServer implements HttpServer, Closeable {
    private static final BasicHttpResponse TEMPORARY_OVERLOAD;

    private static final DefaultBHttpServerConnectionFactory connectionFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
    private static final Counter requestsCounter = Metrics.counter( "oap_http", "type", "requests" );
    private static final Counter handledCounter = Metrics.counter( "oap_http", "type", "handled" );
    private static final Counter rejectedCounter = Metrics.counter( "oap_http", "type", "rejected" );
    private static final Counter connectionClosedCounter = Metrics.counter( "oap_http", "type", "conn_closed" );
    private static final Counter socketOrSslErrorCounter = Metrics.counter( "oap_http", "type", "socket_or_ssl_error" );
    private static final Counter keepaliveTimeoutCounter = Metrics.counter( "oap_http", "type", "keepalive_timeout" );

    static {
        TEMPORARY_OVERLOAD = new BasicHttpResponse( HttpVersion.HTTP_1_1, HTTP_UNAVAILABLE, "temporary overload" );
        TEMPORARY_OVERLOAD.setHeader( "Connection", "close" );

    }

    private final ConcurrentHashMap<String, ServerHttpContext> connections = new ConcurrentHashMap<>();
    private final UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
    private final int workers;
    private final int queueSize;
    private final boolean registerStatic;
    private final AtomicInteger activeCount = new AtomicInteger();
    public long keepAliveTimeout = Dates.s( 20 );
    public String originalServer = "OAP Server/1.0";
    public boolean responseDate = true;
    protected HealthHttpHandler healthHttpHandler;
    private HttpService httpService;
    private ExecutorService executor;
    private ExecutorService rejectedExecutor;
    private BlockingQueue<Runnable> workQueue;
    private Gauge workQueueMetric;

    public ApacheHttpServer( int workers, int queueSize, boolean registerStatic ) {
        this.workers = workers;
        this.queueSize = queueSize;
        this.registerStatic = registerStatic;

        Metrics.gauge( "oap_http_connections", connections, ConcurrentHashMap::size );
        Metrics.gauge( "oap_http_processes", activeCount, AtomicInteger::get );
    }

    //TODO Fix resolution of local through headers instead of socket inet address
    private static ServerHttpContext createHttpContext( HttpServer httpServer, Socket socket, DefaultBHttpServerConnection connection, long timeNs ) {
        final Protocol protocol;
        if( !Inet.isLocalAddress( socket.getInetAddress() ) ) protocol = Protocol.LOCAL;
        else protocol = socket instanceof SSLSocket ? Protocol.HTTPS : Protocol.HTTP;

        return new ServerHttpContext( httpServer, HttpCoreContext.create(), protocol, connection, timeNs );
    }

    public void start() {
        log.info( "workers = {}, queue size = {}", workers, queueSize );

        var httpProcessorBuilder = HttpProcessorBuilder.create();
        if( originalServer != null )
            httpProcessorBuilder = httpProcessorBuilder.add( new ResponseServer( originalServer ) );
        if( responseDate )
            httpProcessorBuilder = httpProcessorBuilder.add( new ResponseDate() );

        httpService = new HttpService( httpProcessorBuilder
            .add( new ResponseContent() )
            .add( new ResponseConnControl() )
            .build(),
            DefaultConnectionReuseStrategy.INSTANCE,
            DefaultHttpResponseFactory.INSTANCE,
            mapper );

        workQueue = queueSize == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>( queueSize );

        if( queueSize > 0 )
            workQueueMetric = Gauge.builder( "oap_http_queue", workQueue, BlockingQueue::size ).register( Metrics.globalRegistry );

        this.executor = new ThreadPoolExecutor( 0, workers, 10, TimeUnit.SECONDS,
            workQueue,
            new ThreadFactoryBuilder().setNameFormat( "http-%d" ).build(),
            new ThreadPoolExecutor.AbortPolicy() );

        this.rejectedExecutor = new ThreadPoolExecutor( 0, workers,
            10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "http-rejected-%d" ).build() );

        if( registerStatic )
            mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
        if( healthHttpHandler != null )
            mapper.register( "/health", new BlockingHandlerAdapter( "/", healthHttpHandler, GenericCorsPolicy.DEFAULT, Protocol.HTTP ) );
    }

    @Override
    public void bind( String context, CorsPolicy corsPolicy, Handler handler, Protocol protocol ) {
        var location1 = "/" + context + "/*";
        var location2 = "/" + context;
        mapper.register( location1, new BlockingHandlerAdapter( "/" + context, handler, corsPolicy, protocol ) );
        mapper.register( location2, new BlockingHandlerAdapter( "/" + context, handler, corsPolicy, protocol ) );

        log.info( "{} bound to [{}, {}]", handler, location1, location2 );
    }

    @Override
    public void unbind( final String context ) {
        mapper.unregister( "/" + context + "/*" );
    }

    @SneakyThrows
    public void accepted( Socket socket ) {
        var timeNs = System.nanoTime();

        if( keepAliveTimeout > 0 ) {
            socket.setSoTimeout( ( int ) keepAliveTimeout );
        }
        try {
            var connection = connectionFactory.createConnection( socket );
            var connectionId = connection.toString();

            try {
                executor.submit( () -> {
                    boolean firstRequest = true;
                    try {
                        handledCounter.increment();

                        log.debug( "connection accepted: {}", connection );

                        var httpContext = createHttpContext( this, socket, connection, timeNs );
                        connections.put( connectionId, httpContext );

                        Thread.currentThread().setName( connection.toString() );

                        log.trace( "start handling {}", connection );
                        while( !Thread.interrupted() && connection.isOpen() ) {
                            requestsCounter.increment();
                            activeCount.incrementAndGet();
                            try {
                                ServerHttpContext httpContextWithStartTime;
                                if( firstRequest ) {
                                    httpContextWithStartTime = httpContext;
                                    firstRequest = false;
                                } else httpContextWithStartTime = httpContext.withCurrentTimeNs();
                                httpService.handleRequest( connection, httpContextWithStartTime );
                            } finally {
                                activeCount.decrementAndGet();
                            }
                        }
                    } catch( SocketTimeoutException e ) {
                        Closeables.close( socket );
                        keepaliveTimeoutCounter.increment();
                        log.trace( "{}: timeout", connection );
                    } catch( IndexOutOfBoundsException e ) {
                        socketOrSslErrorCounter.increment();
                        log.debug( e.getMessage(), e );
                    } catch( SocketException | SSLException e ) {
                        socketOrSslErrorCounter.increment();
                        log.debug( "{}: {}", connection, e.getMessage() );
                    } catch( ConnectionClosedException e ) {
                        connectionClosedCounter.increment();
                        log.debug( "connection closed: {}", connection );
                    } catch( Throwable e ) {
                        log.error( e.getMessage(), e );
                    } finally {
                        var info = connections.remove( connectionId );

                        log.trace( "closing connection: {}, requests: {}, duration: {}",
                            info.connection, ( long ) requestsCounter.count(), Dates.durationToString( ( System.nanoTime() - timeNs ) / 1_000_000 ) );
                        try {
                            connection.close();
                        } catch( IOException e ) {
                            log.trace( e.getMessage(), e );
                        }
                    }
                } );
            } catch( oap.concurrent.ThreadPoolExecutor.RejectedExecutionException e ) {
                rejectedCounter.increment();
                log.warn( e.getMessage() );

                try {
                    rejectedExecutor.execute( () -> {
                        try {
                            connection.sendResponseHeader( TEMPORARY_OVERLOAD );
                        } catch( Exception t ) {
                            log.trace( t.getMessage(), t );
                        } finally {
                            Closeables.close( connection );
                        }
                    } );
                } catch( Exception t ) {
                    log.trace( t.getMessage(), t );
                    connection.close();
                }
            }
        } catch( final IOException e ) {
            log.warn( e.getMessage() );
            throw e;
        }
    }

    @Override
    public int getQueueSize() {
        return workQueue.size();
    }

    @Override
    public int getActiveCount() {
        return activeCount.get();
    }

    public synchronized void preStop() {
        connections.forEach( ( key, connection ) -> Closeables.close( connection ) );

        Closeables.close( executor );
        Closeables.close( rejectedExecutor );

        log.info( "server gone down" );

        if( workQueueMetric != null ) {
            Metrics.globalRegistry.remove( workQueueMetric );
            workQueueMetric = null;
        }
    }

    public void stop() {
        preStop();
    }

    @Override
    public void close() {
        preStop();
    }
}
