package oap.http.pniov3;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class PnioController implements AutoCloseable {
    private static final VarHandle scheduler;
    private static final MethodHandle carrierThread;
    private static final AtomicInteger threadCounter = new AtomicInteger();

    static {
        try {
            MethodHandles.Lookup caller = MethodHandles.lookup();

            Class<?> aClass = Class.forName( "java.lang.ThreadBuilders$VirtualThreadBuilder" );
            Field field = aClass.getDeclaredField( "scheduler" );
            field.setAccessible( true );
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn( aClass, caller );
            scheduler = lookup.findVarHandle( aClass, "scheduler", Executor.class );

            aClass = Class.forName( "jdk.internal.misc.CarrierThread" );
            Constructor<?> constructor = aClass.getDeclaredConstructor( ForkJoinPool.class );
            constructor.setAccessible( true );
            lookup = MethodHandles.privateLookupIn( aClass, caller );
            carrierThread = lookup.findConstructor( aClass, MethodType.methodType( void.class, ForkJoinPool.class ) );
        } catch( ClassNotFoundException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    public final int maxThreads;
    final ForkJoinPool forkJoinPool;
    public volatile boolean done;

    public PnioController( int parallelism, double maxThreadsMultiplicator ) {
        this( getForkJoinPoolParallelism( parallelism ), ( int ) ( getForkJoinPoolParallelism( parallelism ) * maxThreadsMultiplicator ) );
    }

    public PnioController( int parallelism, int maxThreads ) {
        int forkJoinPoolParallelism = getForkJoinPoolParallelism( parallelism );

        // ForkJoinPool#MAX_CAP = 0x7fff
        forkJoinPool = new ForkJoinPool( forkJoinPoolParallelism, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            @SneakyThrows
            @Override
            public ForkJoinWorkerThread newThread( ForkJoinPool pool ) {
                return ( ForkJoinWorkerThread ) carrierThread.invokeWithArguments( pool );
            }
        }, ( t, e ) -> log.error( e.getMessage(), e ),
            true, 0, 0x7fff, forkJoinPoolParallelism, null, 60_000L, TimeUnit.MILLISECONDS );
        this.maxThreads = maxThreads;

        Metrics.gauge( "pnio_controller", Tags.of( "type", "QueuedTask" ), this, _ -> forkJoinPool.getQueuedTaskCount() );
        Metrics.gauge( "pnio_controller", Tags.of( "type", "ActiveThread" ), this, _ -> forkJoinPool.getActiveThreadCount() );
        Metrics.gauge( "pnio_controller", Tags.of( "type", "RunningThread" ), this, _ -> forkJoinPool.getRunningThreadCount() );
        Metrics.gauge( "pnio_controller", Tags.of( "type", "QueuedSubmission" ), this, _ -> forkJoinPool.getQueuedSubmissionCount() );
        Metrics.gauge( "pnio_controller", Tags.of( "type", "Steal" ), this, _ -> forkJoinPool.getStealCount() );
        Metrics.gauge( "pnio_controller", Tags.of( "type", "RequestQueue" ), this, _ -> threadCounter.get() );
    }

    private static int getForkJoinPoolParallelism( int parallelism ) {
        int forkJoinPoolParallelism;
        if( parallelism > 0 ) {
            forkJoinPoolParallelism = parallelism;
        } else {
            forkJoinPoolParallelism = Runtime.getRuntime().availableProcessors() - parallelism;
        }
        return forkJoinPoolParallelism;
    }

    @Override
    public void close() {
        done = true;

        Closeables.close( forkJoinPool );
    }

    public void pushTask( PnioWorkerTask<?, ?> task, Consumer<PnioWorkerTask<?, ?>> rejected, boolean important ) {
        if( !important ) {
            if( maxThreads < threadCounter.get() ) {
                rejected.accept( task );
                return;
            }
        }

        Thread.Builder.OfVirtual ofVirtual = Thread.ofVirtual();
        scheduler.set( ofVirtual, forkJoinPool );
        ofVirtual.start( () -> {
            threadCounter.incrementAndGet();
            try {
                task.run();
            } finally {
                threadCounter.decrementAndGet();
            }
        } );
    }
}
