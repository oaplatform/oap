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
package oap.util;

import com.google.common.base.Throwables;
import oap.reflect.Reflect;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.*;

public class Try {

    public static Runnable run( ThrowingRunnable<? extends Exception> throwing ) {
        return throwing.asRunnable();
    }

    public static <R> Supplier<R> supply( ThrowingSupplier<R> throwing ) {
        return throwing.asSupplier();
    }

    public static <T> Consumer<T> consume( ThrowingConsumer<T> throwing ) {
        return throwing.asConsumer();
    }

    public static <T, U> BiConsumer<T, U> consume( ThrowingBiConsumer<T, U> throwing ) {
        return throwing.asConsumer();
    }

    public static <T> Predicate<T> filter( ThrowingPredicate<T> throwing ) {
        return throwing.asPredicate();
    }

    public static <T, R> Function<T, R> map( ThrowingFunction<T, R> throwing ) {
        return throwing.asFunction();
    }

    public static <T, R> Function<T, R> mapOrThrow( ThrowingFunction<T, R> throwing,
                                                    Class<? extends RuntimeException> e ) {
        return throwing.orElseThrow( e );
    }

    public static <T> ToLongFunction<T> mapToLong( ThrowingToLongFunction<T> throwing ) {
        return throwing.asFunction();
    }

    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean test( T t ) throws Exception;

        default Predicate<T> asPredicate() {
            return t -> {
                try {
                    return this.test( t );
                } catch( Exception e ) {
                    throw Throwables.propagate( e );
                }
            };
        }


    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply( T t ) throws Exception;

        default Function<T, R> asFunction() {
            return t -> {
                try {
                    return this.apply( t );
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                } catch( Exception e ) {
                    throw Throwables.propagate( e );
                }
            };
        }

        default Function<T, R> orElseThrow( Class<? extends RuntimeException> clazz ) {
            return t -> {
                try {
                    return this.apply( t );
                } catch( Exception e ) {
                    throw Reflect.newInstance( clazz, e );
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable<T extends Exception> {
        void run() throws T;

        default Runnable asRunnable() {
            return () -> {
                try {
                    this.run();
                } catch( Exception e ) {
                    Throwables.propagate( e );
                }
            };
        }

    }

    @FunctionalInterface
    public interface ThrowingSupplier<R> {
        R get() throws Exception;

        default Supplier<R> asSupplier() {
            return () -> {
                try {
                    return this.get();
                } catch( Exception e ) {
                    throw Throwables.propagate( e );
                }
            };
        }

    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept( T t ) throws Exception;

        default Consumer<T> asConsumer() {
            return t -> {
                try {
                    this.accept( t );
                } catch( Exception e ) {
                    Throwables.propagate( e );
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingToLongFunction<T> {
        long applyToLong( T t ) throws Exception;

        default ToLongFunction<T> asFunction() {
            return t -> {
                try {
                    return this.applyToLong( t );
                } catch( Exception e ) {
                    throw Throwables.propagate( e );
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void accept( T t, U u ) throws Exception;

        default BiConsumer<T, U> asConsumer() {
            return ( t, u ) -> {
                try {
                    this.accept( t, u );
                } catch( Exception e ) {
                    Throwables.propagate( e );
                }
            };
        }
    }
}
