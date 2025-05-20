package oap.http.pniov2;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerNoContent<WorkflowState> implements PnioListener<WorkflowState> {
    protected void noContextResponse( PnioExchange<WorkflowState> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.NO_CONTENT );
        exchange.endExchange();
    }

    @Override
    public void onTimeout( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onException( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRejected( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public abstract void onDone( PnioExchange<WorkflowState> pnioExchange );

    public void onUnknown( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }
}
