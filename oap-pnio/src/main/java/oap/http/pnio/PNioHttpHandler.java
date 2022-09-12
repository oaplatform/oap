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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static oap.http.pnio.RequestTaskState.ProcessState.CONNECTION_CLOSED;

@Slf4j
public class PNioHttpHandler<WorkflowState> implements Closeable {
    public final int requestSize;
    public final int responseSize;
    public final int threads;
    private final RequestWorkflow<WorkflowState> workflow;
    private final ResponseModifier<WorkflowState> responseModifier;
    private final ThreadPoolExecutor pool;
    private final SynchronousQueue<RequestTaskState<WorkflowState>> queue;

    public final double queueTimeoutPercent;
    public final int cpuAffinityFirstCpu;

    private final ArrayList<RequestTaskCpuRunner<WorkflowState>> tasks = new ArrayList<>();

    public PNioHttpHandler( int requestSize, int responseSize,
                            double queueTimeoutPercent,
                            int threads, boolean cpuQueueFair, int cpuAffinityFirstCpu,
                            RequestWorkflow<WorkflowState> workflow,
                            ResponseModifier<WorkflowState> responseModifier ) {
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.queueTimeoutPercent = queueTimeoutPercent;

        this.threads = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
        this.cpuAffinityFirstCpu = cpuAffinityFirstCpu;
        this.workflow = workflow;
        this.responseModifier = responseModifier;

        Preconditions.checkArgument( this.threads <= Runtime.getRuntime().availableProcessors() );

        this.queue = new SynchronousQueue<>( cpuQueueFair );

        this.pool = new ThreadPoolExecutor( this.threads, this.threads, 1, TimeUnit.MINUTES, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "cpu-http-%d" ).build(),
            new oap.concurrent.ThreadPoolExecutor.BlockingPolicy() );

        for( var i = 0; i < threads; i++ ) {
            RequestTaskCpuRunner<WorkflowState> requestTaskCpuRunner = new RequestTaskCpuRunner<>( queue, cpuAffinityFirstCpu >= 0 ? cpuAffinityFirstCpu + i : -1 );
            pool.submit( requestTaskCpuRunner );
            tasks.add( requestTaskCpuRunner );
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
        InputStream inputStream = exchange.getInputStream();

        var requestState = new RequestTaskState<>( requestSize, responseSize, workflow );

        requestState.reset( exchange, startTimeNano, timeout, workflowState );
        requestState.readFully( inputStream );

        while( !requestState.isDone() ) {
            AbstractRequestTask<WorkflowState> task = requestState.currentTaskNode.task;
            if( task.isCpu() ) {
                requestState.register( queue, queueTimeoutPercent );
            } else {
                requestState.runTasks( false );
            }

            if( !requestState.waitForCompletion() ) {
                break;
            }
        }

        response( requestState, workflowState );
    }

    private void response( RequestTaskState<WorkflowState> requestState, WorkflowState workflowState ) {
        responseModifier.accept( requestState, workflowState, requestState.httpResponse );

        if( requestState.processState == CONNECTION_CLOSED ) {
            requestState.exchange.closeConnection();
            return;
        }

        requestState.httpResponse.send( requestState.exchange );
    }

    @Override
    public void close() {
        pool.shutdownNow();
        try {

            for( var task : tasks ) {
                task.thread.interrupt();
            }

            if( !pool.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                log.trace( "timeout awaitTermination" );
            }
        } catch( InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    public interface ResponseModifier<WorkflowState> {
        void accept( RequestTaskState<WorkflowState> requestState, WorkflowState workflowState, RequestTaskState.HttpResponse httpResponse );
    }
}
