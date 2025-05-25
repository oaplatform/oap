package oap.http.pniov2;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerDefault<State> implements PnioListener<State> {
    @Override
    public void onTimeout( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onException( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }

    protected <State> void setResponseCode( PnioExchange<State> pnioExchange, int internalServerError ) {
        oap.http.server.nio.HttpServerExchange oapExchange = pnioExchange.oapExchange;
        HttpServerExchange exchange = oapExchange.exchange;
        exchange.setStatusCode( internalServerError );
        exchange.endExchange();
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_REQUEST );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onRejected( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.TOO_MANY_REQUESTS );
    }

    @Override
    public abstract void onDone( PnioExchange<State> pnioExchange );

    public void onUnknown( PnioExchange<State> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
