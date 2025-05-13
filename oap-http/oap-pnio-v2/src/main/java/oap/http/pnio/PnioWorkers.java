package oap.http.pnio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oap.concurrent.Executors;
import oap.io.Closeables;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

public class PnioWorkers implements AutoCloseable {
    public final ExecutorService pool;
    public final PnioWorker[] workers;
    private final ArrayBlockingQueue<PnioTask<?>> queue;

    public PnioWorkers( int threads, int maxQueueSize ) {
        pool = Executors.newFixedThreadPool( threads > 0 ? threads : Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat( "PNIO - CPU-%d" ).build() );

        workers = new PnioWorker[threads];

        queue = new ArrayBlockingQueue<>( maxQueueSize, true );

        for( int i = 0; i < threads; i++ ) {
            PnioWorker pnioWorker = new PnioWorker( queue );
            pool.execute( pnioWorker );
            workers[i] = pnioWorker;
        }
    }

    public boolean register( PnioExchange<?> pnioExchange, PnioTask<?> task ) {
        if( !queue.offer( task ) ) {
            pnioExchange.completeWithRejected();
            pnioExchange.response();

            return false;
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
