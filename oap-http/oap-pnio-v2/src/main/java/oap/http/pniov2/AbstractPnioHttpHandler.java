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
public abstract class AbstractPnioHttpHandler<E extends AbstractPnioExchange<E>> implements PnioHttpHandlerReference {
    public final int requestSize;
    public final int responseSize;
    public final PnioListener<E> pnioListener;
    public final ConcurrentHashMap<Long, E> exchanges = new ConcurrentHashMap<>();
    private final PnioController pnioController;
    public ComputeTask<E> task;

    public AbstractPnioHttpHandler( PnioHttpSettings settings,
                                    ComputeTask<E> task,
                                    PnioListener<E> pnioListener,
                                    PnioController pnioController ) {
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;

        this.task = task;
        this.pnioListener = pnioListener;
        this.pnioController = pnioController;

        Preconditions.checkArgument( settings.responseSize > 0, "responseSize must be greater than 0" );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout ) {
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
            E pnioExchange = createPnioExchange( oapExchange, timeout, message, pnioController, task, pnioListener );
            pnioExchange.onDone( () -> exchanges.remove( pnioExchange.id ) );
            exchanges.put( pnioExchange.id, pnioExchange );

            try {
                PnioComputeTask pnioComputeTask = new PnioComputeTask( task, pnioExchange );
                if( !pnioController.submit( pnioComputeTask ) ) {
                    exchanges.remove( pnioExchange.id );
                    pnioExchange.completeWithRejected();
                    pnioExchange.response();
                }
            } catch( Exception e ) {
                exchanges.remove( pnioExchange.id );
                pnioExchange.completeWithFail( e );
                pnioExchange.response();
            }
        }, ( _, e ) -> {
            E pnioExchange = createPnioExchange( oapExchange, timeout, null, pnioController, task, pnioListener );
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

    protected abstract E createPnioExchange( HttpServerExchange oapExchange, long timeout, byte[] message,
                                             PnioController pnioController, ComputeTask<E> computeTask, PnioListener<E> pnioListener );

    @Override
    public AbstractPnioHttpHandler<?> getPnioHttpHandler() {
        return this;
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
    }
}
