package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedRunnable;
import oap.io.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static oap.concurrent.Once.once;
import static oap.io.Sockets.socketClosed;

@Slf4j
public abstract class AbstractHttpListener extends SynchronizedRunnable implements Closeable {
    private final HttpServer server;
    protected int timeout = 1000;
    protected long sleep = 60000;
    private ServerSocket serverSocket;

    AbstractHttpListener( HttpServer server ) {
        this.server = server;
    }

    protected abstract ServerSocket createSocket();

    @Override
    public void run() {
        try {
            while( !Thread.interrupted() && ( serverSocket = createSocket() ) == null ) {
                once( () -> log.warn( "Server socket cannot be opened; waiting for it until " + sleep + "ms..." ) );
                Thread.sleep( sleep );
            }
            log.debug( "ready to rock [{}]", serverSocket );

            this.notifyReady();

            while( !Thread.interrupted() && !serverSocket.isClosed() )
                try {
                    server.accepted( serverSocket.accept() );
                } catch( SocketTimeoutException ignore ) {
                } catch( SocketException e ) {
                    if( socketClosed( e ) ) log.debug( e.getMessage() );
                    else log.error( e.getMessage(), e );
                } catch( IOException e ) {
                    log.error( e.getMessage(), e );
                }
        } catch( RuntimeException t ) {
            log.error( t.getMessage(), t );
            throw t;
        } catch( InterruptedException e ) {
            log.warn( "Sleep until next socket creation was interrupted", e );
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        Closeables.close( serverSocket );
    }
}
