package oap.http;

import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.nio.file.Path;

@Slf4j
public class SecureHttpListener extends AbstractHttpListener {

    public SecureHttpListener( final HttpServer httpServer, final Path keystoreLocation,
                               final String keystorePassword, final int port ) {
        super( httpServer, () -> {
            log.info( "Binding to [{}] ...", port );

            final ServerSocket serverSocket = ServerSocketUtils.createSecureSocket( keystoreLocation,
                keystorePassword, port );

            log.info( "Ready to accept ssl connections on [{}]", serverSocket.getLocalSocketAddress() );


            return serverSocket;
        } );
    }
}
