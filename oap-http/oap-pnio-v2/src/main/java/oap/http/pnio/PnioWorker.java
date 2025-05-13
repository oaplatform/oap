package oap.http.pnio;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;

public class PnioWorker implements Runnable {
    public final BlockingQueue<PnioTask<?>> queue;

    public boolean done = false;
    @Nullable
    public Thread thread;

    public PnioWorker( BlockingQueue<PnioTask<?>> queue ) {
        this.queue = queue;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();

        while( !done && !thread.isInterrupted() ) {
            try {
                PnioTask<?> task = queue.take();
                try {
                    task.pnioExchange.process();
                } catch( BufferOverflowException e ) {
                    task.pnioExchange.completeWithBufferOverflow( false );
                    task.pnioExchange.response();
                } catch( Throwable t ) {
                    task.pnioExchange.completeWithFail( t );
                    if( !task.pnioExchange.oapExchange.isResponseStarted() ) {
                        task.pnioExchange.response();
                    }
                }
            } catch( InterruptedException t ) {
                done = true;
            }
        }
    }

    public void interrupt() {
        if( thread != null ) {
            thread.interrupt();
        }
    }
}
