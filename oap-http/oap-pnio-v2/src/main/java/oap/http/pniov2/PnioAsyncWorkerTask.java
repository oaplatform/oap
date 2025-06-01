package oap.http.pniov2;

import java.util.concurrent.TimeUnit;

public class PnioAsyncWorkerTask<RequestState, T, R extends AsyncTask<T, RequestState>> extends PnioWorkerTask<RequestState, ComputeTask<RequestState>> {
    protected final R asyncTask;
    public T result;

    public PnioAsyncWorkerTask( PnioExchange<RequestState> pnioExchange, R asyncTask ) {
        super( pnioExchange, new ComputeTask<RequestState>() {
            @Override
            public void run( PnioExchange<RequestState> pnioExchange ) {

            }
        } );
        this.asyncTask = asyncTask;
    }

    @Override
    protected void run() {
    }

    @Override
    public void fork( PnioWorkerThread pnioWorkerThread ) {
        try {
            asyncTask
                .apply( pnioExchange )
                .orTimeout( pnioExchange.getTimeLeftNano(), TimeUnit.NANOSECONDS )
                .whenComplete( ( result, t ) -> {
                    try {
                        if( t != null ) {
                            pnioExchange.completeWithFail( t );
                        } else {
                            this.result = result;
                        }
                    } finally {
                        state.set( COMPLETED );
                    }
                } );
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
            state.set( COMPLETED );
        }


        super.fork( pnioWorkerThread );
    }
}
