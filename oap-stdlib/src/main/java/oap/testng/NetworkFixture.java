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

package oap.testng;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NetworkFixture implements Fixture {
    public static final NetworkFixture FIXTURE = new NetworkFixture();
    private final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();

    public int port() {
        return FIXTURE.port( "DEFAULT" );
    }

    public int port( String key ) {
        return ports.computeIfAbsent( key, k -> {
            try( var socket = new ServerSocket() ) {
                socket.setReuseAddress( true );
                socket.bind( new InetSocketAddress( 0 ) );
                var localPort = socket.getLocalPort();
                log.debug( "finding port for key=" + key + "... port=" + localPort );
                return localPort;
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }

    @Override
    public void beforeMethod() {
        clearPorts();
    }

    @Override
    public void afterMethod() {
        clearPorts();
    }

    public void clearPorts() {
        log.debug( "clear ports" );
        ports.clear();
    }
}
