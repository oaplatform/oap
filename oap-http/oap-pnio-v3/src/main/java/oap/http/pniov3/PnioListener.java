package oap.http.pniov3;

public interface PnioListener<RequestState> {
    void onTimeout( PnioExchange<RequestState> pnioExchange );

    void onException( PnioExchange<RequestState> pnioExchange );

    void onRequestBufferOverflow( PnioExchange<RequestState> pnioExchange );

    void onResponseBufferOverflow( PnioExchange<RequestState> pnioExchange );

    void onRejected( PnioExchange<RequestState> pnioExchange );

    void onDone( PnioExchange<RequestState> pnioExchange );

    void onUnknown( PnioExchange<RequestState> pnioExchange );
}
