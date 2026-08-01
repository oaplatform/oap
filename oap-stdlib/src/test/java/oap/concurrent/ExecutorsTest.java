package oap.concurrent;

import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorsTest {
    @Test
    public void testNewBlockingVirtualThreadPerTaskExecutorLimitsConcurrency() throws InterruptedException {
        int threads = 4;
        int tasks = 20;

        ExecutorService executor = Executors.newBlockingVirtualThreadPerTaskExecutor( threads );

        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        CountDownLatch done = new CountDownLatch( tasks );

        for( int i = 0; i < tasks; i++ ) {
            executor.execute( () -> {
                int current = running.incrementAndGet();
                maxRunning.updateAndGet( max -> Math.max( max, current ) );
                try {
                    Thread.sleep( 20 );
                } catch( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                    done.countDown();
                }
            } );
        }

        assertThat( done.await( 10, TimeUnit.SECONDS ) ).isTrue();
        assertThat( maxRunning.get() ).isLessThanOrEqualTo( threads );

        executor.shutdown();
        assertThat( executor.awaitTermination( 10, TimeUnit.SECONDS ) ).isTrue();
    }
}
