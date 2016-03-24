package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

@Slf4j
class AbstractHttpListener implements Runnable, Closeable {

    private final ServerSocket serverSocket;
    private final HttpServer httpServer;

    AbstractHttpListener( final HttpServer httpServer,
                          final Supplier<ServerSocket> socketSupplier ) {
        this.httpServer = httpServer;
        this.serverSocket = socketSupplier.get();
    }

    @Override
    public void run() {
        while( !Thread.interrupted() && !serverSocket.isClosed() ) {
            try {
                final Socket socket = serverSocket.accept();

                httpServer.accept( socket );
            } catch( final IOException e ) {
                log.error( "An error occurred when processing socket connection on port [{}]",
                    serverSocket.getLocalPort() );
                close();
            }
        }
    }

    @Override
    public void close() {
        Closeables.close( serverSocket );

        log.info( "Socket is closed" );
    }
}
