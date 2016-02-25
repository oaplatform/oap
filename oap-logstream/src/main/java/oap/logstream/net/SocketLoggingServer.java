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
import oap.io.Files;
import oap.io.Sockets;
import oap.logstream.LoggingBackend;
import oap.util.concurrent.ThreadPoolExecutor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketLoggingServer implements Runnable {
    private final ExecutorService executor =
        new ThreadPoolExecutor( 0, 1024, 100, TimeUnit.SECONDS, new SynchronousQueue<>() );
    private final SynchronizedThread thread = new SynchronizedThread( this );
    protected int soTimeout = 60000;
    private int port;
    private int bufferSize;
    private LoggingBackend backend;
    private Path controlState;
    private List<Worker> workers = new ArrayList<>();
    private ServerSocket serverSocket;
    private Map<String, Long> control = new ConcurrentHashMap<>();

    public SocketLoggingServer( int port, int bufferSize, LoggingBackend backend, Path controlState ) {
        this.port = port;
        this.bufferSize = bufferSize;
        this.backend = backend;
        this.controlState = controlState;
    }

    @Override
    public void run() {
        try {
            while( thread.isRunning() && !serverSocket.isClosed() ) try {
                Socket socket = serverSocket.accept();
                log.debug( "accepted connection {}", socket );
                executor.submit( new Worker( socket ) );
            } catch( SocketTimeoutException ignore ) {
            } catch( IOException e ) {
                if( !"Socket closed".equals( e.getMessage() ) )
                    log.error( e.getMessage(), e );
            }
        } finally {
            Closeables.close( serverSocket );
            workers.forEach( Closeables::close );
        }
    }

    public void start() {
        try {
            if( controlState.toFile().exists() ) this.control = Files.readObject( controlState );
        } catch( Exception e ) {
            log.warn( e.getMessage() );
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.bind( new InetSocketAddress( port ) );
            log.debug( "ready to rock " + serverSocket.getLocalSocketAddress() );
            thread.start();

        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void stop() {
        Closeables.close( serverSocket );
        thread.stop();
        workers.forEach( Closeables::close );
        Closeables.close( executor );
        Files.writeObject( controlState, control );
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
            String hostName = null;
            try {
                DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                DataInputStream in = new DataInputStream( socket.getInputStream() );
                socket.setSoTimeout( soTimeout );
                socket.setKeepAlive( true );
                socket.setTcpNoDelay( true );
                hostName = socket.getInetAddress().getCanonicalHostName();
                log.debug( "[{}] start logging... ", hostName );
                while( !closed ) {
                    long digestionId = in.readLong();
                    long lastId = control.computeIfAbsent( hostName, h -> 0L );
                    int size = in.readInt();
                    String selector = in.readUTF();
                    if( size > bufferSize ) {
                        log.trace( "pong size {}", -10 );
                        out.writeInt( -10 );
                        throw new IOException( "buffer overflow: chunk size is " + size + " when buffer size is " + bufferSize + " from " + hostName );
                    }
                    in.readFully( buffer, 0, size );
                    if( lastId < digestionId ) {
                        log.trace( "[{}] logging ({}, {}, {})", hostName, digestionId, selector, size );
                        backend.log( hostName, selector, buffer, 0, size );
                        control.put( hostName, digestionId );
                    } else log.warn( "[{}] buffer {} already written ({})", hostName, digestionId, lastId );
                    log.trace( "pong size {}", size );
                    out.writeInt( size );
                }
            } catch( EOFException e ) {
                log.debug( "[{}] {} closed", hostName, socket );
            } catch( IOException e ) {
                log.error( "[" + hostName + "] " + e.getMessage(), e );
            } finally {
                Sockets.close( socket );
                workers.remove( this );
            }
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }
}
