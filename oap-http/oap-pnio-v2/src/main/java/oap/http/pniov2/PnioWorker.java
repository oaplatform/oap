package oap.http.pniov2;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PnioWorker implements Runnable {
    public final BlockingQueue<PnioTask<?>> queue;
    private final AtomicInteger queueSize;

    public boolean done = false;
    @Nullable
    public Thread thread;

    public PnioWorker( BlockingQueue<PnioTask<?>> queue, AtomicInteger queueSize ) {
        this.queue = queue;
        this.queueSize = queueSize;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();

        while( !done && !thread.isInterrupted() ) {
            try {
                PnioTask<?> task = queue.take();
                queueSize.decrementAndGet();
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
