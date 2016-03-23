package oap.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;

public class SecureHttpRequestListener extends AbstractHttpRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger( SecureHttpRequestListener.class );

    public SecureHttpRequestListener( final HttpServer httpServer, final String keystoreLocation,
                                      final String keystorePassword, final int port ) {
        super( httpServer, () -> {
            LOGGER.info( "Binding to [{}] ...", port );

            final ServerSocket serverSocket = ServerSocketUtils.createSecureSocket( keystoreLocation,
                keystorePassword, port );

            LOGGER.info( "Ready to accept ssl connections on [{}]", serverSocket.getLocalSocketAddress() );


            return serverSocket;
        } );
    }
}
