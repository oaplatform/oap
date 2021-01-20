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
import oap.concurrent.SynchronizedRunnable;
import oap.concurrent.Threads;
import oap.http.server.HttpServer;
import oap.io.Closeables;

import java.io.Closeable;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static oap.concurrent.Once.executeOnce;
import static oap.io.Sockets.socketClosed;

@Slf4j
public abstract class AbstractHttpListener extends SynchronizedRunnable implements Closeable {
    private final HttpServer server;
    protected int timeout = 1000;
    protected long sleep = 60000;
    private ServerSocket serverSocket;

    AbstractHttpListener( HttpServer server ) {
        this.server = server;
    }

    protected abstract ServerSocket createSocket();

    @Override
    public void run() {
        try {
            try {
                while( !Thread.interrupted() && ( serverSocket = createSocket() ) == null ) {
                    executeOnce( () -> log.warn( "Server socket cannot be opened; waiting for it ..." ) );
                    Threads.sleepSafely( sleep );
                }
                log.debug( "ready to rock [{}]", serverSocket );
            } finally {
                this.notifyReady();
            }

            while( !Thread.interrupted() && !serverSocket.isClosed() )
                try {
                    server.accepted( serverSocket.accept() );
                } catch( SocketTimeoutException ignore ) {
                } catch( SocketException e ) {
                    if( socketClosed( e ) ) log.debug( e.getMessage() );
                    else log.error( e.getMessage(), e );
                } catch( Exception e ) {
                    log.error( e.getMessage(), e );
                }
        } catch( RuntimeException t ) {
            log.error( t.getMessage(), t );
            throw t;
        } finally {
            close();
        }
    }

    public void preStop() {
        close();
    }

    @Override
    public void close() {
        if( !serverSocket.isClosed() )
            Closeables.close( serverSocket );
    }
}
