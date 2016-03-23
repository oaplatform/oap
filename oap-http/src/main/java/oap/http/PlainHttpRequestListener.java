package oap.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;

public class PlainHttpRequestListener extends AbstractHttpRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger( PlainHttpRequestListener.class );

    public PlainHttpRequestListener( final HttpServer httpServer, final int port ) {
        super( httpServer, () -> {
            LOGGER.info( "Binding to [{}] ...", port );

            final ServerSocket serverSocket = ServerSocketUtils.createPlainSocket( port );

            LOGGER.info( "Ready to accept plain connections on [{}]", serverSocket.getLocalSocketAddress() );

            return serverSocket;

        } );
    }
}