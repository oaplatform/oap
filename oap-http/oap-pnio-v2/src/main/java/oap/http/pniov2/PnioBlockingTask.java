package oap.http.pniov2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

public class PnioBlockingTask<State> extends ForkJoinTask<Void> {
    private final BlockingTask<State> task;
    private final State state;
    private final PnioExchange<State> pnioExchange;
    private final PnioController pnioController;

    public PnioBlockingTask( BlockingTask<State> task, State state, PnioExchange<State> pnioExchange, PnioController pnioController ) {
        this.task = task;
        this.state = state;
        this.pnioExchange = pnioExchange;
        this.pnioController = pnioController;
    }

    @Override
    public Void getRawResult() {
        return null;
    }

    @Override
    protected void setRawResult( Void value ) {

    }

    @Override
    protected boolean exec() {
        try {
            CompletableFuture
                .runAsync( () -> {
                    try {
                        task.accept( pnioExchange, state );
                    } catch( Throwable e ) {
                        pnioExchange.completeWithFail( e );
                    }
                }, pnioController.blockingPool )
                .orTimeout( pnioExchange.getTimeLeftNano(), TimeUnit.NANOSECONDS )
                .whenComplete( ( _, t ) -> {
                    if( t != null ) {
                        pnioExchange.completeWithFail( t );
                    }
                    quietlyComplete();
                } );

            return false;
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
            return true;
        }
    }
}
