package oap.http.pnio;

import io.undertow.server.HttpServerExchange;
import lombok.SneakyThrows;
import org.joda.time.DateTimeUtils;

import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public abstract class AbstractPnioTask implements Runnable {
    private static Field selectorWorkQueueField;
    private static Field workLockField;
    private static Field selectorField;
    private static Field delayWorkQueueField;

    static {
        init();
    }

    public final long timeoutNano;
    public final HttpServerExchange exchange;
    protected final ArrayDeque<Runnable> selectorWorkQueue;
    protected final Object workLock;
    protected final Selector selector;
    protected final TreeSet<?> delayWorkQueue;

    public AbstractPnioTask( Object workerThread ) {
        this( workerThread, null, -1 );
    }

    @SneakyThrows
    @SuppressWarnings( "unchecked" )
    public AbstractPnioTask( Object workerThread, HttpServerExchange exchange, long timeout ) {
        selectorWorkQueue = ( ArrayDeque<Runnable> ) selectorWorkQueueField.get( workerThread );
        delayWorkQueue = ( TreeSet<?> ) delayWorkQueueField.get( workerThread );
        workLock = workLockField.get( workerThread );
        selector = ( Selector ) selectorField.get( workerThread );

        this.exchange = exchange;
        this.timeoutNano = timeout * 1_000_000;
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
        return exchange != null && now - exchange.getRequestStartTime() > timeoutNano;
    }

    @Override
    public void run() {
        ArrayList<AbstractPnioTask> tasks = new ArrayList<>();

        synchronized( workLock ) {
            long now = DateTimeUtils.currentTimeMillis();

            Iterator<Runnable> iterator = selectorWorkQueue.descendingIterator();
            while( iterator.hasNext() ) {
                Runnable runnable = iterator.next();
                if( runnable instanceof AbstractPnioTask pnioTask ) {
                    if( pnioTask.timeoutNano > 0 ) {
                        if( pnioTask.isTimeout( now ) ) {
                            iterator.remove();
                            tasks.add( new PnioTimeoutTask( pnioTask.exchange ) );
                        } else {
                            break;
                        }
                    }
                }
            }

            for( AbstractPnioTask task : tasks ) {
                selectorWorkQueue.addFirst( task );
            }
        }
        if( !tasks.isEmpty() ) {
            selector.wakeup();
        }
    }
}
