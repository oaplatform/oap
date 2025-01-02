/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import com.google.common.base.Preconditions;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@SuppressWarnings( { "all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue" } )
public class PnioExchange<WorkflowState> {
    private static final AtomicLong idGenerator = new AtomicLong();

    public final HttpServerExchange oapExchange;

    public final byte[] requestBuffer;
    public final PnioResponseBuffer responseBuffer;
    public final long timeoutNano;
    public final HttpResponse httpResponse = new HttpResponse();
    public final ExecutorService blockingPool;
    public final WorkflowState workflowState;
    public final PnioListener<WorkflowState> pnioListener;
    public final long id = idGenerator.incrementAndGet();
    private final PnioWorkers<WorkflowState> workers;
    public Throwable throwable;
    public ProcessState processState = ProcessState.RUNNING;
    public RequestWorkflow.Node<WorkflowState> currentTaskNode;

    public PnioExchange( byte[] requestBuffer, int responseSize, ExecutorService blockingPool,
                         RequestWorkflow<WorkflowState> workflow, WorkflowState inputState,
                         HttpServerExchange oapExchange, long timeout,
                         PnioWorkers<WorkflowState> workers, PnioListener<WorkflowState> pnioListener ) {
        this.requestBuffer = requestBuffer;
        this.responseBuffer = new PnioResponseBuffer( responseSize );

        this.blockingPool = blockingPool;

        this.workflowState = inputState;
        this.currentTaskNode = workflow.root;

        this.oapExchange = oapExchange;
        this.timeoutNano = timeout * 1_000_000;

        this.workers = workers;

        this.pnioListener = pnioListener;

        PnioMetrics.activeRequests.incrementAndGet();
    }

    public boolean gzipSupported() {
        return oapExchange.gzipSupported();
    }

    public String getCurrentTaskName() {
        RequestWorkflow.Node<WorkflowState> node = currentTaskNode;
        if( node == null ) return "NONE";

        return node.handler.getClass().getSimpleName();
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

        return timeoutNano - durationInMillis;
    }

    public long getRequestStartTime() {
        return oapExchange.exchange.getRequestStartTime();
    }

    public void runComputeTask( ComputePnioRequestHandler<WorkflowState> compute ) {
        try {
            compute.handle( this, workflowState );
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( false );
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    public CompletableFuture<Void> runBlockingTask( BlockingPnioRequestHandler<WorkflowState> blocking ) {
        Preconditions.checkNotNull( blockingPool );

        return CompletableFuture.runAsync( () -> {
            try {
                blocking.handle( this, workflowState );
            } catch( Exception e ) {
                throw new CompletionException( e );
            }
        }, blockingPool );
    }

    public void runAsyncTask( AsyncPnioRequestHandler<WorkflowState> async, Runnable success, Consumer<Throwable> exception ) {
        try {
            async.handle( this, workflowState, success, exception );
        } catch( Throwable e ) {
            exception.accept( e );
        }
    }

    public void process() {
        while( !isDone() ) {
            if( currentTaskNode == null ) {
                complete();
                break;
            }

            AbstractPnioRequestHandler<WorkflowState> task = currentTaskNode.handler;

            if( getTimeLeftNano() <= 0 ) {
                completeWithTimeout();
                break;
            }

            switch( task ) {
                case ComputePnioRequestHandler<WorkflowState> compute -> {
                    runComputeTask( compute );
                    currentTaskNode = currentTaskNode.next;
                }

                case BlockingPnioRequestHandler<WorkflowState> blocking -> {
                    CompletableFuture<Void> completableFuture = runBlockingTask( blocking );
                    asyncProcess( completableFuture, currentTaskNode );
                    return;
                }

                case AsyncPnioRequestHandler<WorkflowState> async -> {
                    CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                    runAsyncTask( async, () -> completableFuture.complete( null ), new Consumer<Throwable>() {
                        @Override
                        public void accept( Throwable throwable ) {
                            completableFuture.completeExceptionally( throwable );
                        }
                    } );
                    asyncProcess( completableFuture, currentTaskNode );
                    return;
                }
                default -> throw new IllegalStateException( "Unexpected value: " + task.getClass() );
            }
        }

        response();
    }

    private void asyncProcess( CompletableFuture<Void> completableFuture, RequestWorkflow.Node<WorkflowState> taskNode ) {
        completableFuture
            .orTimeout( getTimeLeftNano(), TimeUnit.NANOSECONDS )
            .whenComplete( ( _, t ) -> {
                if( t != null ) {
                    if( t instanceof CancellationException || t instanceof InterruptedException ) {
                        completeWithInterrupted();
                    } else if( t instanceof TimeoutException ) {
                        completeWithTimeout();
                    } else {
                        completeWithFail( t );
                    }
                } else {
                    this.currentTaskNode = taskNode.next;
                }

                io.undertow.server.HttpServerExchange exchange = oapExchange.exchange;
                workers.register( this, new PnioTask( this ) );
            } );
    }

    public void response() {
        switch( processState ) {
            case DONE -> pnioListener.fireOnDone( this );
            case CONNECTION_CLOSED -> oapExchange.closeConnection();
            case REJECTED -> pnioListener.fireOnRejected( this );
            case REQUEST_BUFFER_OVERFLOW -> pnioListener.fireOnRequestBufferOverflow( this );
            case RESPONSE_BUFFER_OVERFLOW -> pnioListener.fireOnResponseBufferOverflow( this );
            case TIMEOUT -> pnioListener.fireOnTimeout( this );
            case EXCEPTION -> pnioListener.fireOnException( this );
            case null, default -> pnioListener.fireOnUnknown( this );
        }
    }

    void send() {
        oapExchange.setStatusCode( httpResponse.status );

        httpResponse.headers.forEach( oapExchange::setResponseHeader );
        httpResponse.cookies.forEach( oapExchange::setResponseCookie );

        String contentType = httpResponse.contentType;
        if( contentType != null ) {
            oapExchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );
        }

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
