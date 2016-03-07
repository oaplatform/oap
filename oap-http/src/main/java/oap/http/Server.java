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


import com.google.common.base.Throwables;
import oap.concurrent.Threads;
import oap.io.Closeables;
import oap.metrics.Metrics;
import oap.concurrent.ThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.concurrent.*;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 */
public class Server implements HttpServer {

    private final static Logger logger = LoggerFactory.getLogger( Server.class );
    private static final String METRICS_CONNECTIONS = "connections";
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
    private ThreadPoolExecutor executor;
    private int port;
    private ServerSocket serverSocket;
    private Thread thread;
    private Semaphore semaphore = new Semaphore( 0 );

    public Server( int port, int workers ) {
        this.port = port;
        final BlockingQueue<Runnable> queue = new SynchronousQueue<>();

        this.executor = new ThreadPoolExecutor( 0, workers, 10, TimeUnit.SECONDS, queue );
        this.mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
    }

    @Override
    public void bind( String context, Cors cors, Handler handler, boolean local ) {
        String location = "/" + context + "/*";
        this.mapper.register( location, new BlockingHandlerAdapter( "/" + context, handler, cors, local ) );
        logger.info( handler + " bound to " + location );

    }

    @Override
    public void unbind( String context ) {
        String location = "/" + context + "/*";
        this.mapper.unregister( location );
    }

    public void start() {
        try {

            logger.info( "binding to " + port + "..." );

            Metrics.measureGauge( METRICS_CONNECTIONS, connections::size );
            serverSocket = new ServerSocket();

            serverSocket.setReuseAddress( true );
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

        connections.forEach( ( key, connection ) -> Closeables.close( connection ) );

        Closeables.close( executor );

        logger.info( "server gone down" );
    }

    private void run() {
        try {
            semaphore.release();

            final DefaultBHttpServerConnectionFactory connectionFactory =
                new DefaultBHttpServerConnectionFactory();

            while( !Thread.interrupted() && !serverSocket.isClosed() ) {
                try {
                    final Socket accept = serverSocket.accept();
                    DefaultBHttpServerConnection connection =
                        connectionFactory.createConnection( accept );

                    final String connectionName = connection.toString();

                    try {
                        executor.submit( () -> {
                            try {
                                connections.put( connectionName, connection );

                                logger.trace( "connection accepted: " + connection );
                                HttpContext context = HttpCoreContext.create();

                                Thread.currentThread().setName( connection.toString() );
                                logger.trace( "start handling " + connectionName );
                                while( !Thread.interrupted() && connection.isOpen() )
                                    httpService.handleRequest( connection, context );
                            } catch( SocketException e ) {
                                if( "Socket closed".equals( e.getMessage() ) )
                                    logger.trace( "se:connection closed: " + connectionName, e );
                                else if( "Connection reset".equals( e.getMessage() ) )
                                    logger.warn( "Connection reset: " + connectionName );
                                else logger.error( e.getMessage(), e );
                            } catch( ConnectionClosedException e ) {
                                logger.trace( "cce:connection closed: " + connectionName, e );
                            } catch( Throwable e ) {
                                logger.error( e.getMessage(), e );
                            } finally {
                                connections.remove( connectionName );
                                Closeables.close( connection );
                                logger.trace( "f:connection closed: " + connectionName );
                            }
                        } );
                    } catch( IllegalStateException e ) {
                        logger.warn( e.getMessage() );
                        IOUtils.closeQuietly( connection );
                    }
                } catch( SocketTimeoutException ignored ) {
                } catch( SocketException e ) {
                    if( serverSocket != null && !serverSocket.isClosed() ) logger.warn( e.getMessage() );
                } catch( Throwable e ) {
                    logger.warn( e.getMessage(), e );
                }
            }
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );

            connections.values().forEach( Closeables::close );
            connections.clear();

            Throwables.propagate( e );
        }
    }

}

