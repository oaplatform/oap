/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pniov2;

import com.google.common.base.Preconditions;
import io.undertow.io.Receiver;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PnioHttpHandler<RequestState> implements PnioHttpHandlerReference {
    private static final ThreadLocal<PnioIoQueue> queue = new ThreadLocal<>();
    public final int requestSize;
    public final int responseSize;
    public final int ioQueueSize;
    public final boolean important;
    public final PnioListener<RequestState> pnioListener;
    public final ConcurrentHashMap<Long, PnioExchange<RequestState>> exchanges = new ConcurrentHashMap<>();
    private final PnioController pnioController;
    public ComputeTask<RequestState> task;

    public PnioHttpHandler( PnioHttpSettings settings,
                            ComputeTask<RequestState> task,
                            PnioListener<RequestState> pnioListener,
                            PnioController pnioController ) {
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;
        this.ioQueueSize = settings.ioQueueSize;
        this.important = settings.important;

        this.task = task;
        this.pnioListener = pnioListener;
        this.pnioController = pnioController;

        Preconditions.checkArgument( settings.responseSize > 0, "responseSize must be greater than 0" );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout, RequestState requestState ) {
        PnioMetrics.REQUESTS.increment();

        oapExchange.exchange.addExchangeCompleteListener( ( _, nl ) -> {
            PnioMetrics.activeRequests.decrementAndGet();
            if( nl != null ) {
                nl.proceed();
            }
        } );

        if( requestSize > 0 ) {
            oapExchange.exchange.getRequestReceiver().setMaxBufferSize( requestSize );
        }

        oapExchange.exchange.getRequestReceiver().receiveFullBytes( ( _, message ) -> {

            PnioIoQueue pnioIoQueue = queue.get();
            if( pnioIoQueue == null ) {
                pnioIoQueue = new PnioIoQueue( ioQueueSize, pnioController.getPnioWorkQueues() );
                queue.set( pnioIoQueue );
            }

            PnioExchange<RequestState> pnioExchange = new PnioExchange<>( message, responseSize, pnioController, task, oapExchange, timeout, pnioListener, requestState );
            pnioExchange.onDone( () -> exchanges.remove( pnioExchange.id ) );
            exchanges.put( pnioExchange.id, pnioExchange );

            try {
                pnioIoQueue.pushTask( new PnioWorkerTask<>( pnioExchange, task ), pnioWorkerTask -> {
                    exchanges.remove( pnioExchange.id );
                    pnioExchange.completeWithRejected();
                    pnioExchange.response();
                }, important );
            } catch( Exception e ) {
                exchanges.remove( pnioExchange.id );
                pnioExchange.completeWithFail( e );
                pnioExchange.response();
            }
        }, ( _, e ) -> {
            PnioExchange<RequestState> pnioExchange = new PnioExchange<>( null, responseSize, pnioController, task, oapExchange, timeout, pnioListener, requestState );
            exchanges.put( pnioExchange.id, pnioExchange );
            try {

                if( e instanceof Receiver.RequestToLargeException ) {
                    pnioExchange.completeWithBufferOverflow( true );
                } else {
                    pnioExchange.completeWithFail( e );
                }

                pnioExchange.response();
            } finally {
                exchanges.remove( pnioExchange.id );
            }
        } );

        oapExchange.exchange.dispatch();
    }

    @Override
    public PnioHttpHandler<?> getPnioHttpHandler() {
        return this;
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
        int ioQueueSize;
        boolean important;
    }
}
