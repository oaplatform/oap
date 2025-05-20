package oap.http.pniov2;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerDefault<WorkflowState> implements PnioListener<WorkflowState> {
    @Override
    public void onTimeout( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onException( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }

    protected <WorkflowState> void setResponseCode( PnioExchange<WorkflowState> pnioExchange, int internalServerError ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( internalServerError );
        exchange.endExchange();
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_REQUEST );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onRejected( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.TOO_MANY_REQUESTS );
    }

    @Override
    public abstract void onDone( PnioExchange<WorkflowState> pnioExchange );

    public void onUnknown( PnioExchange<WorkflowState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
