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

import org.joda.time.DateTimeUtils;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cluster Unique Id
 */
public class Cuid {
    private static final String UNKNOWN_IP = "UUUUUUUU";
    private static String suffix = ipSuffix();
    private static Counter counter = new TimeSeedCounter();

    public static String next() {
        return Long.toHexString( counter.next() ) + suffix;
    }

    public static void reset( String suffix, long seed ) {
        reset( suffix, new SeedCounter( seed ) );
    }

    public static void resetToDefaults() {
        reset( ipSuffix(), new TimeSeedCounter() );
    }

    private static void reset( String suffix, Counter counter ) {
        Cuid.suffix = suffix;
        Cuid.counter = counter;
    }

    private static String ipSuffix() {
        try {
            return Stream.of( NetworkInterface.getNetworkInterfaces() )
                .filter( Try.filter( i -> !i.isLoopback() && !i.isVirtual() && i.isUp() && !i.isPointToPoint() ) )
                .findFirst()
                .flatMap( i -> Stream.of( i.getInetAddresses() )
                    .filter( a -> a instanceof Inet4Address && a.isSiteLocalAddress() )
                    .findFirst()
                    .map( a -> Strings.toHexString( a.getAddress() ) )
                )
                .orElse( UNKNOWN_IP );
        } catch( Exception e ) {
            return UNKNOWN_IP;
        }
    }

    public interface Counter {
        long next();
    }

    public static class SeedCounter implements Counter {
        private AtomicLong value = new AtomicLong();

        SeedCounter( long seed ) {
            this.value.set( seed );
        }

        @Override
        public long next() {
            return value.incrementAndGet();
        }
    }

    public static class TimeSeedCounter implements Counter {
        private AtomicLong value = new AtomicLong( DateTimeUtils.currentTimeMillis() << 16 );


        @Override
        public long next() {
            return value.incrementAndGet();
        }
    }
}
