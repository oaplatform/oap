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
package oap.logstream.net;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedThread;
import oap.io.Closeables;
import oap.io.Sockets;
import oap.logstream.LoggingBackend;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class SocketLoggingServer implements Runnable {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SynchronizedThread thread = new SynchronizedThread( this );
    private int port;
    private int bufferSize;
    private LoggingBackend backend;
    private List<Worker> workers = new ArrayList<>();
    private ServerSocket serverSocket;

    public SocketLoggingServer( int port, int bufferSize, LoggingBackend backend ) {
        this.port = port;
        this.bufferSize = bufferSize;
        this.backend = backend;
    }

    @Override
    public void run() {
        try {
            while( thread.isRunning() ) try {
                Socket socket = serverSocket.accept();
                log.debug( "accepted connection " + socket );
                executor.submit( new Worker( socket ) );
            } catch( SocketTimeoutException ignore ) {
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            }
        } finally {
            Closeables.close( serverSocket );
            workers.forEach( Closeables::close );
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.setSoTimeout( 1000 );
            serverSocket.bind( new InetSocketAddress( port ) );
            log.debug( "ready to rock " + serverSocket.getLocalSocketAddress() );
            thread.start();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void stop() {
        thread.stop();
    }

    public class Worker implements Runnable, Closeable {
        private Socket socket;
        private byte[] buffer = new byte[bufferSize];
        private boolean closed;

        public Worker( Socket socket ) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream in = new DataInputStream( socket.getInputStream() );
                DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                String hostName = socket.getInetAddress().getCanonicalHostName();
                log.debug( "start logging for " + hostName );
                while( !closed ) {
                    String selector = in.readUTF();
                    if( !"SHUTDOWN".equals( selector ) ) {
                        int size = in.readInt();
                        if( size > bufferSize )
                            throw new IOException(
                                "buffer overflow: chunk size is " + size + " when buffer size is " + bufferSize );
                        in.readFully( buffer, 0, size );
                        out.writeInt( size );
                        log.trace( "logging " + size + " bytes to " + selector );
                        backend.log( hostName, selector, buffer, 0, size );
                    } else close();
                }
            } catch( EOFException e ) {
                log.debug( socket + " closed" );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if( !closed ) {
                this.closed = true;
                Sockets.close( socket );
                workers.remove( this );
            }
        }
    }
}
