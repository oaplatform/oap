package oap.http.pniov2;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

public class PnioAsyncTask<T, RequestState> extends ForkJoinTask<T> {
    private final AsyncTask<T, RequestState> task;
    private final PnioExchange<RequestState> pnioExchange;
    protected T result;

    public PnioAsyncTask( AsyncTask<T, RequestState> task, PnioExchange<RequestState> pnioExchange ) {
        this.task = task;
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
            task.apply( pnioExchange )
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
