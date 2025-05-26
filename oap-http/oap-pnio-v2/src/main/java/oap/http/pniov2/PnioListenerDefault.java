package oap.http.pniov2;

import oap.http.Http;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerDefault<E extends AbstractPnioExchange<E>> implements PnioListener<E> {
    @Override
    public void onTimeout( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onException( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }

    protected void setResponseCode( E pnioExchange, int internalServerError ) {
        pnioExchange.httpResponse.status = internalServerError;
        pnioExchange.response();
    }

    @Override
    public void onRequestBufferOverflow( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_REQUEST );
    }

    @Override
    public void onResponseBufferOverflow( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onRejected( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.TOO_MANY_REQUESTS );
    }

    @Override
    public abstract void onDone( E pnioExchange );

    public void onUnknown( E pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
