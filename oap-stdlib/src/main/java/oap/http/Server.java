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
package oap.http;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.ThreadPoolExecutor;
import oap.http.cors.CorsPolicy;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 * <p>
 * metrics:
 * - http.*
 */
@Slf4j
public class Server implements HttpServer, Closeable {
    private static final DefaultBHttpServerConnectionFactory connectionFactory = DefaultBHttpServerConnectionFactory.INSTANCE;

    private static final Counter requestsCounter = Metrics.counter( "oap_http", "type", "requests" );
    private static final Counter handledCounter = Metrics.counter( "oap_http", "type", "handled" );
    private static final Counter rejectedCounter = Metrics.counter( "oap_http", "type", "rejected" );
    private static final Counter keepaliveTimeoutCounter = Metrics.counter( "oap_http", "type", "keepalive_timeout" );

    private final ConcurrentHashMap<String, ServerHttpContext> connections = new ConcurrentHashMap<>();
    private final UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
    private final int workers;
    private final int queueSize;
    private final boolean registerStatic;
    private final AtomicInteger activeCount = new AtomicInteger();
    public int keepAliveTimeout = 1000 * 20;
    public String originalServer = "OAP Server/1.0";
    public boolean responseDate = true;
    public RejectedInfo rejected = new RejectedInfo( HTTP_UNAVAILABLE, "temporary overload" );
    private HttpService httpService;
    private ExecutorService executor;
    private ExecutorService rejectedExecutor;
    private BlockingQueue<Runnable> workQueue;
    private Gauge workQueueMetric;

    public Server( int workers, int queueSize, boolean registerStatic ) {
        this.workers = workers;
        this.queueSize = queueSize;
        this.registerStatic = registerStatic;

        Metrics.gauge( "oap_http_connections", connections, ConcurrentHashMap::size );
        Metrics.gauge( "oap_http_processes", activeCount, AtomicInteger::get );
    }

    //TODO Fix resolution of local through headers instead of socket inet address
    private static ServerHttpContext createHttpContext( HttpServer httpServer, Socket socket, DefaultBHttpServerConnection connection ) {
        final Protocol protocol;
        if( !Inet.isLocalAddress( socket.getInetAddress() ) ) protocol = Protocol.LOCAL;
        else protocol = socket instanceof SSLSocket ? Protocol.HTTPS : Protocol.HTTP;

        return new ServerHttpContext( httpServer, HttpCoreContext.create(), protocol, connection );
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
            new ThreadFactoryBuilder().setNameFormat( "http-%d" ).build() );

        this.rejectedExecutor = new ThreadPoolExecutor( 0, workers,
            10, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "http-rejected-%d" ).build() );

        if( registerStatic )
            mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
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
        socket.setSoTimeout( keepAliveTimeout );
        try {
            var connection = connectionFactory.createConnection( socket );
            var connectionId = connection.toString();

            try {
                executor.submit( () -> {
                    try {
                        handledCounter.increment();

                        log.debug( "connection accepted: {}", connection );

                        var httpContext = createHttpContext( this, socket, connection );
                        connections.put( connectionId, httpContext );

                        Thread.currentThread().setName( connection.toString() );

                        if( log.isTraceEnabled() )
                            log.trace( "start handling {}", connection );
                        while( !Thread.interrupted() && connection.isOpen() ) {
                            requestsCounter.increment();
                            activeCount.incrementAndGet();
                            try {
                                httpService.handleRequest( connection, httpContext );
                            } finally {
                                activeCount.decrementAndGet();
                            }
                        }
                    } catch( SocketTimeoutException e ) {
                        keepaliveTimeoutCounter.increment();
                        if( log.isTraceEnabled() )

                            log.trace( "{}: timeout", connection );
                    } catch( SocketException | SSLException e ) {
                        log.debug( "{}: {}", connection, e.getMessage() );
                    } catch( ConnectionClosedException e ) {
                        log.debug( "connection closed: {}", connection );
                    } catch( Throwable e ) {
                        log.error( e.getMessage(), e );
                    } finally {
                        var info = connections.remove( connectionId );

                        if( log.isTraceEnabled() )
                            log.trace( "connection: {}, requests: {}, duration: {}",
                                info.connection, ( long ) requestsCounter.count(), Dates.durationToString( ( long ) ( ( System.nanoTime() - info.start ) / 1E6 ) ) );
                        try {
                            connection.close();
                        } catch( IOException e ) {
                            log.trace( e.getMessage(), e );
                        }
                    }
                } );
            } catch( RejectedExecutionException e ) {
                rejectedCounter.increment();
                log.warn( e.getMessage() );

                try {
                    rejectedExecutor.execute( () -> {
                        try {
                            var response = new BasicHttpResponse( HttpVersion.HTTP_1_1, rejected.code, rejected.reason );
                            response.setHeader( "Connection", "close" );
                            connection.sendResponseHeader( response );
                        } catch( Exception ignored ) {
                        } finally {
                            Closeables.close( connection );
                        }
                    } );
                } catch( Exception ignored ) {
                    connection.close();
                }
            }
        } catch( final IOException e ) {
            log.warn( e.getMessage() );

            connections.values().forEach( Closeables::close );
            connections.clear();

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

    @ToString
    public static class RejectedInfo {
        public final int code;
        public final String reason;

        @JsonCreator
        public RejectedInfo( int code, String reason ) {
            this.code = code;
            this.reason = reason;
        }

        public RejectedInfo( int code ) {
            this( code, null );
        }
    }
}

