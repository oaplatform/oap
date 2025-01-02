package oap.http.pnio;

import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PnioExchanges<WorkflowState> implements Iterable<PnioExchange<WorkflowState>>, Runnable, AutoCloseable {
    public final LinkedBlockingQueue<PnioExchange<WorkflowState>> list = new LinkedBlockingQueue<>();
    private final Scheduled scheduled;

    public PnioExchanges() {
        scheduled = Scheduler.scheduleWithFixedDelay( 5, TimeUnit.SECONDS, this );
    }

    public void offer( PnioExchange<WorkflowState> pnioExchange ) {
        list.offer( pnioExchange );

        cleanup();
    }

    private void cleanup() {
        PnioExchange<WorkflowState> head;

        while( ( head = list.peek() ) != null && head.isDone() ) {
            synchronized( list ) {
                if( ( head = list.peek() ) != null && head.isDone() ) {
                    list.remove();
                }
            }
        }
    }

    @Override
    public Iterator<PnioExchange<WorkflowState>> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach( Consumer<? super PnioExchange<WorkflowState>> action ) {
        list.forEach( action );
    }

    @Override
    public Spliterator<PnioExchange<WorkflowState>> spliterator() {
        return list.spliterator();
    }

    @Override
    public void run() {
        cleanup();
    }

    @Override
    public void close() {
        Closeables.close( scheduled );
    }
}
