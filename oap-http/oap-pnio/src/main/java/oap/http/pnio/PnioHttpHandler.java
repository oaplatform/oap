/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.undertow.io.Receiver;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.io.Closeables;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PnioHttpHandler<WorkflowState> implements Closeable, AutoCloseable {
    private static Field workerThreadsField;

    static {
        init();
    }

    public final int requestSize;
    public final int responseSize;
    private final NioHttpServer server;
    private final PnioListener<WorkflowState> pnioListener;
    private final ExecutorService blockingPool;
    private final int maxQueueSize;
    private RequestWorkflow<WorkflowState> workflow;

    @Deprecated
    // use builder for settings
    public PnioHttpHandler( NioHttpServer server,
                            int requestSize,
                            int responseSize,
                            int maxQueueSize,
                            int blockingPoolSize,
                            RequestWorkflow<WorkflowState> workflow,
                            PnioListener<WorkflowState> pnioListener ) {
        this( server,
            PnioHttpSettings.builder()
                .requestSize( requestSize )
                .responseSize( responseSize )
                .maxQueueSize( maxQueueSize )
                .blockingPoolSize( blockingPoolSize )
                .build(),
            workflow,
            pnioListener );
    }

    public PnioHttpHandler( NioHttpServer server,
                            PnioHttpSettings settings,
                            RequestWorkflow<WorkflowState> workflow,
                            PnioListener<WorkflowState> pnioListener ) {
        this.server = server;
        this.requestSize = settings.requestSize;
        this.responseSize = settings.responseSize;
        this.maxQueueSize = settings.maxQueueSize;

        this.workflow = workflow;
        this.pnioListener = pnioListener;

        blockingPool = settings.blockingPoolSize > 0
            ? Executors.newFixedThreadPool( settings.blockingPoolSize, new ThreadFactoryBuilder().setNameFormat( "PNIO - blocking-%d" ).build() )
            : null;
    }

    @SneakyThrows
    private static void init() {
        workerThreadsField = Class.forName( "org.xnio.nio.NioXnioWorker" ).getDeclaredField( "workerThreads" );
        workerThreadsField.setAccessible( true );
    }

    public void handleRequest( HttpServerExchange oapExchange, long timeout, WorkflowState workflowState ) {
        PnioMetrics.REQUESTS.increment();

        oapExchange.exchange.addExchangeCompleteListener( ( _, nl ) -> {
            PnioMetrics.activeRequests.decrementAndGet();
            if( nl != null ) {
                nl.proceed();
            }
        } );

        oapExchange.exchange.getRequestReceiver().setMaxBufferSize( requestSize );

        oapExchange.exchange.getRequestReceiver().receiveFullBytes( ( exchange, message ) -> {
            PnioExchange<WorkflowState> pnioExchange = new PnioExchange<>( message, responseSize, maxQueueSize, blockingPool, workflow, workflowState, oapExchange, timeout, pnioListener );

            new PnioTask<>( exchange.getIoThread(), pnioExchange ).register( true );
        }, ( exchange, e ) -> {
            PnioExchange<WorkflowState> pnioExchange = new PnioExchange<>( null, responseSize, maxQueueSize, blockingPool, workflow, workflowState, oapExchange, timeout, pnioListener );

            if( e instanceof Receiver.RequestToLargeException ) {
                pnioExchange.completeWithBufferOverflow( true );
            } else {
                pnioExchange.completeWithFail( e );
            }

            pnioExchange.response();
        } );

        oapExchange.exchange.dispatch();
    }

    public void updateWorkflow( RequestWorkflow<WorkflowState> newWorkflow ) {
        this.workflow = newWorkflow;
    }

    @Override
    public void close() {
        Closeables.close( blockingPool );
    }

    @Builder
    public static class PnioHttpSettings {
        int requestSize;
        int responseSize;

        int blockingPoolSize;
        int maxQueueSize;
    }
}
