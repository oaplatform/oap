package oap.http.pnio;

public class PnioTask<WorkflowState> implements Runnable {
    public final PnioExchange<WorkflowState> pnioExchange;

    public PnioTask( PnioExchange<WorkflowState> pnioExchange ) {
        this.pnioExchange = pnioExchange;
    }

    public boolean isTimeout( long now ) {
        return ( System.nanoTime() - pnioExchange.getRequestStartTime() ) > pnioExchange.timeoutNano;
    }

    @Override
    public void run() {
        pnioExchange.process();
    }
}
