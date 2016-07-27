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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cluster Unique Id
 */
public class Cuid {
    private static String suffix = defaultSuffix();
    private static AtomicLong seed = new AtomicLong( DateTimeUtils.currentTimeMillis() );

    public static String next() {
        return Long.toHexString( seed.getAndIncrement() ) + suffix;
    }

    public static synchronized void reset( String suffix, long seed ) {
        Cuid.suffix = suffix;
        Cuid.seed.set( seed );
    }

    public static void resetToDefaults() {
        reset( defaultSuffix(), DateTimeUtils.currentTimeMillis() );
    }

    private static String defaultSuffix() {
        try {
            return Stream.of( NetworkInterface.getNetworkInterfaces() )
                .filter( Try.filter( i -> !i.isLoopback() && !i.isVirtual() && i.isUp() ) )
                .findFirst()
                .map( Try.map( i -> Strings.toHexString( i.getHardwareAddress() ) ) )
                .orElse( "XXXXXXXXXXXX" );
        } catch( SocketException e ) {
            throw new RuntimeException( e );
        }
    }
}
