/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import com.google.common.io.ByteStreams;
import io.undertow.util.StatusCodes;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;
import oap.io.FixedLengthArrayOutputStream;
import oap.json.ext.Ext;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestTaskState<WorkflowState> {
    public HttpServerExchange exchange;

    public final byte[] requestBuffer;
    public final byte[] responseBuffer;

    public CompletableFuture<Void> future;
    public Throwable throwable;
    public volatile ProcessState processState;

    public long startTime;
    public long timeout;

    public int requestLength;
    public int responseLength;

    private final RequestWorkflow<WorkflowState> workflow;
    public RequestWorkflow.Node<WorkflowState> currentTaskNode;

    final HttpResponse httpResponse = new HttpResponse( this );
    private WorkflowState workflowState;

    public Ext ext;

    public RequestTaskState( int requestSize, int responseSize, RequestWorkflow<WorkflowState> workflow ) {
        requestBuffer = new byte[requestSize];
        responseBuffer = new byte[responseSize];

        this.workflow = workflow;
    }

    public void reset( HttpServerExchange exchange, long startTime, long timeout, WorkflowState inputState ) {
        processState = ProcessState.RUNNING;

        requestLength = 0;
        responseLength = 0;

        this.exchange = exchange;
        this.currentTaskNode = workflow.root;
        this.startTime = startTime;
        this.timeout = timeout;

        this.workflowState = inputState;
    }

    public boolean gzipSupported() {
        return exchange.gzipSupported();
    }

    public String getCurrentTaskName() {
        if( currentTaskNode == null ) return "NONE";

        return currentTaskNode.task.getClass().getSimpleName();
    }

    public void readFully( InputStream body ) {
        try {
            FixedLengthArrayOutputStream to = new FixedLengthArrayOutputStream( requestBuffer );
            ByteStreams.copy( body, to );
            requestLength = to.size();
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( true );
        } catch( SocketException e ) {
            completeWithConnectionClosed( e );
        } catch( IOException e ) {
            completeWithFail( e );
        }
    }

    public String getRequestAsString() {
        return new String( requestBuffer, 0, requestLength );
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

    public boolean register( SynchronousQueue<RequestTaskState<WorkflowState>> queue, double timeoutPercent ) {
        try {
            long timeoutCpuQueue = getTimeLeft( timeoutPercent );
            if( timeoutCpuQueue < 1 ) {
                completeWithRejected();
                return false;
            } else {
                future = new CompletableFuture<>();

                if( !queue.offer( this, timeoutCpuQueue, TimeUnit.MILLISECONDS ) ) {
                    completeWithRejected();
                    return false;
                } else {
                    return true;
                }
            }
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            completeWithInterrupted();
            return false;
        }
    }

    public boolean waitForCompletion() {
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
        long duration = ( now - startTime ) / 1000000;

        return timeout - duration;
    }

    public void runTasks( boolean isCpu ) {
        try {
            while( currentTaskNode != null && currentTaskNode.task.isCpu() == isCpu ) {
                if( getTimeLeft() <= 0 ) {
                    completeWithTimeout();
                    return;
                }
                if( isDone() ) return;
                AbstractRequestTask<WorkflowState> task = currentTaskNode.task;
                task.accept( this, workflowState );
                currentTaskNode = currentTaskNode.next;
            }
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    public static class HttpResponse {
        private final RequestTaskState<?> requestTaskState;
        public int status = StatusCodes.NO_CONTENT;
        public String contentType;
        private String body;

        private HttpResponse( RequestTaskState<?> requestTaskState ) {
            this.requestTaskState = requestTaskState;
        }

        public void send( HttpServerExchange hsExchange ) {
            hsExchange.setStatusCode( status );
            if( contentType != null ) hsExchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );
            if( body != null )
                hsExchange.send( body );
            else if( requestTaskState.responseLength > 0 )
                hsExchange.send( requestTaskState.responseBuffer, 0, requestTaskState.responseLength );
            else
                hsExchange.endExchange();
        }

        public void setBody( String body ) {
            this.body = body;
        }

        public String getBody() {
            return this.body != null ? this.body : new String( requestTaskState.responseBuffer, 0, requestTaskState.responseLength );
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
}
