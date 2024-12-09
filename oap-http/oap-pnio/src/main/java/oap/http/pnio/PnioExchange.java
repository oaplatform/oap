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
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;
import oap.util.Dates;
import org.slf4j.event.Level;

import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@SuppressWarnings( { "all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue" } )
public class PnioExchange<WorkflowState> {
    public final HttpServerExchange oapExchange;

    public final byte[] requestBuffer;
    public final PnioBuffer responseBuffer;
    public final long timeout;
    public final HttpResponse httpResponse = new HttpResponse();
    private final WorkflowState workflowState;
    private final PnioHttpHandler.ErrorResponse<WorkflowState> errorResponse;
    public Throwable throwable;
    public volatile ProcessState processState = ProcessState.RUNNING;
    RequestWorkflow.Node<WorkflowState> currentTaskNode;

    public PnioExchange( byte[] requestBuffer, int responseSize, RequestWorkflow<WorkflowState> workflow, WorkflowState inputState,
                         HttpServerExchange oapExchange, long timeout, PnioHttpHandler.ErrorResponse<WorkflowState> errorResponse ) {
        responseBuffer = new PnioBuffer( responseSize );

        this.workflowState = inputState;
        this.currentTaskNode = workflow.root;

        this.oapExchange = oapExchange;
        this.timeout = timeout;

        this.requestBuffer = requestBuffer;

        this.errorResponse = errorResponse;
    }

    public boolean gzipSupported() {
        return oapExchange.gzipSupported();
    }

    public String getCurrentTaskName() {
        if( currentTaskNode == null ) return "NONE";

        return currentTaskNode.handler.getClass().getSimpleName();
    }

    public String getRequestAsString() {
        return new String( requestBuffer, StandardCharsets.UTF_8 );
    }

    public void completeWithBufferOverflow( boolean request ) {
        processState = request ? ProcessState.REQUEST_BUFFER_OVERFLOW : ProcessState.RESPONSE_BUFFER_OVERFLOW;
    }

    public void completeWithTimeout() {
        processState = ProcessState.TIMEOUT;
    }

    public void completeWithConnectionClosed( Throwable throwable ) {
        this.throwable = throwable;
        this.processState = ProcessState.CONNECTION_CLOSED;
    }

    public void completeWithInterrupted() {
        processState = ProcessState.INTERRUPTED;
    }

    public void completeWithFail( Throwable throwable ) {
        this.throwable = throwable;
        this.processState = ProcessState.EXCEPTION;
    }

    public void completeWithRejected() {
        processState = ProcessState.REJECTED;
    }

    public void complete() {
        processState = ProcessState.DONE;
    }

    public final boolean isDone() {
        return processState != ProcessState.RUNNING;
    }

    public boolean isRequestGzipped() {
        return oapExchange.isRequestGzipped();
    }

    public long getTimeLeftNano() {
        long now = System.nanoTime();
        long durationInMillis = now - getRequestStartTime();

        return timeout * 1_000_000 - durationInMillis;
    }

    public long getRequestStartTime() {
        return oapExchange.exchange.getRequestStartTime();
    }

    public void runComputeTask( RequestWorkflow.Node<WorkflowState> taskNode ) {
        try {
            PnioRequestHandler<WorkflowState> task = taskNode.handler;
            task.handle( this, workflowState );
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( false );
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    public void runBlockingTask( RequestWorkflow.Node<WorkflowState> taskNode, Runnable complete ) {
        oapExchange.exchange.dispatch( () -> {
            try {
                PnioRequestHandler<WorkflowState> task = taskNode.handler;
                CompletableFuture<Void> future = task.handle( this, workflowState );
                future.get( getTimeLeftNano(), TimeUnit.NANOSECONDS );
            } catch( InterruptedException e ) {
                completeWithInterrupted();
            } catch( TimeoutException e ) {
                completeWithTimeout();
            } catch( ExecutionException e ) {
                completeWithFail( e.getCause() );
            } catch( Throwable e ) {
                completeWithFail( e );
            }
            complete.run();
        } );
    }

    public void runAsyncTask( RequestWorkflow.Node<WorkflowState> taskNode, Runnable complete ) {
        try {
            PnioRequestHandler<WorkflowState> task = taskNode.handler;
            CompletableFuture<Void> future = task.handle( this, workflowState );
            future
                .orTimeout( getTimeLeftNano(), TimeUnit.NANOSECONDS )
                .whenCompleteAsync( ( _, t ) -> {
                    if( t != null ) {
                        if( t instanceof TimeoutException ) {
                            completeWithTimeout();
                        } else {
                            completeWithFail( t );
                        }
                    }
                    complete.run();
                }, oapExchange.getWorkerPool() );

        } catch( InterruptedException e ) {
            completeWithInterrupted();
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    public void process( PnioExchange<WorkflowState> pnioExchange ) {
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
                    pnioExchange.runBlockingTask( currentTaskNode, () -> asyncProcess( pnioExchange, currentTaskNode ) );
                    return;
                }

                case ASYNC -> {
                    pnioExchange.runAsyncTask( currentTaskNode, () -> asyncProcess( pnioExchange, currentTaskNode ) );
                    return;
                }
            }
        }

        response( pnioExchange, workflowState, errorResponse );
    }

    private void asyncProcess( PnioExchange<WorkflowState> pnioExchange, RequestWorkflow.Node<WorkflowState> currentTaskNode ) {
        pnioExchange.currentTaskNode = currentTaskNode.next;

        io.undertow.server.HttpServerExchange exchange = oapExchange.exchange;
        new PnioTask( exchange.getIoThread(), exchange, timeout, () -> {
            process( pnioExchange );
        } ).register();
    }

    private void response( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState, PnioHttpHandler.ErrorResponse<WorkflowState> errorResponse ) {
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

    void send() {
        oapExchange.setStatusCode( httpResponse.status );

        httpResponse.headers.forEach( oapExchange::setResponseHeader );
        httpResponse.cookies.forEach( oapExchange::setResponseCookie );

        String contentType = httpResponse.contentType;
        if( contentType != null ) oapExchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );

        if( !responseBuffer.isEmpty() ) {
            oapExchange.send( responseBuffer.buffer, 0, responseBuffer.length );
        } else {
            oapExchange.endExchange();
        }
    }

    public enum ProcessState {
        RUNNING,
        DONE,
        TIMEOUT,
        INTERRUPTED,
        EXCEPTION,
        CONNECTION_CLOSED,
        REJECTED,
        REQUEST_BUFFER_OVERFLOW,
        RESPONSE_BUFFER_OVERFLOW
    }

    public static class HttpResponse {
        public final HashMap<String, String> headers = new HashMap<>();
        public final ArrayList<Cookie> cookies = new ArrayList<>();
        public int status = StatusCodes.NO_CONTENT;
        public String contentType;
    }
}
