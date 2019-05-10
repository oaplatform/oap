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


import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.ThreadPoolExecutor;
import oap.concurrent.Threads;
import oap.http.cors.CorsPolicy;
import oap.io.Closeables;
import oap.metrics.Metrics;
import oap.metrics.Metrics2;
import oap.net.Inet;
import org.apache.http.ConnectionClosedException;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.joda.time.Duration;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 * <p>
 * metrics:
 * - http.*
 */
@Slf4j
public class Server implements HttpServer {
    private static final DefaultBHttpServerConnectionFactory connectionFactory = DefaultBHttpServerConnectionFactory.INSTANCE;

    private static final Counter handled = Metrics.counter( "http.handled" );
    private static final Counter keepaliveTimeout = Metrics.counter( "http.keepalive_timeout" );
    private static final Histogram histogramConnections = Metrics2.histogram( "http.connections" );
    private static final Histogram histogramRequestsPerConnection = Metrics2.histogram( "http.connection_requests" );
    private static final Counter histogramRequestsZeroPerConnection = Metrics.counter( "http.connection_requests_zero" );
    private static final Timer timeOfLive = Metrics2.timer( "http.connection_timeoflive" );

    private final ConcurrentHashMap<String, ServerHttpContext> connections = new ConcurrentHashMap<>();
    private final UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
    private final HttpService httpService = new HttpService( HttpProcessorBuilder.create()
//        .add( new ResponseDate() )
        .add( new ResponseServer( "OAP Server/1.0" ) )
        .add( new ResponseContent() )
//        .add( new ResponseConnControl() )
        .build(),
        DefaultConnectionReuseStrategy.INSTANCE,
        DefaultHttpResponseFactory.INSTANCE,
        mapper );
    private final ExecutorService executor;
    private final Thread thread;
    public int keepAliveTimeout = 1000 * 20;

    public Server( int workers, boolean registerStatic ) {
        this.executor = new ThreadPoolExecutor( 0, workers, 10, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "http-%d" ).build() );

        if( registerStatic )
            mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );

        thread = new Thread( this::stats );
        thread.setName( "HTTP-Server-stats" );
        thread.start();
    }

    //TODO Fix resolution of local through headers instead of socket inet address
    private static ServerHttpContext createHttpContext( Socket socket, DefaultBHttpServerConnection connection ) {
        final Protocol protocol;
        if( !Inet.isLocalAddress( socket.getInetAddress() ) ) protocol = Protocol.LOCAL;
        else protocol = socket instanceof SSLSocket ? Protocol.HTTPS : Protocol.HTTP;

        return new ServerHttpContext( HttpCoreContext.create(), protocol, connection );
    }

    private void stats() {
        while( !Thread.interrupted() ) {
            histogramConnections.update( connections.size() );
            Threads.sleepSafely( 400 );
        }
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

            executor.submit( () -> {
                try {
                    handled.inc();

                    var count = 0;

                    log.debug( "connection accepted: {}", connection );

                    var httpContext = createHttpContext( socket, connection );
                    connections.put( connectionId, httpContext );

                    Thread.currentThread().setName( connection.toString() );

                    if( log.isTraceEnabled() )
                        log.trace( "start handling {}", connection );
                    while( !Thread.interrupted() && connection.isOpen() ) {
                        if( count > 100 ) {
                            reportAndGetDuration( httpContext );
                            count = 0;
                        } else {
                            count++;
                        }
                        httpContext.requests++;
                        httpService.handleRequest( connection, httpContext );
                    }
                } catch( SocketTimeoutException e ) {
                    keepaliveTimeout.inc();
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
                    long duration = reportAndGetDuration( info );
                    if( info.requests == 0 )
                        histogramRequestsZeroPerConnection.inc();

                    if( log.isTraceEnabled() )
                        log.trace( "connection: {}, requests: {}, duration: {}",
                            info.connection, info.requests, new Duration( duration * 1000000L ) );
                    try {
                        connection.close();
                    } catch( IOException e ) {
                        log.trace( e.getMessage(), e );
                    }
                }
            } );
        } catch( final IOException e ) {
            log.warn( e.getMessage() );

            connections.values().forEach( Closeables::close );
            connections.clear();

            throw e;
        }
    }

    public long reportAndGetDuration( ServerHttpContext info ) {
        if( info.requests > 0 )
            histogramRequestsPerConnection.update( info.requests );

        var duration = System.nanoTime() - info.start;
        timeOfLive.update( duration, TimeUnit.NANOSECONDS );
        return duration;
    }

    public void stop() {
        connections.forEach( ( key, connection ) -> Closeables.close( connection ) );

        Closeables.close( executor );


        thread.stop();

        log.info( "server gone down" );
    }
}

