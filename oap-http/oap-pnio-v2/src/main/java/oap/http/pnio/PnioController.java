package oap.http.pnio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.io.Closeables;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PnioController implements AutoCloseable {
    public final ExecutorService blockingPool;
    public final int blockingPoolSize;

    public PnioController( int blockingPoolSize ) {
        blockingPool = blockingPoolSize > 0
            ? Executors.newFixedThreadPool( blockingPoolSize, new ThreadFactoryBuilder().setNameFormat( "PNIO - BLK-%d" ).build() )
            : null;
        this.blockingPoolSize = blockingPoolSize;
    }

    @Override
    public void close() {
        Closeables.close( blockingPool );
    }

    public CompletableFuture<Void> runAsync( Runnable runnable ) {
        return CompletableFuture.runAsync( runnable, blockingPool );
    }
}
