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
import java.util.function.Consumer;

@Slf4j
public class PnioController implements AutoCloseable {
    private static final VarHandle scheduler;
    private static final MethodHandle carrierThread;

    static {
        try {
            Class<?> aClass = Class.forName( "java.lang.ThreadBuilders$VirtualThreadBuilder" );
            Field field = aClass.getDeclaredField( "scheduler" );
            field.setAccessible( true );
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn( aClass, MethodHandles.lookup() );
            scheduler = lookup.findVarHandle( aClass, "scheduler", Executor.class );

            aClass = Class.forName( "jdk.internal.misc.CarrierThread" );
            Constructor<?> constructor = aClass.getDeclaredConstructor( ForkJoinPool.class );
            constructor.setAccessible( true );
            lookup = MethodHandles.privateLookupIn( aClass, MethodHandles.lookup() );
            carrierThread = lookup.findConstructor( aClass, MethodType.methodType( void.class, ForkJoinPool.class ) );
        } catch( ClassNotFoundException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    final ForkJoinPool pool;
    final ForkJoinPool importantPool;
    public volatile boolean done;

    public PnioController( int parallelism, int importantParallelism ) {
        pool = new ForkJoinPool( parallelism, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            @SneakyThrows
            @Override
            public ForkJoinWorkerThread newThread( ForkJoinPool pool ) {
                return ( ForkJoinWorkerThread ) carrierThread.invoke( pool );
            }
        }, new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException( Thread t, Throwable e ) {
                log.error( e.getMessage(), e );
            }
        }, true, parallelism, parallelism, parallelism, null, 60_000L, TimeUnit.MILLISECONDS );

        importantPool = new ForkJoinPool( parallelism, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            @SneakyThrows
            @Override
            public ForkJoinWorkerThread newThread( ForkJoinPool pool ) {
                return ( ForkJoinWorkerThread ) carrierThread.invoke( pool );
            }
        }, new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException( Thread t, Throwable e ) {
                log.error( e.getMessage(), e );
            }
        }, true, importantParallelism, importantParallelism, importantParallelism, _ -> true, 60_000L, TimeUnit.MILLISECONDS );

        Metrics.gauge( "pnio_tasks", Tags.of( "important", "false" ), this, c -> c.pool.getQueuedTaskCount() );
        Metrics.gauge( "pnio_tasks", Tags.of( "important", "true" ), this, c -> c.importantPool.getQueuedTaskCount() );
    }

    @Override
    public void close() {
        done = true;

        Closeables.close( pool );
        Closeables.close( importantPool );
    }

    public void pushTask( PnioWorkerTask<?, ?> task, Consumer<PnioWorkerTask<?, ?>> rejected, boolean important ) {
        if( important ) {
            Thread.Builder.OfVirtual ofVirtual = Thread.ofVirtual();
            scheduler.set( ofVirtual, importantPool );
            ofVirtual.start( task::run );
        } else {
            Thread.Builder.OfVirtual ofVirtual = Thread.ofVirtual();
            scheduler.set( ofVirtual, pool );
            ofVirtual.start( task::run );
        }
    }
}
