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
    public final PnioController controller;
    public final WorkflowState workflowState;
    public final PnioListener<WorkflowState> pnioListener;
    public final long id = idGenerator.incrementAndGet();
    public final long startTimeNano;
    public final boolean importance;
    public Throwable throwable;
    public ProcessState processState = ProcessState.RUNNING;
    public RequestWorkflow.Node<WorkflowState> currentTaskNode;
    private Runnable onDoneRunnable;

    public PnioExchange( byte[] requestBuffer, int responseSize, PnioController controller,
                         RequestWorkflow<WorkflowState> workflow, WorkflowState inputState,
                         HttpServerExchange oapExchange, long timeout,
                         PnioListener<WorkflowState> pnioListener,
                         boolean importance ) {
        this.startTimeNano = System.nanoTime();
        this.requestBuffer = requestBuffer;
        this.responseBuffer = new PnioResponseBuffer( responseSize );

        this.controller = controller;

        this.workflowState = inputState;
        this.currentTaskNode = workflow.root;

        this.oapExchange = oapExchange;
        this.timeoutNano = timeout * 1_000_000;

        Preconditions.checkArgument( timeoutNano > 0, "timeoutNano must be greater than 0" );

        this.pnioListener = pnioListener;

        this.importance = importance;

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
        long durationInNano = now - getRequestStartTime();

        return timeoutNano - durationInNano;
    }

    public long getRequestStartTime() {
        return oapExchange.exchange.getRequestStartTime();
    }

    public void runComputeTask( PnioRequestHandler<WorkflowState> compute ) {
        try {
            compute.handle( this, workflowState );
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( false );
        } catch( Throwable e ) {
            completeWithFail( e );
        }
    }

    public CompletableFuture<Void> runBlockingTask( PnioRequestHandler<WorkflowState> blocking ) {
        Preconditions.checkNotNull( controller.blockingPool );

        return controller.runAsync( () -> {
            try {
                blocking.handle( this, workflowState );
            } catch( Exception e ) {
                throw new CompletionException( e );
            }
        } );
    }

    public void runAsyncTask( PnioRequestHandler<WorkflowState> async, Runnable success, Consumer<Throwable> exception ) {
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

            PnioRequestHandler<WorkflowState> task = currentTaskNode.handler;

            if( log.isTraceEnabled() ) {
                log.trace( "[PNIO] Processing task: " + task.description() + ", type: " + task.type );
            }

            if( getTimeLeftNano() <= 0 ) {
                completeWithTimeout();
                break;
            }

            switch( task.type ) {
                case COMPUTE -> {
                    runComputeTask( task );
                    currentTaskNode = currentTaskNode.next;
                }

                case BLOCKING -> {
                    CompletableFuture<Void> completableFuture = runBlockingTask( task );
                    asyncProcess( completableFuture, currentTaskNode );
                    return;
                }

                case ASYNC -> {
                    CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                    runAsyncTask( task, () -> completableFuture.complete( null ), new Consumer<Throwable>() {
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
                controller.register( this, new PnioTask( this ), importance );
            } );
    }

    public void response() {
        try {
            switch( processState ) {
                case DONE -> {
                    PnioMetrics.COMPLETED.increment();
                    pnioListener.onDone( this );
                }
                case CONNECTION_CLOSED -> oapExchange.closeConnection();
                case REJECTED -> {
                    PnioMetrics.REJECTED.increment();
                    pnioListener.onRejected( this );
                }
                case REQUEST_BUFFER_OVERFLOW -> {
                    PnioMetrics.REQUEST_BUFFER_OVERFLOW.increment();
                    pnioListener.onRequestBufferOverflow( this );
                }
                case RESPONSE_BUFFER_OVERFLOW -> {
                    PnioMetrics.RESPONSE_BUFFER_OVERFLOW.increment();
                    pnioListener.onResponseBufferOverflow( this );
                }
                case TIMEOUT -> {
                    PnioMetrics.TIMEOUT.increment();
                    pnioListener.onTimeout( this );
                }
                case EXCEPTION -> {
                    PnioMetrics.EXCEPTION.increment();
                    pnioListener.onException( this );
                }
                case null, default -> {
                    PnioMetrics.UNKNOWN.increment();
                    pnioListener.onUnknown( this );
                }
            }
        } finally {
            if( onDoneRunnable != null ) {
                onDoneRunnable.run();
            }
        }
    }

    public void send() {
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

    public void onDone( Runnable onDoneRunnable ) {
        Preconditions.checkArgument( this.onDoneRunnable == null );

        this.onDoneRunnable = onDoneRunnable;
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
