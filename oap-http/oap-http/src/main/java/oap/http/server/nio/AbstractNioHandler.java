package oap.http.server.nio;

import io.undertow.server.HttpHandler;

public abstract class AbstractNioHandler implements io.undertow.server.HttpHandler, NioHandler {
    protected HttpHandler httpHandler;

    public AbstractNioHandler() {
    }

    public AbstractNioHandler( HttpHandler httpHandler ) {
        this.httpHandler = httpHandler;
    }
}
