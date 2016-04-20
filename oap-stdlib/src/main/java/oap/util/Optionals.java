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

import java.util.Optional;
import java.util.function.Consumer;

public class Optionals {
    public static <T> java.util.stream.Stream<T> toStream( Optional<T> opt ) {
        return opt.map( Stream::of ).orElse( Stream.empty() );
    }

    @Deprecated
    @SafeVarargs
    public static <T> Optional<T> firstPresent( Optional<T>... optionals ) {
        return findFirst( optionals );
    }

    @SafeVarargs
    public static <T> Optional<T> findFirst( Optional<T>... optionals ) {
        return Stream.of( optionals ).flatMap( Optionals::toStream ).findFirst();
    }

    public static <T> Fork<T> fork( Optional<T> opt ) {
        return new Fork<>( opt );
    }

    public static class Fork<T> {
        private Optional<T> opt;

        public Fork( Optional<T> opt ) {
            this.opt = opt;
        }

        public Fork<T> ifPresent( Consumer<? super T> consumer ) {
            opt.ifPresent( consumer );
            return this;
        }

        public Fork<T> ifAbsent( Runnable run ) {
            if( !opt.isPresent() ) run.run();
            return this;
        }
    }
}
