package oap.http.pniov2;

import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerDefault implements PnioListener {
    @Override
    public void onTimeout( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onException( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }

    protected void setResponseCode( AbstractPnioExchange pnioExchange, int internalServerError ) {
        pnioExchange.httpResponse.status = internalServerError;
        pnioExchange.response();
    }

    @Override
    public void onRequestBufferOverflow( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_REQUEST );
    }

    @Override
    public void onResponseBufferOverflow( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onRejected( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.TOO_MANY_REQUESTS );
    }

    @Override
    public abstract void onDone( AbstractPnioExchange pnioExchange );

    public void onUnknown( AbstractPnioExchange pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
