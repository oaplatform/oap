package oap.http.pniov2;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class PnioWorkQueue {
    private final AtomicInteger state = new AtomicInteger();
    private final int queueSize;
    private final LinkedBlockingDeque<PnioWorkerTask<?, ?>> queue = new LinkedBlockingDeque<>();

    public PnioWorkQueue( int queueSize ) {
        this.queueSize = queueSize;
    }

    public boolean tryPushTask( PnioWorkerTask<?, ?> task ) {
        if( queue.size() >= queueSize ) {
            return false;
        }

        forcePushTask( task );

        return true;
    }

    public void forcePushTask( PnioWorkerTask<?, ?> task ) {
        queue.addLast( task );
    }

    public PnioWorkerTask<?, ?> tryPeekTask() {
        if( queue.isEmpty() ) {
            return null;
        }

        return queue.removeFirst();
    }

    public PnioWorkerTask<?, ?> peekTask() throws InterruptedException {
        return queue.takeFirst();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
