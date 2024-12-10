package oap.http.pnio;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListener<WorkflowState> {
    static void noContextResponse( oap.http.server.nio.HttpServerExchange pnioExchange ) {
        HttpServerExchange exchange = pnioExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.NO_CONTENT );
        exchange.endExchange();
    }

    public final void fireOnTimeout( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.TIMEOUT.increment();

        onTimeout( pnioExchange );
    }

    public void onTimeout( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }

    public final void fireOnException( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.EXCEPTION.increment();

        onException( pnioExchange );
    }

    public void onException( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }

    public final void fireOnRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.REQUEST_BUFFER_OVERFLOW.increment();

        onRequestBufferOverflow( pnioExchange );
    }

    public void onRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }

    public final void fireOnResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.RESPONSE_BUFFER_OVERFLOW.increment();

        onResponseBufferOverflow( pnioExchange );
    }

    public void onResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }

    public final void fireOnRejected( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.REJECTED.increment();

        onRejected( pnioExchange );
    }

    public void onRejected( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }

    public final void fireOnDone( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.COMPLETED.increment();

        onDone( pnioExchange );
    }

    public abstract void onDone( PnioExchange<WorkflowState> pnioExchange );

    public final void fireOnUnknown( PnioExchange<WorkflowState> pnioExchange ) {
        PnioMetrics.UNKNOWN.increment();

        onUnknown( pnioExchange );
    }

    public void onUnknown( PnioExchange<WorkflowState> pnioExchange ) {
        noContextResponse( pnioExchange.oapExchange );
    }
}
