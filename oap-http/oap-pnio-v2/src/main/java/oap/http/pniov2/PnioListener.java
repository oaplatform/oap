package oap.http.pniov2;

public interface PnioListener<E extends AbstractPnioExchange<E>> {
    void onTimeout( E pnioExchange );

    void onException( E pnioExchange );

    void onRequestBufferOverflow( E pnioExchange );

    void onResponseBufferOverflow( E pnioExchange );

    void onRejected( E pnioExchange );

    void onDone( E pnioExchange );

    void onUnknown( E pnioExchange );
}
