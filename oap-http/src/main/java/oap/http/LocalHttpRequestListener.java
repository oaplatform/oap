package oap.http;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;

public class LocalHttpRequestListener extends AbstractHttpRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger( LocalHttpRequestListener.class );

    public LocalHttpRequestListener( final HttpServer httpServer ) {
        super( httpServer, () -> {
            LOGGER.info( "Binding to localhost port 9090" );

            final ServerSocket serverSocket = ServerSocketUtils.createLocalSocket();

            LOGGER.info( "Ready to accept plain connections on [{}]", serverSocket.getLocalSocketAddress() );

            return serverSocket;
        } );
    }
}
