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

import oap.net.Inet;

import java.util.concurrent.atomic.AtomicLong;

import static oap.util.Strings.toHexString;

/**
 * Cluster Unique Id
 */
public interface Cuid {
    Cuid UNIQUE = new UniqueCuid();

    String next();

    long nextLong();

    String last();

    static IncrementalCuid incremental( long seed ) {
        return new IncrementalCuid( seed );
    }

    class UniqueCuid implements Cuid {
        private static final String UNKNOWN_IP = "UUUUUUUU";
        private static final String suffix = Inet.getLocalIp()
            .map( a -> toHexString( a.getAddress() ) )
            .orElse( UNKNOWN_IP );
        private static final IncrementalUniqueValueGenerator generator = new IncrementalUniqueValueGenerator();

        @Override
        public String next() {
            return format( nextLong() );
        }

        public long nextLong() {
            return generator.next();
        }

        @Override
        public String last() {
            return format( generator.last() );
        }

        private static String format( long value ) {
            return toHexString( value ) + suffix;
        }


    }

    class IncrementalCuid implements Cuid {
        private final AtomicLong counter = new AtomicLong();

        public IncrementalCuid( long seed ) {
            this.counter.set( seed );
        }

        @Override
        public String next() {
            return toHexString( nextLong() );
        }

        @Override
        public long nextLong() {
            return this.counter.incrementAndGet();
        }

        @Override
        public String last() {
            return toHexString( this.counter.get() );
        }

        public void reset( long value ) {
            this.counter.set( value );
        }

    }
}
