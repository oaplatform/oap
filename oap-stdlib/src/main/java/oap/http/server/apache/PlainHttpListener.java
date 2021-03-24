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

package oap.http.server.apache;

import lombok.extern.slf4j.Slf4j;
import oap.http.server.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@Slf4j
public class PlainHttpListener extends AbstractHttpListener {
    protected final InetSocketAddress address;

    public PlainHttpListener( HttpServer server, int port ) {
        this( server, new InetSocketAddress( port ) );
    }

    public PlainHttpListener( HttpServer server, InetSocketAddress address ) {
        super( server );
        this.address = address;
    }

    public void start() {
        log.info( "port = {}", address.getPort() );
    }

    @Override
    protected ServerSocket createSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.setSoTimeout( timeout );
            log.info( "binding to {}", address );
            serverSocket.bind( address );
            return serverSocket;
        } catch( BindException e ) {
            log.error( "cannot bind to address {}", address );
            throw new UncheckedIOException( e );
        } catch( IOException e ) {
            log.error( "failed to create server socket {}", address );
            throw new UncheckedIOException( e );
        }
    }
}
