package oap.http.pniov2;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;

@NotThreadSafe
public class PnioIoQueue {
    private final PnioWorkQueue[] queues;
    int rr = 0;

    public PnioIoQueue( int queueSize, PnioWorkQueue[] queues ) {
        this.queues = queues;
    }

    public void pushTask( PnioWorkerTask<?, ?> task, Consumer<PnioWorkerTask<?, ?>> rejected, boolean important ) {
        PnioWorkQueue queue = nextQueue();
        if( important ) {
            queue.forcePushTask( task );
        } else {
            if( !queue.tryPushTask( task ) ) {
                rejected.accept( task );
            }
        }
    }

    public PnioWorkQueue nextQueue() {
        rr++;

        if( rr > queues.length - 1 ) {
            rr = 0;
        }

        return queues[rr];
    }
}
