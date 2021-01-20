package oap.http;


import oap.http.server.HttpServer;

@Deprecated
public class LocalHttpListener extends oap.http.server.apache.LocalHttpListener {

    public LocalHttpListener( HttpServer server, int port ) {
        super( server, port );
    }

}
