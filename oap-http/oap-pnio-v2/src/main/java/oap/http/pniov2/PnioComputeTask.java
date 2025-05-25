package oap.http.pniov2;

import java.nio.BufferOverflowException;
import java.util.concurrent.RecursiveAction;

public class PnioComputeTask<State> extends RecursiveAction {
    private final ComputeTask<State> task;
    private final PnioExchange<State> pnioExchange;
    private final State state;

    public PnioComputeTask( ComputeTask<State> task, PnioExchange<State> pnioExchange, State state ) {
        this.task = task;
        this.pnioExchange = pnioExchange;
        this.state = state;
    }

    @Override
    protected void compute() {
        try {
            task.accept( pnioExchange, state );
        } catch( BufferOverflowException e ) {
            pnioExchange.completeWithBufferOverflow( false );
            pnioExchange.response();
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
            pnioExchange.response();
        }
    }
}
