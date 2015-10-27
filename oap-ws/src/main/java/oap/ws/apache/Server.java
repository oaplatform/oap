/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.ws.apache;


import com.google.common.base.Throwables;
import oap.application.Application;
import oap.concurrent.Threads;
import oap.io.Closeables;
import oap.metrics.Metrics;
import oap.metrics.Name;
import oap.ws.RawService;
import oap.ws.Service;
import oap.ws.WsConfig;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnection;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 */
public class Server implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger( Server.class );
    private static final Name CONNECTIONS = Metrics.name( "connections" );
    private final UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
    private final HttpService httpService = new HttpService( HttpProcessorBuilder.create()
        .add( new ResponseDate() )
        .add( new ResponseServer( "OAP Server/1.0" ) )
        .add( new ResponseContent() )
        .add( new ResponseConnControl() )
        .build(),
        DefaultConnectionReuseStrategy.INSTANCE,
        DefaultHttpResponseFactory.INSTANCE,
        mapper );
    private final ConcurrentHashMap<String, HttpConnection> connections = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private int port;
    private final List<WsConfig> wsConfigs;
    private ServerSocket serverSocket;
    private Thread thread;
    private Semaphore semaphore = new Semaphore( 0 );

    public Server( int port, int workers ) {
        this( port, workers, WsConfig.fromClassPath() );
    }

    public Server( int port, int workers, List<WsConfig> wsConfigs ) {
        this.port = port;
        this.wsConfigs = wsConfigs;
        this.executor = Executors.newFixedThreadPool( workers );
        registerHandler( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
    }

    public void bind( String context, Object impl ) {
        String binding = "/" + context + "/*";
        String serviceName = impl.getClass().getSimpleName();
        RawService service = impl instanceof RawService ? (RawService) impl : new Service( impl );
        registerHandler( binding, new ServiceHandler( serviceName, "/" + context, service ) );
        logger.info( serviceName + " bound to " + binding );
    }

    public void unbind( String context ) {
        String binding = "/" + context + "/*";

        this.mapper.unregister( binding );
    }

    public void registerHandler( String pattern, HttpRequestHandler handler ) {
        this.mapper.register( pattern, handler );
    }

    public void start() {
        try {

            logger.info( "binding web services..." );

            for( WsConfig config : wsConfigs )
                for( WsConfig.Service service : config.services )
                    bind( service.context, Application.service( service.service ) );

            logger.info( "starting [localhost:" + port + "]..." );

            serverSocket = new ServerSocket();

            serverSocket.setSoTimeout( 500 );
            serverSocket.setReuseAddress( true );
            serverSocket.setReceiveBufferSize( 1024 * 512 );
            serverSocket.setPerformancePreferences( 2, 1, 0 );
            serverSocket.bind( new InetSocketAddress( port ) );
            logger.info( "ready to rock on " + serverSocket.getLocalSocketAddress() );

            thread = new Thread( this::run );
            thread.start();

            semaphore.acquire();

        } catch( InterruptedException e ) {
            logger.trace( e.getMessage(), e );
        } catch( BindException e ) {
            logger.error( e.getMessage() + " [" + serverSocket.getLocalSocketAddress() + ":" + port + "]", e );
            throw new RuntimeException( e.getMessage(), e );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void stop() {
        Closeables.close( serverSocket );
        Threads.interruptAndJoin( thread );

        serverSocket = null;
        thread = null;

        connections.forEach( ( key, value ) -> Closeables.close( value ) );

        Closeables.close( executor );

        for( WsConfig config : wsConfigs )
            for( WsConfig.Service service : config.services ) unbind( service.context );

        logger.info( "server gone down" );
    }

    private void run() {
        try {
            semaphore.release();

            final ConnectionConfig connectionConfig = ConnectionConfig.custom().setBufferSize( 1024 * 128 ).build();

            final DefaultBHttpServerConnectionFactory connectionFactory =
                new DefaultBHttpServerConnectionFactory( connectionConfig );

            while( !Thread.interrupted() && !serverSocket.isClosed() ) {
                try {
                    DefaultBHttpServerConnection connection =
                        connectionFactory.createConnection( serverSocket.accept() );

                    connections.put( connection.toString(), connection );

                    logger.trace( "connection accepted: " + connection );
                    HttpContext context = HttpCoreContext.create();
                    executor.submit( () -> {
                        Metrics.measureCounterIncrement( CONNECTIONS );
                        String connectionName = connection.toString();
                        try {
                            Thread.currentThread().setName( connection.toString() );
                            logger.trace( "start handling " + connectionName );
                            while( !Thread.interrupted() && connection.isOpen() )
                                httpService.handleRequest( connection, context );
                        } catch( SocketException e ) {
                            if( "Socket closed".equals( e.getMessage() ) )
                                logger.trace( "connection closed: " + connectionName );
                            else logger.error( e.getMessage(), e );
                        } catch( ConnectionClosedException e ) {
                            logger.trace( "connection closed: " + connectionName );
                        } catch( Throwable e ) {
                            logger.error( e.getMessage(), e );
                        } finally {
                            connections.remove( connectionName );
                            Closeables.close( connection );
                            Metrics.measureCounterDecrement( Metrics.name( "connections" ) );
                            logger.trace( "connection closed: " + connectionName );
                        }
                    } );
                } catch( SocketTimeoutException ignored ) {
                } catch( SocketException e ) {
                    if( serverSocket != null && !serverSocket.isClosed() ) logger.warn( e.getMessage(), e );
                } catch( Throwable e ) {
                    logger.warn( e.getMessage(), e );
                } finally {
                    Metrics.reset( CONNECTIONS );
                }
            }
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );

            connections.values().forEach( Closeables::close );
            connections.clear();

            Throwables.propagate( e );
        }
    }

    @Override
    public void close() {
        stop();
    }

}

