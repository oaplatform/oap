package oap.http.pniov2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oap.concurrent.Executors;
import oap.io.Closeables;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

public class PnioController implements AutoCloseable {
    public final ExecutorService blockingPool;
    public final int blockingPoolSize;
    private final ForkJoinPool forkJoinPool;

    public PnioController( int parallelism, int blockingPoolSize ) {
        this.blockingPoolSize = blockingPoolSize;
        forkJoinPool = parallelism > 0 ? new ForkJoinPool( parallelism ) : new ForkJoinPool();

        blockingPool = blockingPoolSize > 0
            ? Executors.newFixedThreadPool( blockingPoolSize, new ThreadFactoryBuilder().setNameFormat( "PNIO - BLK-%d" ).build() )
            : null;
    }

    @Override
    public void close() {
        Closeables.close( forkJoinPool );
        Closeables.close( blockingPool );
    }

    public boolean submit( PnioComputeTask<?> task ) {
        try {
            forkJoinPool.submit( task );

            return true;
        } catch( RejectedExecutionException e ) {
            return false;
        }
    }
}
