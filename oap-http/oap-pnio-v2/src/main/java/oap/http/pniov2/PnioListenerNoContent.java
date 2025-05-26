package oap.http.pniov2;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioListenerNoContent<E extends AbstractPnioExchange<E>> implements PnioListener<E> {
    protected void noContextResponse( E pnioExchange ) {
        pnioExchange.httpResponse.responseNoContent();
        pnioExchange.response();
    }

    @Override
    public void onTimeout( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onException( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRequestBufferOverflow( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onResponseBufferOverflow( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public void onRejected( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }

    @Override
    public abstract void onDone( E pnioExchange );

    public void onUnknown( E pnioExchange ) {
        noContextResponse( pnioExchange );
    }
}
