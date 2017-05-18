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

import com.google.common.base.Suppliers;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Functions {

    @FunctionalInterface
    public interface TriFunction<T, U, S, R> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @param s the third function argument
         * @return the function result
         */
        R apply( T t, U u, S s );
    }

    @SuppressWarnings( "unchecked" )
//    CHECKSTYLE:OFF
    public static class empty {
//    CHECKSTYLE:ON

        private static final Consumer<?> CONSUMER = v -> {};

        private static final BiConsumer<?, ?> BI_CONSUMER = ( v, u ) -> {};
        private static final Predicate<?> acceptAll = x -> true;
        private static final Predicate<?> rejectAll = x -> false;

        public static Runnable run = () -> {};

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
            return ( Predicate<T> ) acceptAll;
        }

        public static <T> Predicate<T> reject() {
            return ( Predicate<T> ) rejectAll;
        }
    }

    public static <T> Supplier<T> memoize( Supplier<T> delegate ) {
        return () -> Suppliers.memoize( delegate::get ).get();
    }
}
