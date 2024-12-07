package oap.http.pnio;

import io.undertow.server.HttpServerExchange;

public class PnioTask extends AbstractPnioTask {
    private final Runnable task;

    public PnioTask( Object workerThread, HttpServerExchange exchange, long timeout, Runnable task ) {
        super( workerThread, exchange, timeout );
        this.task = task;
    }

    @Override
    public void run() {
        task.run();

        super.run();
    }

    public void register() {
        synchronized( workLock ) {
            selectorWorkQueue.add( this );
        }
        selector.wakeup();
    }
}
