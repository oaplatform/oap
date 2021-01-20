package oap.http;

import oap.http.server.HttpServer;

@Deprecated
public class PlainHttpListener extends oap.http.server.apache.PlainHttpListener {

    public PlainHttpListener( HttpServer server, int port ) {
        super( server, port );
    }

}
