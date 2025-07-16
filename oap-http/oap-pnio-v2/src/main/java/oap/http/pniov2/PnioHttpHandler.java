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
import io.undertow.util.SameThreadExecutor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PnioHttpHandler<RequestState> implements PnioHttpHandlerReference {
    public final String uniqueName;

    public final int requestSize;
    public final int responseSize;
    public final int ioQueueSize;
    public final boolean important;
    public final PnioListener<RequestState> pnioListener;
    public final ConcurrentHashMap<Long, PnioExchange<RequestState>> exchanges = new ConcurrentHashMap<>();
    public final AtomicLong rr = new AtomicLong();
    private final PnioController pnioController;
    private final PnioMetrics metrics;
    public ComputeTask<RequestState> task;

    public PnioHttpHandler( String uniqueName,
                            PnioHttpSettings settings,
                            ComputeTask<RequestState> task,
                            PnioListener<RequestState> pnioListener,
                            PnioController pnioController ) {
        this.uniqueName = uniqueName;
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;
        this.ioQueueSize = settings.ioQueueSize;
        this.important = settings.important;

        this.task = task;
        this.pnioListener = pnioListener;
        this.pnioController = pnioController;

        this.metrics = new PnioMetrics( uniqueName );

        Preconditions.checkArgument( settings.responseSize > 0, "responseSize must be greater than 0" );
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
                    pnioController.pushTask( rr, new PnioWorkerTask<>( pnioExchange, task ), _ -> {
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

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
        int ioQueueSize;
        boolean important;
    }
}
