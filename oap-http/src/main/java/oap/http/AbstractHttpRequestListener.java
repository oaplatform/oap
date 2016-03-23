package oap.http;

import oap.concurrent.SynchronizedThread;
import oap.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

class AbstractHttpRequestListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractHttpRequestListener.class );

    private final SynchronizedThread synchronizedThread = new SynchronizedThread( this );

    private final ServerSocket serverSocket;
    private final HttpServer httpServer;

    AbstractHttpRequestListener( final HttpServer httpServer,
                                 final Supplier<ServerSocket> socketSupplier ) {
        this.httpServer = httpServer;
        this.serverSocket = socketSupplier.get();
    }

    @Override
    public void run() {
        try {
            while( synchronizedThread.isRunning() && !serverSocket.isClosed() ) {
                try {
                    final Socket socket = serverSocket.accept();

                    httpServer.accept( socket );
                } catch( final IOException e ) {
                    LOGGER.error( "An error occurred when processing socket connection on port [{}]",
                        serverSocket.getLocalPort() );
                }
            }
        } finally {
            Closeables.close( serverSocket );
        }
    }

    public void start() {
        if( serverSocket != null ) {
            synchronizedThread.start();
        }
    }

    public void stop() {
        Closeables.close( serverSocket );
        synchronizedThread.stop();

        LOGGER.info( "Socket is closed" );
    }
}
