/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import lombok.extern.slf4j.Slf4j;
import oap.highload.Affinity;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import static oap.http.pnio.PnioRequestHandler.Type.COMPUTE;

@Slf4j
class RequestTaskComputeRunner<WorkflowState> implements Runnable {
    private final BlockingQueue<PnioExchange<WorkflowState>> queue;
    private final Affinity affinity;
    volatile boolean done = false;
    private Thread thread;

    RequestTaskComputeRunner( BlockingQueue<PnioExchange<WorkflowState>> queue, Affinity affinity ) {
        this.queue = Objects.requireNonNull( queue );
        this.affinity = affinity;
    }

    @Override
    public void run() {
        this.thread = Thread.currentThread();
        affinity.set();
        Thread.currentThread().setPriority( Thread.MAX_PRIORITY );

        while( !done ) {
            try {
                PnioExchange<WorkflowState> pnioExchange = queue.take();
                try {
                    pnioExchange.runTasks( COMPUTE );
                    pnioExchange.completeFuture();
                } catch( Throwable e ) {
                    pnioExchange.completeWithFail( e );
                }
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                interrupt();
                break;
            }
        }
    }

    public void interrupt() {
        done = true;
        thread.interrupt();
    }
}
