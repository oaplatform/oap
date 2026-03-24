/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pniov3;

import io.undertow.io.Receiver;
import io.undertow.util.SameThreadExecutor;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PnioHttpHandler<RequestState> implements PnioHttpHandlerReference {
    public final String uniqueName;
    public final PnioListener<RequestState> pnioListener;
    public final ConcurrentHashMap<Long, PnioExchange<RequestState>> exchanges = new ConcurrentHashMap<>();
    private final PnioController pnioController;
    private final PnioMetrics metrics;
    public int requestSize = 64 * 1024;
    public int responseSize = 32 * 1024;
    public boolean important = false;
    public ComputeTask<RequestState> task;

    public PnioHttpHandler( String uniqueName,
                            ComputeTask<RequestState> task,
                            PnioListener<RequestState> pnioListener,
                            PnioController pnioController ) {
        this.uniqueName = uniqueName;

        this.task = task;
        this.pnioListener = pnioListener;
        this.pnioController = pnioController;

        this.metrics = new PnioMetrics( uniqueName );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout, RequestState requestState ) {
        metrics.requests.increment();

        oapExchange.exchange.addExchangeCompleteListener( ( _, nl ) -> {
            metrics.activeRequests.decrementAndGet();
            if( nl != null ) {
                nl.proceed();
            }
        } );

        if( requestSize > 0 ) {
            oapExchange.exchange.getRequestReceiver().setMaxBufferSize( requestSize );
        }

        oapExchange.exchange.getRequestReceiver().receiveFullBytes( ( _, message ) -> {
            oapExchange.exchange.dispatch( SameThreadExecutor.INSTANCE, () -> {
                PnioExchange<RequestState> pnioExchange = new PnioExchange<>( uniqueName, message, responseSize, pnioController, task, oapExchange, timeout, pnioListener, requestState, metrics );
                pnioExchange.onDone( () -> exchanges.remove( pnioExchange.id ) );
                exchanges.put( pnioExchange.id, pnioExchange );

                try {
                    pnioController.pushTask( new PnioWorkerTask<>( pnioExchange, task ), _ -> {
                        exchanges.remove( pnioExchange.id );
                        pnioExchange.completeWithRejected();
                        pnioExchange.response();
                    }, important );
                } catch( Exception e ) {
                    exchanges.remove( pnioExchange.id );
                    pnioExchange.completeWithFail( e );
                    pnioExchange.response();
                }
            } );
        }, ( _, e ) -> {
            oapExchange.exchange.dispatch( SameThreadExecutor.INSTANCE, () -> {
                PnioExchange<RequestState> pnioExchange = new PnioExchange<>( uniqueName, null, responseSize, pnioController, task, oapExchange, timeout, pnioListener, requestState, metrics );
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
        } );
    }

    @Override
    public PnioHttpHandler<?> getPnioHttpHandler() {
        return this;
    }
}
