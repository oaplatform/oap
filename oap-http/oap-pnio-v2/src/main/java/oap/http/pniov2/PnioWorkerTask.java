package oap.http.pniov2;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferOverflowException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@ToString
public class PnioWorkerTask<RequestState, R extends ComputeTask<RequestState>> {
    public static final AtomicLong counter = new AtomicLong();
    protected static final int COMPLETED = 0x1;
    protected static final int FORKED = 0x2;
    protected static final int INIT = 0;
    public final long id;
    protected final PnioExchange<RequestState> pnioExchange;
    protected final R computeTask;
    protected final AtomicInteger state = new AtomicInteger( INIT );

    public PnioWorkerTask( PnioExchange<RequestState> pnioExchange, R computeTask ) {
        this.pnioExchange = pnioExchange;
        this.computeTask = computeTask;
        id = counter.incrementAndGet();
    }

    @SuppressWarnings( "checkstyle:CatchParameterName" )
    protected void run() {
        try {
            computeTask.run( pnioExchange );
        } catch( PnioForceTerminateException _ ) {
            pnioExchange.response();
        } catch( BufferOverflowException e ) {
            pnioExchange.completeWithBufferOverflow( false );
            pnioExchange.response();
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
            if( log.isTraceEnabled() ) {
                log.trace( e.getMessage(), e );
            }
            pnioExchange.response();
        }
        state.set( COMPLETED );
    }

    public void join( PnioWorkerThread pnioWorkerThread ) {
        PnioWorkQueue workQueue = pnioWorkerThread.workQueue;

        run();

        while( state.get() != COMPLETED ) {
            try {
                PnioWorkerTask<?, ?> pnioWorkerTask = workQueue.takeTask();
                if( pnioWorkerTask != null ) {
                    pnioWorkerTask.run();
                }
            } catch( InterruptedException e ) {
                pnioExchange.completeWithFail( e );
            }
        }
    }

    public void fork( PnioWorkerThread pnioWorkerThread ) {
        state.compareAndSet( 0, FORKED );
    }
}
