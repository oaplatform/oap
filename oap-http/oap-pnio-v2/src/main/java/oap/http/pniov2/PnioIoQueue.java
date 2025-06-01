package oap.http.pniov2;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;

@NotThreadSafe
public class PnioIoQueue {
    private final PnioWorkQueue localThreadQueue;
    private final PnioWorkQueue[] queues;
    int rr = 0;

    public PnioIoQueue( int queueSize, PnioWorkQueue[] queues ) {
        localThreadQueue = new PnioWorkQueue( queueSize );
        this.queues = queues;
    }

    public void pushTask( PnioWorkerTask<?, ?> task, Consumer<PnioWorkerTask<?, ?>> rejected, boolean important ) {
        if( rr >= queues.length - 1 ) {
            rr = 0;
        }

        PnioWorkQueue queue = queues[rr];
        if( important ) {
            queue.forcePushTask( task );
        } else {
            if( !queue.tryPushTask( task ) ) {
                rejected.accept( task );
                PnioWorkerTask<?, ?> currentQueueTask;
                while( ( currentQueueTask = localThreadQueue.tryPeekTask() ) != null ) {
                    rejected.accept( currentQueueTask );
                }
            } else {
                copyTo( queue, rejected );
            }
        }
    }

    public void copyTo( PnioWorkQueue queue, Consumer<PnioWorkerTask<?, ?>> rejected ) {
        PnioWorkerTask<?, ?> currentQueueTask;
        while( ( currentQueueTask = localThreadQueue.tryPeekTask() ) != null ) {

            if( !queue.tryPushTask( currentQueueTask ) ) {
                rejected.accept( currentQueueTask );
            }
        }
    }
}
