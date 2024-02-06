package oap.http.server.nio;

import org.xnio.XnioWorker;

public interface XnioHttpHandler extends HttpHandler {
    default void init( XnioWorker xnioWorker ) {}
}
