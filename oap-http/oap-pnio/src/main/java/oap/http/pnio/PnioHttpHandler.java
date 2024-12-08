/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import com.google.common.base.Throwables;
import io.undertow.io.Receiver;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.util.Dates;
import org.slf4j.event.Level;

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
            PnioExchange<WorkflowState> requestState = new PnioExchange<>( message, responseSize, workflow, workflowState, oapExchange, timeout );

            new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
                process( oapExchange, timeout, workflowState, requestState );
            } ).register();
        }, ( exchange, e ) -> {
            PnioExchange<WorkflowState> requestState = new PnioExchange<>( null, responseSize, workflow, workflowState, oapExchange, timeout );

            if( e instanceof Receiver.RequestToLargeException ) {
                requestState.completeWithBufferOverflow( true );
            } else {
                requestState.completeWithFail( e );
            }

            new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
                process( oapExchange, timeout, workflowState, requestState );
            } ).register();
        } );

        oapExchange.exchange.dispatch();
    }

    private void process( HttpServerExchange oapExchange, long timeout, WorkflowState workflowState, PnioExchange<WorkflowState> pnioExchange ) {
        while( !pnioExchange.isDone() ) {
            RequestWorkflow.Node<WorkflowState> currentTaskNode = pnioExchange.currentTaskNode;

            if( currentTaskNode == null ) {
                pnioExchange.complete();
                break;
            }

            PnioRequestHandler<WorkflowState> task = currentTaskNode.handler;

            if( pnioExchange.getTimeLeftNano() <= 0 ) {
                pnioExchange.completeWithTimeout();
                break;
            }

            switch( task.getType() ) {
                case COMPUTE -> {
                    pnioExchange.runComputeTask( currentTaskNode );
                    pnioExchange.currentTaskNode = currentTaskNode.next;
                }

                case BLOCK -> {
                    pnioExchange.runBlockingTask( currentTaskNode, () -> asyncProcess( oapExchange, timeout, workflowState, pnioExchange, currentTaskNode ) );
                    return;
                }

                case ASYNC -> {
                    pnioExchange.runAsyncTask( currentTaskNode, () -> asyncProcess( oapExchange, timeout, workflowState, pnioExchange, currentTaskNode ) );
                    return;
                }
            }
        }

        response( pnioExchange, workflowState );
    }

    private void asyncProcess( HttpServerExchange oapExchange, long timeout, WorkflowState workflowState, PnioExchange<WorkflowState> pnioExchange, RequestWorkflow.Node<WorkflowState> currentTaskNode ) {
        pnioExchange.currentTaskNode = currentTaskNode.next;

        io.undertow.server.HttpServerExchange exchange = oapExchange.exchange;
        new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
            process( oapExchange, timeout, workflowState, pnioExchange );
        } ).register();
    }

    private void response( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState ) {
        switch( pnioExchange.processState ) {
            case DONE -> pnioExchange.send();
            case CONNECTION_CLOSED -> pnioExchange.oapExchange.closeConnection();
            case null, default -> {
                PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
                try {
                    httpResponse.cookies.clear();
                    httpResponse.headers.clear();

                    errorResponse.handle( pnioExchange, workflowState );
                } catch( Throwable e ) {
                    if( e instanceof OutOfMemoryError ) {
                        log.error( "OOM error, need restarting!", e );
                    }
                    LogConsolidated.log( log, Level.ERROR, Dates.s( 5 ), e.getMessage(), e );

                    httpResponse.cookies.clear();
                    httpResponse.headers.clear();
                    httpResponse.status = Http.StatusCode.BAD_GATEWAY;
                    httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                    pnioExchange.responseBuffer.setAndResize( Throwables.getStackTraceAsString( e ) );
                }

                pnioExchange.send();
            }
        }
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
