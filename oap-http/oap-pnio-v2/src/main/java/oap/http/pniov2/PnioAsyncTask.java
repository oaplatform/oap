package oap.http.pniov2;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

public class PnioAsyncTask<T, State> extends ForkJoinTask<T> {
    private final AsyncTask<T, State> task;
    private final State state;
    private final PnioExchange<State> pnioExchange;
    protected T result;

    public PnioAsyncTask( AsyncTask<T, State> task, State state, PnioExchange<State> pnioExchange ) {
        this.task = task;
        this.state = state;
        this.pnioExchange = pnioExchange;
    }

    @Override
    public T getRawResult() {
        return result;
    }

    @Override
    protected void setRawResult( T value ) {
        this.result = value;
    }

    @Override
    protected boolean exec() {
        try {
            task.apply( pnioExchange, state )
                .orTimeout( pnioExchange.getTimeLeftNano(), TimeUnit.NANOSECONDS )
                .whenComplete( ( result, t ) -> {
                    if( t != null ) {
                        pnioExchange.completeWithFail( t );
                    } else {
                        PnioAsyncTask.this.result = result;
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
