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


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.ThreadPoolExecutor;
import oap.http.cors.CorsPolicy;
import oap.io.Closeables;
import oap.metrics.Metrics;
import oap.net.Inet;
import oap.util.Throwables;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static oap.io.Sockets.connectionReset;
import static oap.io.Sockets.socketClosed;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 */
@Slf4j
public class Server implements HttpServer {

   private static final DefaultBHttpServerConnectionFactory connectionFactory =
      DefaultBHttpServerConnectionFactory.INSTANCE;

   private final ConcurrentHashMap<String, HttpConnection> connections = new ConcurrentHashMap<>();
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

   private final ExecutorService executor;

   public Server( final int workers ) {
      this.executor = new ThreadPoolExecutor( 0, workers, 10, TimeUnit.SECONDS, new SynchronousQueue<>(),
         new ThreadFactoryBuilder().setNameFormat( "http-%d" ).build() );

      mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
   }

   //TODO Fix resolution of local through headers instead of socket inet address
   private static HttpContext createHttpContext( final Socket socket ) {
      final HttpContext httpContext = HttpCoreContext.create();

      final Protocol protocol;
      if( !Inet.isLocalAddress( socket.getInetAddress() ) ) protocol = Protocol.LOCAL;
      else protocol = SSLSocket.class.isInstance( socket ) ? Protocol.HTTPS : Protocol.HTTP;

      httpContext.setAttribute( "protocol", protocol );

      return httpContext;
   }

   @Override
   public void bind( final String context, final CorsPolicy corsPolicy, final Handler handler,
                     final Protocol protocol ) {
      final String location = "/" + context + "/*";
      mapper.register( location, new BlockingHandlerAdapter( "/" + context, handler, corsPolicy, protocol ) );

      log.debug( handler + " bound to " + location );
   }

   @Override
   public void unbind( final String context ) {
      mapper.unregister( "/" + context + "/*" );
   }

   public void start() {
      Metrics.measureGauge( "connections", connections::size );
   }

   public void accepted( final Socket socket ) {
      try {
         final DefaultBHttpServerConnection connection =
            connectionFactory.createConnection( socket );
         final String connectionId = connection.toString();

         executor.submit( () -> {
            try {
               connections.put( connectionId, connection );

               log.debug( "connection accepted: {}", connection );

               final HttpContext httpContext = createHttpContext( socket );

               Thread.currentThread().setName( connection.toString() );

               log.debug( "start handling {}", connection );
               while( !Thread.interrupted() && connection.isOpen() )
                  httpService.handleRequest( connection, httpContext );
            } catch( SocketException e ) {
               if( socketClosed( e ) )
                  log.debug( "Socket closed: {}", connection );
               else if( connectionReset( e ) )
                  log.warn( "Connection reset: {}", connection );
               else log.error( e.getMessage(), e );
            } catch( ConnectionClosedException e ) {
               log.debug( "connection closed: {}", connection );
            } catch( Throwable e ) {
               log.error( e.getMessage(), e );
            } finally {
               connections.remove( connectionId );
               Closeables.close( connection );
            }
         } );
      } catch( final IOException e ) {
         log.warn( e.getMessage() );

         connections.values().forEach( Closeables::close );
         connections.clear();

         throw Throwables.propagate( e );
      }
   }

   public void stop() {
      connections.forEach( ( key, connection ) -> Closeables.close( connection ) );

      Closeables.close( executor );

      log.info( "server gone down" );
   }
}

