package oap.http.pniov2;

public interface PnioListener<WorkflowState> {
    void onTimeout( PnioExchange<WorkflowState> pnioExchange );

    void onException( PnioExchange<WorkflowState> pnioExchange );

    void onRequestBufferOverflow( PnioExchange<WorkflowState> pnioExchange );

    void onResponseBufferOverflow( PnioExchange<WorkflowState> pnioExchange );

    void onRejected( PnioExchange<WorkflowState> pnioExchange );

    void onDone( PnioExchange<WorkflowState> pnioExchange );

    void onUnknown( PnioExchange<WorkflowState> pnioExchange );
}
