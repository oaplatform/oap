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
import oap.util.Try;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

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
        try {
            Thread.sleep( time );
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

    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @SneakyThrows
    public static <T> T synchronously( Lock lock, Try.ThrowingSupplier<T> func ) {
        lock.lockInterruptibly();
        try {
            return func.get();
        } finally {
            lock.unlock();
        }
    }

    @SneakyThrows
    public static void synchronously( Lock lock, Try.ThrowingRunnable func ) {
        lock.lockInterruptibly();
        try {
            func.run();
        } finally {
            lock.unlock();
        }
    }


}
