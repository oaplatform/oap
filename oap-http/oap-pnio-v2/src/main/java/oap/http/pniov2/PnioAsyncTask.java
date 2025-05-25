package oap.http.pniov2;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

public class PnioAsyncTask<State> extends ForkJoinTask<Void> {
    private final AsyncTask<State> task;
    private final State state;
    private final PnioExchange<State> pnioExchange;

    public PnioAsyncTask( AsyncTask<State> task, State state, PnioExchange<State> pnioExchange ) {
        this.task = task;
        this.state = state;
        this.pnioExchange = pnioExchange;
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
            task.apply( pnioExchange, state )
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
