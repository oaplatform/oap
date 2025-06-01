package oap.http.pniov2;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PnioController implements AutoCloseable {
    private final PnioWorkerThread[] pool;
    private final AtomicLong rr = new AtomicLong();
    public volatile boolean done;

    public PnioController( int parallelism, int threadQueueSize ) {
        pool = new PnioWorkerThread[parallelism];
        for( int i = 0; i < parallelism; i++ ) {
            pool[i] = new PnioWorkerThread( this, threadQueueSize );
        }

        for( int i = 0; i < parallelism; i++ ) {
            pool[i].start();
        }
    }

    @Override
    public void close() {
        done = true;

        for( PnioWorkerThread thread : pool ) {
            try {
                thread.interrupt();
                thread.join();
            } catch( InterruptedException e ) {
                log.trace( e.getMessage(), e );
            }
        }
    }

    public PnioWorkQueue[] getPnioWorkQueues() {
        PnioWorkQueue[] queues = new PnioWorkQueue[pool.length];

        for( int i = 0; i < pool.length; i++ ) {
            queues[i] = pool[i].workQueue;
        }

        return queues;
    }

    public long getTaskCount() {
        long count = 0;

        for( PnioWorkerThread thread : pool ) {
            count += thread.workQueue.size();
        }

        return count;
    }
}
