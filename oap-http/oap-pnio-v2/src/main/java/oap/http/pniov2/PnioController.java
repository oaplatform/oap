package oap.http.pniov2;

import oap.io.Closeables;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

public class PnioController implements AutoCloseable {
    private final ForkJoinPool forkJoinPool;

    public PnioController( int parallelism ) {
        forkJoinPool = parallelism > 0 ? new ForkJoinPool( parallelism ) : new ForkJoinPool();
    }

    @Override
    public void close() {
        Closeables.close( forkJoinPool );
    }

    public boolean submit( PnioComputeTask task ) {
        try {
            forkJoinPool.submit( task );

            return true;
        } catch( RejectedExecutionException e ) {
            return false;
        }
    }
}
