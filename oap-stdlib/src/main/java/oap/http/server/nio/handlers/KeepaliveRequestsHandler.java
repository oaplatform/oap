package oap.http.server.nio.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.Headers;
import oap.http.server.nio.AbstractNioHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class KeepaliveRequestsHandler extends AbstractNioHandler implements ServerConnection.CloseListener {
    public final int keepaliveRequests;
    final ConcurrentHashMap<Long, AtomicLong> requests = new ConcurrentHashMap<>();

    public KeepaliveRequestsHandler( int keepaliveRequests ) {
        this.keepaliveRequests = keepaliveRequests;
    }

    public KeepaliveRequestsHandler( HttpHandler httpHandler, int keepaliveRequests ) {
        super( httpHandler );
        this.keepaliveRequests = keepaliveRequests;
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        ServerConnection connection = exchange.getConnection();
        long id = connection.getId();
        AtomicLong count = requests.computeIfAbsent( id, connectionId -> {
            connection.addCloseListener( this );
            return new AtomicLong( 0L );
        } );
        long requests = count.incrementAndGet();

        try {
            if( requests >= keepaliveRequests ) {
                exchange.getResponseHeaders().put( Headers.CONNECTION, "close" );
            }
            httpHandler.handleRequest( exchange );
        } finally {
            if( requests >= keepaliveRequests ) {
                connection.close();
            }
        }
    }

    @Override
    public void closed( ServerConnection connection ) {
        requests.remove( connection.getId() );
    }
}