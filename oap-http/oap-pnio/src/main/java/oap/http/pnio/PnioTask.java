package oap.http.pnio;

import lombok.SneakyThrows;
import org.joda.time.DateTimeUtils;

import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.TreeSet;

public class PnioTask<WorkflowState> implements Runnable {
    private static Field selectorWorkQueueField;
    private static Field workLockField;
    private static Field selectorField;
    private static Field delayWorkQueueField;

    static {
        init();
    }

    public final PnioExchange<WorkflowState> pnioExchange;
    protected final ArrayDeque<Runnable> selectorWorkQueue;
    protected final Object workLock;
    protected final Selector selector;
    protected final TreeSet<?> delayWorkQueue;

    @SneakyThrows
    @SuppressWarnings( "unchecked" )
    public PnioTask( Object workerThread, PnioExchange<WorkflowState> pnioExchange ) {
        selectorWorkQueue = ( ArrayDeque<Runnable> ) selectorWorkQueueField.get( workerThread );
        delayWorkQueue = ( TreeSet<?> ) delayWorkQueueField.get( workerThread );
        workLock = workLockField.get( workerThread );
        selector = ( Selector ) selectorField.get( workerThread );

        this.pnioExchange = pnioExchange;
    }

    @SneakyThrows
    private static void init() {
        Class<?> workerThreadClass = Class.forName( "org.xnio.nio.WorkerThread" );

        selectorWorkQueueField = workerThreadClass.getDeclaredField( "selectorWorkQueue" );
        selectorWorkQueueField.setAccessible( true );

        delayWorkQueueField = workerThreadClass.getDeclaredField( "delayWorkQueue" );
        delayWorkQueueField.setAccessible( true );

        workLockField = workerThreadClass.getDeclaredField( "workLock" );
        workLockField.setAccessible( true );

        selectorField = workerThreadClass.getDeclaredField( "selector" );
        selectorField.setAccessible( true );
    }

    public boolean isTimeout( long now ) {
        return ( System.nanoTime() - pnioExchange.getRequestStartTime() ) > pnioExchange.timeoutNano;
    }

    @Override
    public void run() {
        pnioExchange.process();

        synchronized( workLock ) {
            long now = DateTimeUtils.currentTimeMillis();

            Iterator<Runnable> iterator = selectorWorkQueue.descendingIterator();
            while( iterator.hasNext() ) {
                Runnable runnable = iterator.next();
                if( runnable instanceof PnioTask<?> pnioTask ) {
                    if( pnioTask.pnioExchange.timeoutNano > 0 ) {
                        if( pnioTask.isTimeout( now ) ) {
                            iterator.remove();

                            pnioExchange.pnioListener.fireOnTimeout( pnioExchange );
                        } else {
                            break;
                        }
                    } else if( pnioExchange.maxQueueSize > selectorWorkQueue.size() ) {
                        iterator.remove();

                        pnioExchange.pnioListener.fireOnRejected( pnioExchange );
                    }
                }
            }
        }
    }

    public void register( boolean init ) {
        synchronized( workLock ) {
            if( init ) {
                if( selectorWorkQueue.size() > pnioExchange.maxQueueSize ) {
                    pnioExchange.pnioListener.fireOnRejected( pnioExchange );
                    return;
                }
            }
            selectorWorkQueue.add( this );
        }
        selector.wakeup();
    }
}
