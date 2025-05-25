package oap.http.pniov2;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerNoContent<State> implements PnioListener<State> {
    protected void noContextResponse( PnioExchange<State> pnioExchange ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( Http.StatusCode.NO_CONTENT );
        exchange.endExchange();
    }

    @Override
    public void onTimeout( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onException( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRejected( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public abstract void onDone( PnioExchange<State> pnioExchange );

    public void onUnknown( PnioExchange<State> pnioExchange ) {
        noContextResponse( pnioExchange );
    }
}
