package oap.http.pniov2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oap.concurrent.Executors;
import oap.io.Closeables;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class PnioWorkers implements AutoCloseable {
    public final ExecutorService pool;
    public final PnioWorker[] workers;
    public final int maxQueueSize;
    public final AtomicInteger queueSize = new AtomicInteger();
    private final LinkedBlockingDeque<PnioTask<?>> queue;

    public PnioWorkers( int threads, int maxQueueSize ) {
        pool = Executors.newFixedThreadPool( threads > 0 ? threads : Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat( "PNIO - CPU-%d" ).build() );

        workers = new PnioWorker[threads];

        queue = new LinkedBlockingDeque<>( maxQueueSize );
        this.maxQueueSize = maxQueueSize;

        for( int i = 0; i < threads; i++ ) {
            PnioWorker pnioWorker = new PnioWorker( queue, queueSize );
            pool.execute( pnioWorker );
            workers[i] = pnioWorker;
        }
    }

    public boolean register( PnioExchange<?> pnioExchange, PnioTask<?> task, boolean important ) {
        if( important ) {
            if( queue.offerFirst( task ) ) {
                queueSize.incrementAndGet();
            } else {
                pnioExchange.completeWithRejected();
                pnioExchange.response();

                return false;
            }
        } else {
            if( queueSize.get() >= maxQueueSize || !queue.offerLast( task ) ) {
                pnioExchange.completeWithRejected();
                pnioExchange.response();

                return false;
            } else {
                queueSize.incrementAndGet();
            }
        }

        return true;
    }

    @Override
    public void close() {
        pool.shutdown();

        for( PnioWorker worker : workers ) {
            worker.interrupt();
        }

        Closeables.close( pool );
    }
}
