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
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PnioExchange<WorkflowState> {
    public final HttpServerExchange exchange;

    public final PnioBuffer requestBuffer;
    public final PnioBuffer responseBuffer;

    public CompletableFuture<Void> future;
    public Throwable throwable;
    public volatile ProcessState processState = ProcessState.RUNNING;

    public final long startTimeNano;
    public final long timeout;

    volatile RequestWorkflow.Node<WorkflowState> currentTaskNode;

    HttpResponse httpResponse = new HttpResponse();
    private final WorkflowState workflowState;

    public PnioExchange( int requestSize, int responseSize, RequestWorkflow<WorkflowState> workflow, WorkflowState inputState,
                         HttpServerExchange exchange, long startTimeNano, long timeout ) {
        requestBuffer = new PnioBuffer( requestSize );
        responseBuffer = new PnioBuffer( responseSize );

        this.workflowState = inputState;
        this.currentTaskNode = workflow.root;

        this.exchange = exchange;
        this.startTimeNano = startTimeNano;
        this.timeout = timeout;

        InputStream inputStream = exchange.getInputStream();
        readFully( inputStream );
    }

    public boolean gzipSupported() {
        return exchange.gzipSupported();
    }

    public String getCurrentTaskName() {
        if( currentTaskNode == null ) return "NONE";

        return currentTaskNode.handler.getClass().getSimpleName();
    }

    private void readFully( InputStream body ) {
        try {
            requestBuffer.copyFrom( body );
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( true );
        } catch( SocketException e ) {
            completeWithConnectionClosed( e );
        } catch( IOException e ) {
            completeWithFail( e );
        }
    }

    public String getRequestAsString() {
        return requestBuffer.string();
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

    void completeWithRejected() {
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
        return exchange.isRequestGzipped();
    }

    void register( SynchronousQueue<PnioExchange<WorkflowState>> queue, double timeoutPercent ) {
        try {
            long timeoutCpuQueue = getTimeLeft( timeoutPercent );
            if( timeoutCpuQueue < 1 ) {
                completeWithRejected();
            } else {
                future = new CompletableFuture<>();

                if( !queue.offer( this, timeoutCpuQueue, TimeUnit.MILLISECONDS ) ) {
                    completeWithRejected();
                }
            }
        } catch( InterruptedException e ) {
            completeWithInterrupted();
        }
    }

    boolean waitForCompletion() {
        try {
            if( isDone() ) return false;
            long tMs = getTimeLeft();
            if( tMs < 1 ) {
                completeWithTimeout();
                return false;
            } else {
                future.get( tMs, TimeUnit.MILLISECONDS );
                return !isDone();
            }

        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            completeWithInterrupted();
            return false;
        } catch( ExecutionException e ) {
            completeWithFail( e.getCause() );
            return false;
        } catch( TimeoutException e ) {
            completeWithTimeout();
            return false;
        }
    }

    public long getTimeLeft( double percent ) {
        return ( long ) ( getTimeLeft() * percent );
    }

    public long getTimeLeft() {
        long now = System.nanoTime();
        long duration = ( now - startTimeNano ) / 1000000;

        return timeout - duration;
    }

    void runTasks( PnioRequestHandler.Type type ) {
        try {
            while( currentTaskNode != null && currentTaskNode.handler.getType() == type ) {
                if( getTimeLeft() <= 0 ) {
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
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    void send() {
        exchange.setStatusCode( httpResponse.status );

        httpResponse.headers.forEach( exchange::setResponseHeader );
        httpResponse.cookies.forEach( exchange::setResponseCookie );

        String contentType = httpResponse.contentType;
        if( contentType != null ) exchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );

        if( !responseBuffer.isEmpty() )
            exchange.send( responseBuffer.array(), 0, responseBuffer.length );
        else
            exchange.endExchange();
    }

    static class HttpResponse {
        public int status = StatusCodes.NO_CONTENT;
        public String contentType;
        public final HashMap<String, String> headers = new HashMap<>();
        public final ArrayList<Cookie> cookies = new ArrayList<>();
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
}
