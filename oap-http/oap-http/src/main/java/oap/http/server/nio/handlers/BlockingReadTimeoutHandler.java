package oap.http.server.nio.handlers;

import io.undertow.server.HttpHandler;
import oap.http.server.nio.NioHandlerBuilder;

import java.time.Duration;

public class BlockingReadTimeoutHandler implements NioHandlerBuilder {
    public final long readTimeout;

    public BlockingReadTimeoutHandler( long readTimeout ) {
        this.readTimeout = readTimeout;
    }

    @Override
    public HttpHandler build( HttpHandler next ) {
        return io.undertow.server.handlers.BlockingReadTimeoutHandler.builder()
            .readTimeout( Duration.ofMillis( readTimeout ) )
            .nextHandler( next )
            .build();
    }
}
