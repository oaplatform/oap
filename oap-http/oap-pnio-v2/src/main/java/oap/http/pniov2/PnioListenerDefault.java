package oap.http.pniov2;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerDefault<WorkflowState> implements PnioListener<WorkflowState> {
    @Override
    public void onTimeout( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.BAD_GATEWAY );
        exchange.endExchange();
    }

    @Override
    public void onException( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.INTERNAL_SERVER_ERROR );
        exchange.endExchange();
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.BAD_REQUEST );
        exchange.endExchange();
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.BAD_GATEWAY );
        exchange.endExchange();
    }

    @Override
    public void onRejected( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.TOO_MANY_REQUESTS );
        exchange.endExchange();
    }

    @Override
    public abstract void onDone( PnioExchange<WorkflowState> pnioExchange );

    public void onUnknown( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.INTERNAL_SERVER_ERROR );
        exchange.endExchange();
    }
}
