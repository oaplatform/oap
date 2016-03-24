package oap.http;


import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;

@Slf4j
public class LocalHttpListener extends AbstractHttpListener {

    public LocalHttpListener( final HttpServer httpServer, final int port ) {
        super( httpServer, () -> {
            log.info( "Binding port [{}]...", port );

            final ServerSocket serverSocket = ServerSocketUtils.createLocalSocket(port);

            log.info( "Ready to accept plain connections on [{}]", serverSocket.getLocalSocketAddress() );

            return serverSocket;
        } );
    }
}
