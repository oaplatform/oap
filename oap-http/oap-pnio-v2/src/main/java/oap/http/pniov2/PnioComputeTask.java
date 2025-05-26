package oap.http.pniov2;

import java.nio.BufferOverflowException;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RejectedExecutionException;

public class PnioComputeTask extends RecursiveAction {
    private final ComputeTask task;
    private final AbstractPnioExchange pnioExchange;

    public PnioComputeTask( ComputeTask task, AbstractPnioExchange pnioExchange ) {
        this.task = task;
        this.pnioExchange = pnioExchange;
    }

    @Override
    protected void compute() {
        try {
            task.accept( pnioExchange );
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
