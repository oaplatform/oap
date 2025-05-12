package oap.http.server.nio.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.ServerConnection;
import io.undertow.util.Headers;
import oap.http.server.nio.NioHandlerBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class KeepaliveRequestsHandler implements NioHandlerBuilder, ServerConnection.CloseListener {
    public final int keepaliveRequests;
    final ConcurrentHashMap<Long, AtomicLong> requests = new ConcurrentHashMap<>();

    public KeepaliveRequestsHandler( int keepaliveRequests ) {
        this.keepaliveRequests = keepaliveRequests;
    }

    @Override
    public void closed( ServerConnection connection ) {
        requests.remove( connection.getId() );
    }

    @Override
    public HttpHandler build( HttpHandler next ) {
        return exchange -> {
            ServerConnection connection = exchange.getConnection();
            long id = connection.getId();
            AtomicLong count = requests.computeIfAbsent( id, connectionId -> {
                connection.addCloseListener( KeepaliveRequestsHandler.this );
                return new AtomicLong( 0L );
            } );
            long requests = count.incrementAndGet();

            if( requests >= keepaliveRequests ) {
                exchange.getResponseHeaders().put( Headers.CONNECTION, "close" );
            }
            next.handleRequest( exchange );
        };
    }
}
