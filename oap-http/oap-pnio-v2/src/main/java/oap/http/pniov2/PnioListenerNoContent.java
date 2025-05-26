package oap.http.pniov2;

public class PnioListenerNoContent<RequestState> implements PnioListener<RequestState> {
    protected void noContextResponse( PnioExchange<RequestState> pnioExchange ) {
        pnioExchange.httpResponse.responseNoContent();
        pnioExchange.send();
    }

    @Override
    public void onTimeout( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onException( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRequestBufferOverflow( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onResponseBufferOverflow( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRejected( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onDone( PnioExchange<RequestState> pnioExchange ) {
        pnioExchange.send();
    }

    public void onUnknown( PnioExchange<RequestState> pnioExchange ) {
        noContextResponse( pnioExchange );
    }
}
