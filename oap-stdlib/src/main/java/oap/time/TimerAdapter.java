package oap.time;

import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TimerAdapter implements AutoCloseable {
    private final Timer timer;
    private long start = System.nanoTime();

    public TimerAdapter( Timer timer ) {
        this.timer = Objects.requireNonNull( timer );
    }

    @Override
    public void close() throws Exception {
        timer.record( System.nanoTime() - start, TimeUnit.NANOSECONDS );
    }

    public static <T> T recordForCallable( Timer timer, Callable<T> callable ) {
        Objects.requireNonNull( timer );
        Objects.requireNonNull( callable );
        try ( var adapter = new TimerAdapter( timer ) ) {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException( "Cannot calculate metric for " + callable.getClass().getCanonicalName(), e );
        }
    }

    public static <T> T recordForSupplier( Timer timer, Supplier<T> supplier ) {
        Objects.requireNonNull( timer );
        Objects.requireNonNull( supplier );
        try ( var adapter = new TimerAdapter( timer ) ) {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException( "Cannot calculate metric for " + supplier.getClass().getCanonicalName(), e );
        }
    }
}
