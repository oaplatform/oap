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
package oap.net;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.system.Env;
import oap.util.Stream;
import oap.util.function.Try;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Optional;

@Slf4j
public class Inet {
    public static final String HOSTNAME = hostname();
    public static final String LOCALHOST_NAME = resolveLocalhost();

    @SneakyThrows
    private static String resolveLocalhost() {
        return InetAddress.getByName( "127.0.0.1" ).getCanonicalHostName();
    }

    @SneakyThrows
    public static String hostname() {
        return Env.get( "HOSTNAME", InetAddress.getLocalHost().getHostName() );
    }

    public static boolean isLocalAddress( InetAddress addr ) {
        // Check if the address is a valid special local or loop back
        if( addr.isAnyLocalAddress() || addr.isLoopbackAddress() )
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress( addr ) != null;
        } catch( SocketException e ) {
            return false;
        }
    }

    public static long toLong( String ip ) {
        StringBuilder stringBuilder = new StringBuilder();
        long result = 0;
        int i = 3;

        for( char c : ip.toCharArray() )
            if( c != '.' ) stringBuilder.append( c );
            else {
                result |= Long.parseLong( stringBuilder.toString() ) << ( i * 8 );
                stringBuilder.setLength( 0 );
            }

        result |= Long.parseLong( stringBuilder.toString() );

        return result;
    }

    public static Optional<InetAddress> getLocalIp() {
        try {
            return Stream.of( NetworkInterface.getNetworkInterfaces() )
                .filter( Try.filter( i -> !i.isLoopback() && !i.isVirtual() && i.isUp() && !i.isPointToPoint() ) )
                .findFirst()
                .flatMap( i -> Stream.of( i.getInetAddresses() )
                    .filter( a -> a instanceof Inet4Address )
                    .findFirst()
                );
        } catch( SocketException e ) {
            log.error( e.getMessage(), e );
            return Optional.empty();
        }
    }

}
