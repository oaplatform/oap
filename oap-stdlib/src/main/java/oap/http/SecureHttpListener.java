package oap.http;

import oap.http.server.HttpServer;
import oap.http.server.apache.SslHttpListener;

import java.nio.file.Path;

@Deprecated
public class SecureHttpListener extends SslHttpListener {

    public SecureHttpListener( HttpServer server, int port, boolean privateNetwork ) {
        this( server, null, null, port, privateNetwork );
    }

    public SecureHttpListener( HttpServer server, Path keystoreLocation, String keystorePassword, int port, boolean privateNetwork ) {
        super( server, keystoreLocation, keystorePassword, port );
    }

}
