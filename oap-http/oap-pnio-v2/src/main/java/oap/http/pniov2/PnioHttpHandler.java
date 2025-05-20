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
public class PnioHttpHandler<WorkflowState> {
    public final int requestSize;
    public final int responseSize;
    public final PnioListener<WorkflowState> pnioListener;
    public final ConcurrentHashMap<Long, PnioExchange<WorkflowState>> exchanges = new ConcurrentHashMap<>();
    public final boolean importance;
    private final PnioController pnioController;
    public RequestWorkflow<WorkflowState> workflow;

    public PnioHttpHandler( PnioHttpSettings settings,
                            RequestWorkflow<WorkflowState> workflow,
                            PnioListener<WorkflowState> pnioListener,
                            PnioController pnioController ) {
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;
        this.importance = settings.importance;

        this.workflow = workflow;
        this.pnioListener = pnioListener;
        this.pnioController = pnioController;

        if( pnioController.blockingPoolSize <= 0 ) {
            workflow.forEach( h -> {
                Preconditions.checkArgument( h.type != PnioRequestHandler.Type.BLOCKING, "blockingPoolSize must be greater than 0" );
            } );
        }

        Preconditions.checkArgument( settings.responseSize > 0, "responseSize must be greater than 0" );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout, WorkflowState workflowState ) {
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
            PnioExchange<WorkflowState> pnioExchange = new PnioExchange<>( message, responseSize, pnioController, workflow, workflowState, oapExchange, timeout, pnioListener, importance );
            exchanges.put( pnioExchange.id, pnioExchange );

            if( !pnioController.register( pnioExchange, new PnioTask<>( pnioExchange ), importance ) ) {
                exchanges.remove( pnioExchange.id );
            } else {
                pnioExchange.onDone( () -> exchanges.remove( pnioExchange.id ) );
            }
        }, ( _, e ) -> {
            PnioExchange<WorkflowState> pnioExchange = new PnioExchange<>( null, responseSize, pnioController, workflow, workflowState, oapExchange, timeout, pnioListener, importance );
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

    public void updateWorkflow( RequestWorkflow<WorkflowState> newWorkflow ) {
        this.workflow = newWorkflow;
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
        boolean importance;
    }
}
