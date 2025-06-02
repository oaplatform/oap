package oap.http.pniov2;

import org.assertj.core.api.CompletableFutureAssert;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PnioWorkQueueTest {
    @Test( invocationCount = 1000 )
    public void testQueue() throws InterruptedException {
        PnioWorkerTask.counter.set( 0L );
        PnioWorkQueue pnioWorkQueue = new PnioWorkQueue( 5 );

        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );


        PnioWorkerTask<?, ?> task = pnioWorkQueue.takeTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 1L );


        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        assertFalse( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        task = pnioWorkQueue.takeTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 2L );

        task = pnioWorkQueue.takeTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 3L );

        pnioWorkQueue.takeTask();
        pnioWorkQueue.takeTask();
        pnioWorkQueue.takeTask();

        CompletableFutureAssert<? extends PnioWorkerTask<?, ?>> pnioWorkerTaskCompletableFutureAssert = assertThat( CompletableFuture.supplyAsync( () -> {
            try {
                return pnioWorkQueue.takeTask();
            } catch( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        } ) );

        pnioWorkQueue.signal();

        pnioWorkerTaskCompletableFutureAssert
            .succeedsWithin( 10, TimeUnit.SECONDS )
            .isNull();
    }

}
