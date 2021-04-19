/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.concurrent;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.util.function.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class Threads {
    public static void interruptAndJoin( Thread thread ) {
        if( thread != null ) {
            thread.interrupt();
            try {
                thread.join();
            } catch( InterruptedException ignored ) {
            }
        }
    }

    public static void sleepSafely( long time ) {
        sleepSafely( time, MILLISECONDS );
    }

    public static void sleepSafely( long time, TimeUnit timeUnit ) {
        try {
            Thread.sleep( timeUnit.toMillis( time ) );
        } catch( InterruptedException ignored ) {
        }
    }

    @SuppressWarnings( "SynchronizationOnLocalVariableOrMethodParameter" )
    @SneakyThrows
    public static void waitFor( Object monitor ) {
        synchronized( monitor ) {
            monitor.wait();
        }
    }

    @SuppressWarnings( "SynchronizationOnLocalVariableOrMethodParameter" )
    public static void notifyAllFor( Object monitor ) {
        synchronized( monitor ) {
            monitor.notifyAll();
        }
    }

    public static void synchronizedOn( Object id, Runnable run ) {
        synchronizedOn( String.valueOf( id ), run );
    }

    public static void synchronizedOn( String id, Runnable run ) {
        synchronized( id.intern() ) {
            run.run();
        }
    }

    public static <R> R synchronizedOn( Object id, Supplier<R> run ) {
        return synchronizedOn( String.valueOf( id ), run );
    }

    public static <R> R synchronizedOn( String id, Supplier<R> run ) {
        synchronized( id.intern() ) {
            return run.get();
        }
    }

    @SneakyThrows
    public static <R> R synchronizedOn( Lock lock, Try.ThrowingSupplier<R> action ) {
        lock.lockInterruptibly();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    @SneakyThrows
    public static void synchronizedOn( Lock lock, Try.ThrowingRunnable action ) {
        lock.lockInterruptibly();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }


    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @SneakyThrows
    @Deprecated
    public static <T> T synchronously( Lock lock, oap.util.Try.ThrowingSupplier<T> action ) {
        return synchronizedOn( lock, action::get );
    }

    @SneakyThrows
    @Deprecated
    public static <E extends Exception> void synchronously( Lock lock, oap.util.Try.ThrowingRunnable<E> action ) {
        synchronizedOn( lock, action::run );
    }


    public static void awaitTermination( ExecutorService service, long timeout, TimeUnit unit ) {
        try {
            if( !service.awaitTermination( timeout, unit ) ) log.warn( "service {} terminated with timeout", service );
        } catch( InterruptedException e ) {
            log.warn( "abnormal termination of " + service, e );
        }
    }

    public static void withThreadName( String threadName, Runnable runnable ) {
        var oldThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName( StringUtils.isBlank( threadName ) ? oldThreadName : threadName );

            runnable.run();
        } finally {
            Thread.currentThread().setName( oldThreadName );
        }
    }

    public static <F> F withThreadName( String threadName, Supplier<F> supplier ) {
        var oldThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName( StringUtils.isBlank( threadName ) ? oldThreadName : threadName );

            return supplier.get();
        } finally {
            Thread.currentThread().setName( oldThreadName );
        }
    }
}
