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
package oap.util.function;

import com.google.common.base.Suppliers;
import oap.concurrent.Once;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Functions {

    public static <T> Supplier<T> memoize( Supplier<T> delegate ) {
        return () -> Suppliers.memoize( delegate::get ).get();
    }

    /**
     * @see #ifInstance(Object, Class, Function)
     */
    @Deprecated
    public static <V, T extends V, R> Optional<R> applyIfInstanceOf( V value, Class<T> clazz, Function<T, R> f ) {
        return ifInstance( value, clazz, f );

    }

    @SuppressWarnings( "unchecked" )
    public static <V, T extends V, R> Optional<R> ifInstance( V value, Class<T> clazz, Function<T, R> f ) {
        return clazz.isInstance( value )
            ? Optional.ofNullable( f.apply( ( T ) value ) )
            : Optional.empty();
    }

    public static Runnable once( Runnable runnable ) {
        return Once.once( runnable );
    }

    public static Runnable exception( Exception exception ) {
        return Try.run( () -> {
            throw exception;
        } );
    }

    public static <T> Consumer<T> exception( Function<T, Exception> exception ) {
        return Try.consume( a -> {
            throw exception.apply( a );
        } );
    }

    public static Supplier<IllegalArgumentException> illegalArgument( String message ) {
        return () -> new IllegalArgumentException( message );
    }

    public static Supplier<IllegalArgumentException> illegalArgument( String format, Object... args ) {
        return illegalArgument( String.format( format, args ) );
    }

    @SuppressWarnings( { "unchecked", "checkstyle:TypeName" } )
    public static class empty {
        private static final Consumer<?> CONSUMER = v -> {};
        private static final BiConsumer<?, ?> BI_CONSUMER = ( v, u ) -> {};
        private static final Predicate<?> ACCEPT_ALL = x -> true;
        private static final Predicate<?> REJECT_ALL = x -> false;
        private static final Runnable NOOP = () -> {};
        @Deprecated
        public static final Runnable run = NOOP;

        public static Runnable noop() {
            return NOOP;
        }

        public static <T> Consumer<T> consume() {
            return ( Consumer<T> ) CONSUMER;
        }

        public static <T, U> BiConsumer<T, U> biConsume() {
            return ( BiConsumer<T, U> ) BI_CONSUMER;
        }

        public static <I, R> Function<I, R> identity() {
            return i -> ( R ) i;
        }

        public static <T> Predicate<T> accept() {
            return ( Predicate<T> ) ACCEPT_ALL;
        }

        public static <T> Predicate<T> reject() {
            return ( Predicate<T> ) REJECT_ALL;
        }
    }

}
