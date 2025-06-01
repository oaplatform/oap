package oap.http.pniov2;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PnioWorkQueueTest {
    @Test
    public void testQueue() {
        PnioWorkQueue pnioWorkQueue = new PnioWorkQueue( 5 );

        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );
        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );


        PnioWorkerTask<?, ?> task = pnioWorkQueue.tryPeekTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 1L );


        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        assertTrue( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        assertFalse( pnioWorkQueue.tryPushTask( new PnioWorkerTask<>( null, null ) ) );

        task = pnioWorkQueue.tryPeekTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 2L );

        task = pnioWorkQueue.tryPeekTask();
        assertNotNull( task );
        assertThat( task.id ).isEqualTo( 3L );
    }

}
