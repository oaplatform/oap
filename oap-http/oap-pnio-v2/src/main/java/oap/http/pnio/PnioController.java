package oap.http.pnio;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.io.Closeables;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PnioController implements AutoCloseable {
    public final ExecutorService blockingPool;
    public final PnioWorkers workers;

    public final int threads;
    public final int blockingPoolSize;
    public final int maxQueueSize;


    public PnioController( int threads, int blockingPoolSize, int maxQueueSize ) {
        this.threads = threads;
        this.blockingPoolSize = blockingPoolSize;
        this.maxQueueSize = maxQueueSize;

        Preconditions.checkArgument( maxQueueSize > 0, "maxQueueSize must be greater than 0" );

        blockingPool = blockingPoolSize > 0
            ? Executors.newFixedThreadPool( blockingPoolSize, new ThreadFactoryBuilder().setNameFormat( "PNIO - BLK-%d" ).build() )
            : null;

        workers = new PnioWorkers( threads, maxQueueSize );
    }

    @Override
    public void close() {
        Closeables.close( workers );
        Closeables.close( blockingPool );
    }

    public CompletableFuture<Void> runAsync( Runnable runnable ) {
        return CompletableFuture.runAsync( runnable, blockingPool );
    }

    public <WorkflowState> boolean register( PnioExchange<WorkflowState> pnioExchange, PnioTask<WorkflowState> workflowStatePnioTask ) {
        return workers.register( pnioExchange, workflowStatePnioTask );
    }
}
