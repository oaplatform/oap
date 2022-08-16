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

import lombok.SneakyThrows;
import lombok.ToString;
import oap.net.Inet;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static oap.util.Strings.toHexString;

/**
 * Cluster Unique Id
 */
public interface Cuid {
    Cuid UNIQUE = new UniqueCuid();

    static IncrementalCuid incremental( long seed ) {
        return new IncrementalCuid( seed );
    }

    String next();

    long nextLong();

    String last();

    class UniqueCuid implements Cuid {
        private static final String UNKNOWN_IP = "UUUUUUUU";
        private static final String suffix = Inet.getLocalIp()
            .map( a -> toHexString( a.getAddress() ) )
            .orElse( UNKNOWN_IP );
        private static final IncrementalUniqueValueGenerator generator = new IncrementalUniqueValueGenerator();

        private static String format( long value ) {
            return toHexString( value ) + suffix;
        }

        @SneakyThrows
        @Deprecated
        public static Info toString( String cuid ) {
            return parse( cuid );
        }

        @SneakyThrows
        public static Info parse( String cuid ) {
            var ip = cuid.substring( cuid.length() - 8 );
            int[] ipInts;

            if( UNKNOWN_IP.equals( ip ) ) ipInts = new int[] { -1, -1, -1, -1 };
            else {
                var ipBytes = Hex.decodeHex( ip );
                ipInts = IntStream.range( 0, ipBytes.length ).map( i -> ipBytes[i] & 0xFF ).toArray();
            }

            var timeStr = cuid.substring( 0, cuid.length() - 8 );
            System.out.println(timeStr);
            var timeL = Long.parseLong( timeStr, 16 );
            System.out.println(timeL);
            System.out.println(timeL >> 16);
            var time = new DateTime( timeL >> 16, DateTimeZone.UTC );
            var count = timeL & 0xFFFF;

            return new Info( ipInts, time, count );
        }

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

        @ToString
        public static class Info {
            public final int[] ip;
            public final DateTime time;
            public final long count;

            public Info( int[] ip, DateTime time, long count ) {
                this.ip = ip;
                this.time = time;
                this.count = count;
            }
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
