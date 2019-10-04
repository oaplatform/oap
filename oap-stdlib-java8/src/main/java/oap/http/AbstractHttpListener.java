package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedRunnable;
import oap.concurrent.Threads;
import oap.io.Closeables;

import java.io.Closeable;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static oap.concurrent.Once.executeOnce;
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
            try {
                while( !Thread.interrupted() && ( serverSocket = createSocket() ) == null ) {
                    executeOnce( () -> log.warn( "Server socket cannot be opened; waiting for it ..." ) );
                    Threads.sleepSafely( sleep );
                }
                log.debug( "ready to rock [{}]", serverSocket );
            } finally {
                this.notifyReady();
            }

            while( !Thread.interrupted() && !serverSocket.isClosed() )
                try {
                    server.accepted( serverSocket.accept() );
                } catch( SocketTimeoutException ignore ) {
                } catch( SocketException e ) {
                    if( socketClosed( e ) ) log.debug( e.getMessage() );
                    else log.error( e.getMessage(), e );
                } catch( Exception e ) {
                    log.error( e.getMessage(), e );
                }
        } catch( RuntimeException t ) {
            log.error( t.getMessage(), t );
            throw t;
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        Closeables.close( serverSocket );
    }
}
