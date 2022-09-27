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
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;
import oap.util.Dates;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static oap.http.pnio.PnioExchange.ProcessState.CONNECTION_CLOSED;
import static oap.http.pnio.PnioRequestHandler.Type.COMPUTE;
import static oap.http.pnio.PnioRequestHandler.Type.IO;

@Slf4j
public class PnioHttpHandler<WorkflowState> implements Closeable, AutoCloseable {
    public final int requestSize;
    public final int responseSize;
    public final int threads;
    private final RequestWorkflow<WorkflowState> workflow;
    private final ErrorResponse<WorkflowState> errorResponse;
    private final ThreadPoolExecutor pool;
    private final SynchronousQueue<PnioExchange<WorkflowState>> queue;

    public final double queueTimeoutPercent;
    public final int cpuAffinityFirstCpu;

    private final List<RequestTaskComputeRunner<WorkflowState>> tasks = new ArrayList<>();

    public PnioHttpHandler( int requestSize, int responseSize,
                            double queueTimeoutPercent,
                            int cpuThreads, boolean cpuQueueFair, int cpuAffinityFirstCpu,
                            RequestWorkflow<WorkflowState> workflow,
                            ErrorResponse<WorkflowState> errorResponse ) {
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.queueTimeoutPercent = queueTimeoutPercent;

        this.threads = cpuThreads > 0 ? cpuThreads : Runtime.getRuntime().availableProcessors();
        this.cpuAffinityFirstCpu = cpuAffinityFirstCpu;
        this.workflow = workflow;
        this.errorResponse = errorResponse;

        Preconditions.checkArgument( this.threads <= Runtime.getRuntime().availableProcessors() );

        this.queue = new SynchronousQueue<>( cpuQueueFair );

        this.pool = new ThreadPoolExecutor( this.threads, this.threads, 1, TimeUnit.MINUTES, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "cpu-http-%d" ).build(),
            new oap.concurrent.ThreadPoolExecutor.BlockingPolicy() );

        for( var i = 0; i < cpuThreads; i++ ) {
            RequestTaskComputeRunner<WorkflowState> requestTaskComputeRunner = new RequestTaskComputeRunner<>( queue,
                cpuAffinityFirstCpu >= 0 ? cpuAffinityFirstCpu + i : -1 );
            pool.submit( requestTaskComputeRunner );
            tasks.add( requestTaskComputeRunner );
        }
    }

    public int getPoolSize() {
        return pool.getPoolSize();
    }

    public int getPoolActiveCount() {
        return pool.getActiveCount();
    }

    public long getPoolCompletedTaskCount() {
        return pool.getCompletedTaskCount();
    }

    public void handleRequest( HttpServerExchange exchange, long startTimeNano, long timeout, WorkflowState workflowState ) {
        var requestState = new PnioExchange<>( requestSize, responseSize, workflow, workflowState, exchange, startTimeNano, timeout );

        while( !requestState.isDone() ) {
            PnioRequestHandler<WorkflowState> task = requestState.currentTaskNode.handler;
            if( task.getType() == COMPUTE ) {
                requestState.register( queue, queueTimeoutPercent );
            } else {
                requestState.runTasks( IO );
            }

            if( !requestState.waitForCompletion() ) {
                break;
            }
        }

        response( requestState, workflowState );
    }

    private void response( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState ) {
        if( pnioExchange.currentTaskNode != null ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            try {
                httpResponse.cookies.clear();
                httpResponse.headers.clear();

                errorResponse.handle( pnioExchange, workflowState );
            } catch( Throwable e ) {
                LogConsolidated.log( log, Level.ERROR, Dates.s( 5 ), e.getMessage(), e );

                httpResponse.cookies.clear();
                httpResponse.headers.clear();
                httpResponse.status = Http.StatusCode.BAD_GATEWAY;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                pnioExchange.responseBuffer.set( Throwables.getStackTraceAsString( e ) );
            }
        }

        if( pnioExchange.processState == CONNECTION_CLOSED ) {
            pnioExchange.exchange.closeConnection();
            return;
        }

        pnioExchange.send();
    }

    @Override
    /**
     * Immediately shutdown
     */
    public void close() {
        pool.shutdownNow();
        if ( !tasks.isEmpty() ) {
            log.info( "Cancelling " + tasks.size() + " tasks..." );
        }
        for( var task : tasks ) {
            task.interrupt();
        }
        tasks.clear();
        waitForFinish();
    }

    private void waitForFinish() {
        try {
            int count = 12; // 1 minute
            while( count > 0 && !pool.awaitTermination( 5, TimeUnit.SECONDS ) ) {
                log.debug( "waiting for finishing..." );
                count--;
            }
            if ( count == 0 ) log.error( "60 sec timeout awaitTermination" );
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }

    public interface ErrorResponse<WorkflowState> {
        void handle( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState );
    }
}
