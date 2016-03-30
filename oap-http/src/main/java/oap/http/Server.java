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
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.metrics.Metrics;
import oap.net.Inet;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.*;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;


/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 */
@Slf4j
public class Server implements HttpServer {

   private static final DefaultBHttpServerConnectionFactory CONNECTION_FACTORY =
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

   private final ThreadPoolExecutor executor;

   public Server( final int workers ) {
      this.executor = new ThreadPoolExecutor( 0, workers, 10, TimeUnit.SECONDS, new SynchronousQueue<>() );

      mapper.register( "/static/*", new ClasspathResourceHandler( "/static", "/WEB-INF" ) );
   }

   @Override
   public void bind( final String context, final Cors cors, final Handler handler,
                     final Protocol protocol ) {
      final String location = "/" + context + "/*";
      mapper.register( location, new BlockingHandlerAdapter( "/" + context, handler, cors, protocol ) );

      log.info( handler + " bound to " + location );
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
            CONNECTION_FACTORY.createConnection( socket );
         final String connectionId = connection.toString();

         executor.submit( () -> {
            try {
               connections.put( connectionId, connection );

               log.trace( "connection accepted: " + connection );

               final HttpContext httpContext = createHttpContext( socket );

               Thread.currentThread().setName( connection.toString() );

               log.trace( "start handling " + connection );
               while( !Thread.interrupted() && connection.isOpen() )
                  httpService.handleRequest( connection, httpContext );
            } catch( SocketException e ) {
               if( "Socket closed".equals( e.getMessage() ) )
                  log.trace( "socket closed: " + connection, e );
               else if( "Connection reset".equals( e.getMessage() ) )
                  log.warn( "Connection reset: " + connection );
               else log.error( e.getMessage(), e );
            } catch( ConnectionClosedException e ) {
               log.trace( "connection closed: " + connection, e );
            } catch( Throwable e ) {
               log.error( e.getMessage(), e );
            } finally {
               connections.remove( connectionId );
               Closeables.close( connection );
               log.trace( "closed: " + connection );
            }
         } );
      } catch( final IOException e ) {
         log.warn( e.getMessage() );

         connections.values().forEach( Closeables::close );
         connections.clear();

         Throwables.propagate( e );
      }
   }

   public void stop() {
      connections.forEach( ( key, connection ) -> Closeables.close( connection ) );

      Closeables.close( executor );

      log.info( "server gone down" );
   }

   private static HttpContext createHttpContext( final Socket socket ) {
      final HttpContext httpContext = HttpCoreContext.create();

      final String protocol;
      if( !Inet.isLocalAddress( socket.getInetAddress() ) ) {
         protocol = Protocol.LOCAL.name();
      } else {
         protocol = SSLSocket.class.isInstance( socket ) ? Protocol.HTTPS.name() : Protocol.HTTP.name();
      }

      httpContext.setAttribute( "protocol", protocol );

      return httpContext;
   }
}

