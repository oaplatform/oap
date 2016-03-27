package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

@Slf4j
public abstract class AbstractHttpListener implements Runnable, Closeable {

   private ServerSocket serverSocket;
   private final HttpServer server;
   protected int timeout = 1000;

   AbstractHttpListener( HttpServer server ) {
      this.server = server;
   }

   protected abstract ServerSocket createSocket();

   @Override
   public void run() {
      serverSocket = createSocket();
      log.debug( "ready to rock [{}]", serverSocket );

      while( !Thread.interrupted() && !serverSocket.isClosed() ) try {
         server.accepted( serverSocket.accept() );
      } catch( final SocketTimeoutException ignore ) {
      } catch( final IOException e ) {
         log.error( e.getMessage(), e );
      }
   }

   @Override
   public void close() {
      Closeables.close( serverSocket );
   }
}
