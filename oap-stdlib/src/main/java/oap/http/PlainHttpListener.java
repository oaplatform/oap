package oap.http;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@Slf4j
public class PlainHttpListener extends AbstractHttpListener {

    private final int port;

    public PlainHttpListener( HttpServer server, int port ) {
        super( server );
        this.port = port;
        log.info( "Plain HTTP listener configured to bind on {}", port );
    }

    @Override
    protected ServerSocket createSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.setSoTimeout( timeout );
            serverSocket.bind( new InetSocketAddress( port ) );
            log.info( "Successfully bound plain http listener to {}", port );
            return serverSocket;
        } catch( BindException e ) {
            log.error( "Cannot bind to port [{}]", port );
            throw new UncheckedIOException( e );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
