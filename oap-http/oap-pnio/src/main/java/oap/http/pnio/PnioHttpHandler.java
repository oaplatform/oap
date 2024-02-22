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
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.highload.Affinity;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.util.Dates;
import org.slf4j.event.Level;
import org.xnio.XnioExecutor;
import org.xnio.XnioWorker;

import java.io.Closeable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
    public final double queueTimeoutPercent;
    public final Affinity cpuAffinity;
    public final Affinity ioAffinity;
    private final NioHttpServer server;
    private final ErrorResponse<WorkflowState> errorResponse;
    private final ThreadPoolExecutor pool;
    private final SynchronousQueue<PnioExchange<WorkflowState>> queue;
    private final List<RequestTaskComputeRunner<WorkflowState>> tasks = new ArrayList<>();
    private RequestWorkflow<WorkflowState> workflow;

    @Deprecated
    // use builder for settings
    public PnioHttpHandler( NioHttpServer server,
                            int requestSize,
                            int responseSize,
                            double queueTimeoutPercent,
                            int cpuThreads,
                            boolean cpuQueueFair,
                            Affinity cpuAffinity,
                            Affinity ioAffinity,
                            RequestWorkflow<WorkflowState> workflow,
                            ErrorResponse<WorkflowState> errorResponse ) {
        this( server,
            PnioHttpSettings.builder()
                .requestSize( requestSize )
                .responseSize( responseSize )
                .queueTimeoutPercent( queueTimeoutPercent )
                .cpuThreads( cpuThreads )
                .cpuQueueFair( cpuQueueFair )
                .cpuAffinity( cpuAffinity )
                .ioAffinity( ioAffinity )
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
        this.queueTimeoutPercent = settings.queueTimeoutPercent;

        if( settings.cpuThreads > 0 ) {
            this.threads = settings.cpuThreads;
        } else if( settings.cpuAffinity.isEnabled() ) {
            this.threads = settings.cpuAffinity.size();
        } else {
            this.threads = Runtime.getRuntime().availableProcessors();
        }
        this.cpuAffinity = settings.cpuAffinity;
        this.ioAffinity = settings.ioAffinity;
        this.workflow = workflow;
        this.errorResponse = errorResponse;

        Preconditions.checkArgument( this.threads <= Runtime.getRuntime().availableProcessors() );

        this.queue = new SynchronousQueue<>( settings.cpuQueueFair );

        this.pool = new ThreadPoolExecutor( this.threads, this.threads, 1, TimeUnit.MINUTES, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "cpu-http-%d" ).build(),
            new oap.concurrent.ThreadPoolExecutor.BlockingPolicy() );

        for( var i = 0; i < settings.cpuThreads; i++ ) {
            RequestTaskComputeRunner<WorkflowState> requestTaskComputeRunner = new RequestTaskComputeRunner<>( queue, cpuAffinity );
            pool.submit( requestTaskComputeRunner );
            tasks.add( requestTaskComputeRunner );
        }

        setupIoWorkers();
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

    @SneakyThrows
    private void setupIoWorkers() {
        XnioWorker xnioWorker = server.undertow.getWorker();

        Field workerThreadsField = xnioWorker.getClass().getDeclaredField( "workerThreads" );
        workerThreadsField.setAccessible( true );
        Object workerThreads = workerThreadsField.get( xnioWorker );
        int length = Array.getLength( workerThreads );
        for( int i = 0; i < length; i++ ) {
            XnioExecutor xnioExecutor = ( XnioExecutor ) Array.get( workerThreads, i );
            xnioExecutor.execute( ioAffinity::set );
        }
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
        }

        if( pnioExchange.processState == CONNECTION_CLOSED ) {
            pnioExchange.exchange.closeConnection();
            return;
        }

        pnioExchange.send();
    }

    public void updateWorkflow( RequestWorkflow<WorkflowState> newWorkflow ) {
        this.workflow = newWorkflow;
    }

    @Override
    public void close() {
        pool.shutdownNow();
        try {

            for( var task : tasks ) {
                task.interrupt();
            }

            if( !pool.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                log.trace( "timeout awaitTermination" );
            }
        } catch( InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    public interface ErrorResponse<WorkflowState> {
        void handle( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState );
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;
        double queueTimeoutPercent;
        int cpuThreads;
        boolean cpuQueueFair;
        Affinity cpuAffinity;
        Affinity ioAffinity;
    }
}
