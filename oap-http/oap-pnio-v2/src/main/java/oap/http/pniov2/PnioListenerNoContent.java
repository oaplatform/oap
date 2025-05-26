package oap.http.pniov2;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerNoContent<State> implements PnioListener {
    protected void noContextResponse( AbstractPnioExchange pnioExchange ) {
        pnioExchange.httpResponse.responseNoContent();
        pnioExchange.response();
    }

    @Override
    public void onTimeout( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onException( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRequestBufferOverflow( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onResponseBufferOverflow( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRejected( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public abstract void onDone( AbstractPnioExchange pnioExchange );

    public void onUnknown( AbstractPnioExchange pnioExchange ) {
        noContextResponse( pnioExchange );
    }
}
