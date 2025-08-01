package oap.http.pniov3;

import oap.http.Http;

public class PnioListenerDefault<RequestState> implements PnioListener<RequestState> {
    @Override
    public void onTimeout( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onException( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }

    protected void setResponseCode( PnioExchange<RequestState> pnioExchange, int internalServerError ) {
        pnioExchange.httpResponse.status = internalServerError;
        pnioExchange.send();
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_REQUEST );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.BAD_GATEWAY );
    }

    @Override
    public void onRejected( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.TOO_MANY_REQUESTS );
    }

    @Override
    public void onDone( PnioExchange<RequestState> pnioExchange ) {
        pnioExchange.send();
    }

    public void onUnknown( PnioExchange<RequestState> pnioExchange ) {
        setResponseCode( pnioExchange, Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
