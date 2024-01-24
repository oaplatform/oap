package oap.http.server.nio.handlers;

import io.undertow.server.HttpHandler;
import oap.http.server.nio.NioHandlerBuilder;
import oap.util.Dates;

import java.time.Duration;

public class BlockingReadTimeoutHandler implements NioHandlerBuilder {
    public long readTimeout = Dates.s( 60 );

    @Override
    public HttpHandler build( HttpHandler next ) {
        return io.undertow.server.handlers.BlockingReadTimeoutHandler.builder()
            .readTimeout( Duration.ofMillis( readTimeout ) )
            .nextHandler( next )
            .build();
    }
}
