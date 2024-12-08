/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

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
    public volatile CompletableFuture<Void> future;
    public Throwable throwable;
    public volatile ProcessState processState = ProcessState.RUNNING;
    RequestWorkflow.Node<WorkflowState> currentTaskNode;

    public PnioExchange( byte[] requestBuffer, int responseSize, RequestWorkflow<WorkflowState> workflow, WorkflowState inputState,
                         HttpServerExchange oapExchange, long timeout ) {
        responseBuffer = new PnioBuffer( responseSize );

        this.workflowState = inputState;
        this.currentTaskNode = workflow.root;

        this.oapExchange = oapExchange;
        this.timeout = timeout;

        this.requestBuffer = requestBuffer;
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
        completeFuture();
    }

    public void completeWithTimeout() {
        processState = ProcessState.TIMEOUT;
        completeFuture();
    }

    public void completeWithConnectionClosed( Throwable throwable ) {
        this.throwable = throwable;
        this.processState = ProcessState.CONNECTION_CLOSED;
        completeFuture();
    }

    void completeFuture() {
        if( future != null ) future.complete( null );
    }

    public void completeWithInterrupted() {
        processState = ProcessState.INTERRUPTED;
        completeFuture();
    }

    public void completeWithFail( Throwable throwable ) {
        this.throwable = throwable;
        this.processState = ProcessState.EXCEPTION;
        completeFuture();
    }

    public void completeWithRejected() {
        processState = ProcessState.REJECTED;
        completeFuture();
    }

    public void complete() {
        processState = ProcessState.DONE;
    }

    public final boolean isDone() {
        return currentTaskNode == null || processState != ProcessState.RUNNING;
    }

    public boolean isRequestGzipped() {
        return oapExchange.isRequestGzipped();
    }

    boolean waitForCompletion() {
//        try {
        if( isDone() ) return false;
        long tNano = getTimeLeftNano();
        if( tNano < 1 ) {
            completeWithTimeout();
            return false;
        } else {
//                future.get( tMs, TimeUnit.MILLISECONDS );
            return !isDone();
        }

//        } catch( InterruptedException e ) {
//            Thread.currentThread().interrupt();
//            completeWithInterrupted();
//            return false;
//        } catch( ExecutionException e ) {
//            completeWithFail( e.getCause() );
//            return false;
//        } catch( TimeoutException e ) {
//            completeWithTimeout();
//            return false;
//        }
    }

    public long getTimeLeftNano() {
        long now = System.nanoTime();
        long durationInMillis = ( now - oapExchange.exchange.getRequestStartTime() );

        return timeout * 1_000_000 - durationInMillis;
    }

    public void runComputeTasks() {
        try {
            while( currentTaskNode != null && currentTaskNode.handler.getType() == PnioRequestHandler.Type.COMPUTE ) {
                if( getTimeLeftNano() <= 0 ) {
                    completeWithTimeout();
                    return;
                }
                if( isDone() ) return;
                PnioRequestHandler<WorkflowState> task = currentTaskNode.handler;
                task.handle( this, workflowState );
                if( isDone() ) return;
                currentTaskNode = currentTaskNode.next;
            }
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( false );
        } catch( Exception e ) {
            completeWithFail( e );
        }
    }

    public void runBlockingTask( Runnable complete ) {
        try {
            if( getTimeLeftNano() <= 0 ) {
                completeWithTimeout();
                return;
            }
            if( isDone() ) return;

            PnioRequestHandler<WorkflowState> task = currentTaskNode.handler;
            CompletableFuture<Void> future = task.handle( this, workflowState );
            future.get( getTimeLeftNano(), TimeUnit.NANOSECONDS );
            currentTaskNode = currentTaskNode.next;
        } catch( InterruptedException e ) {
            completeWithInterrupted();
        } catch( TimeoutException e ) {
            completeWithTimeout();
        } catch( ExecutionException e ) {
            completeWithFail( e.getCause() );
        } catch( Exception e ) {
            completeWithFail( e );
        }
        complete.run();
    }

    public void runAsyncTask( Runnable complete ) {
        try {
            long timeLeftNano = getTimeLeftNano();

            if( timeLeftNano <= 0 ) {
                completeWithTimeout();
                return;
            }
            if( isDone() ) return;

            PnioRequestHandler<WorkflowState> task = currentTaskNode.handler;
            CompletableFuture<Void> future = task.handle( this, workflowState );
            future
                .orTimeout( timeLeftNano, TimeUnit.NANOSECONDS )
                .whenCompleteAsync( ( _, t ) -> {
                    if( t != null ) {
                        if( t instanceof TimeoutException ) {
                            completeWithTimeout();
                        } else {
                            completeWithFail( t );
                        }
                    }
                    currentTaskNode = currentTaskNode.next;
                    complete.run();
                }, oapExchange.getWorkerPool() );

        } catch( InterruptedException e ) {
            completeWithInterrupted();
        } catch( Exception e ) {
            completeWithFail( e );
        }
    }

    void send() {
        oapExchange.setStatusCode( httpResponse.status );

        httpResponse.headers.forEach( oapExchange::setResponseHeader );
        httpResponse.cookies.forEach( oapExchange::setResponseCookie );

        String contentType = httpResponse.contentType;
        if( contentType != null ) oapExchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );

        if( !responseBuffer.isEmpty() )
            oapExchange.send( responseBuffer.buffer, 0, responseBuffer.length );
        else
            oapExchange.endExchange();
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
