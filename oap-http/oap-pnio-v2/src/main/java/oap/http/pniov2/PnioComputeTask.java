package oap.http.pniov2;

import java.nio.BufferOverflowException;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RejectedExecutionException;

public class PnioComputeTask<RequestState> extends RecursiveAction {
    private final ComputeTask<RequestState> task;
    private final PnioExchange<RequestState> pnioExchange;

    public PnioComputeTask( ComputeTask<RequestState> task, PnioExchange<RequestState> pnioExchange ) {
        this.task = task;
        this.pnioExchange = pnioExchange;
    }

    @Override
    protected void compute() {
        try {
            task.accept( pnioExchange );
            pnioExchange.complete();
            pnioExchange.response();
        } catch( RejectedExecutionException e ) {
            pnioExchange.completeWithRejected();
            pnioExchange.response();
        } catch( BufferOverflowException e ) {
            pnioExchange.completeWithBufferOverflow( false );
            pnioExchange.response();
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
            pnioExchange.response();
        }
    }
}
