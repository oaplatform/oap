package oap.http.pniov2;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class PnioWorkerThread extends Thread {
    public final PnioController pnioController;
    public final PnioWorkQueue workQueue;

    public PnioWorkerThread( PnioController pnioController, int queueSize ) {
        this.pnioController = pnioController;
        this.workQueue = new PnioWorkQueue( queueSize );
        setName( "PNIO-" + threadId() );
    }

    @Override
    public void run() {
        while( !pnioController.done ) {
            try {
                PnioWorkerTask<?, ?> currentTask = workQueue.takeTask();
                if( currentTask != null ) {
                    PnioExchange<?> pnioExchange = currentTask.pnioExchange;
                    if( pnioExchange.isTimeout() ) {
                        pnioExchange.completeWithTimeout();
                        pnioExchange.response();
                    } else {
                        currentTask.fork( this );
                        currentTask.join( this );
                    }
                }
            } catch( InterruptedException ignored ) {

            }
        }
    }
}
