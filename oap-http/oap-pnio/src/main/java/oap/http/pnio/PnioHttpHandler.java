/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import io.undertow.io.Receiver;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;

import java.io.Closeable;
import java.lang.reflect.Field;

@Slf4j
public class PnioHttpHandler<WorkflowState> implements Closeable, AutoCloseable {
    private static Field workerThreadsField;

    static {
        init();
    }

    public final int requestSize;
    public final int responseSize;
    private final NioHttpServer server;
    private final ErrorResponse<WorkflowState> errorResponse;
    private RequestWorkflow<WorkflowState> workflow;

    @Deprecated
    // use builder for settings
    public PnioHttpHandler( NioHttpServer server,
                            int requestSize,
                            int responseSize,
                            RequestWorkflow<WorkflowState> workflow,
                            ErrorResponse<WorkflowState> errorResponse ) {
        this( server,
            PnioHttpSettings.builder()
                .requestSize( requestSize )
                .responseSize( responseSize )
                .build(),
            workflow,
            errorResponse );
    }

    public PnioHttpHandler( NioHttpServer server,
                            PnioHttpSettings settings,
                            RequestWorkflow<WorkflowState> workflow,
                            ErrorResponse<WorkflowState> errorResponse ) {
        this.server = server;
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;

        this.workflow = workflow;
        this.errorResponse = errorResponse;
    }

    @SneakyThrows
    private static void init() {
        workerThreadsField = Class.forName( "org.xnio.nio.NioXnioWorker" ).getDeclaredField( "workerThreads" );
        workerThreadsField.setAccessible( true );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout, WorkflowState workflowState ) {
        oapExchange.exchange.getRequestReceiver().setMaxBufferSize( requestSize );

        oapExchange.exchange.getRequestReceiver().receiveFullBytes( ( exchange, message ) -> {
            PnioExchange<WorkflowState> requestState = new PnioExchange<>( message, responseSize, workflow, workflowState, oapExchange, timeout, errorResponse );

            new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
                requestState.process( requestState );
            } ).register();
        }, ( exchange, e ) -> {
            PnioExchange<WorkflowState> requestState = new PnioExchange<>( null, responseSize, workflow, workflowState, oapExchange, timeout, errorResponse );

            if( e instanceof Receiver.RequestToLargeException ) {
                requestState.completeWithBufferOverflow( true );
            } else {
                requestState.completeWithFail( e );
            }

            new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
                requestState.process( requestState );
            } ).register();
        } );

        oapExchange.exchange.dispatch();
    }

    public void updateWorkflow( RequestWorkflow<WorkflowState> newWorkflow ) {
        this.workflow = newWorkflow;
    }

    @Override
    public void close() {
    }

    public interface ErrorResponse<WorkflowState> {
        void handle( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState );
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
    }
}
