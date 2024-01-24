package oap.http.server.nio;

import io.undertow.server.HttpHandler;

public interface NioHandlerBuilder extends NioHandler {
    HttpHandler build( HttpHandler next );
}
