package oap.http;

import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;

@Slf4j
public class PlainHttpListener extends AbstractHttpListener {

    public PlainHttpListener( final HttpServer httpServer, final int port ) {
        super( httpServer, () -> {
            log.info( "Binding to [{}] ...", port );

            final ServerSocket serverSocket = ServerSocketUtils.createPlainSocket( port );

            log.info( "Ready to accept plain connections on [{}]", serverSocket.getLocalSocketAddress() );

            return serverSocket;

        } );
    }
}