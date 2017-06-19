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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.concurrent.SynchronizedThread;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.Sockets;
import oap.logstream.LoggingBackend;
import oap.logstream.LoggingEvent;
import oap.logstream.exceptions.BackendLoggingIsNotAvailableException;
import oap.logstream.exceptions.BufferOverflowException;
import oap.logstream.exceptions.LoggerException;
import oap.metrics.Metrics;
import oap.metrics.Name;

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
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static oap.concurrent.Threads.isInterrupted;

@Slf4j
public class SocketLoggingServer extends LoggingEvent implements Runnable {

    private final ThreadPoolExecutor executor =
        new ThreadPoolExecutor( 0, 1024, 100, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "socket-logging-worker-%d" ).build() );
    private final SynchronizedThread thread = new SynchronizedThread( this );
    private final Name workersMetric;

    protected int soTimeout = 60000;
    private int port;
    private int bufferSize;
    private LoggingBackend backend;
    private Path controlStatePath;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, AtomicLong> control = new ConcurrentHashMap<>();

    public SocketLoggingServer( int port, int bufferSize, LoggingBackend backend, Path controlStatePath ) {
        this.port = port;
        this.bufferSize = bufferSize;
        this.backend = backend;
        this.controlStatePath = controlStatePath;
        this.workersMetric = Metrics.measureGauge(
            Metrics.name( "logging.server." + port + ".workers" ),
            () -> executor.getTaskCount() - executor.getCompletedTaskCount() );
    }

    @Override
    public void run() {
        try {
            while( thread.isRunning() && !serverSocket.isClosed() ) try {
                Socket socket = serverSocket.accept();
                log.debug( "accepted connection {}", socket );
                executor.execute( new Worker( socket ) );
            } catch( SocketTimeoutException ignore ) {
            } catch( IOException e ) {
                if( !"Socket closed".equals( e.getMessage() ) )
                    log.error( e.getMessage(), e );
            }
        } finally {
            Closeables.close( serverSocket );
            Closeables.close( executor );
        }
    }

    public void start() {
        try {
            if( controlStatePath.toFile().exists() ) this.control = Files.readObject( controlStatePath );
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
        Closeables.close( executor );
        Metrics.unregister( workersMetric );
        Files.writeObject( controlStatePath, control );
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
            byte clientId = -1;

            try {
                val out = new DataOutputStream( socket.getOutputStream() );
                val in = new DataInputStream( socket.getInputStream() );
                socket.setSoTimeout( soTimeout );
                socket.setKeepAlive( true );
                socket.setTcpNoDelay( true );

                hostName = socket.getInetAddress().getCanonicalHostName();
                clientId = in.readByte();
                val digestKey = hostName + clientId;

                log.info( "client = {}/{}", hostName, clientId );

                log.debug( "[{}] start logging... ", hostName, clientId );
                while( !closed && !isInterrupted() ) {
                    long digestionId = in.readLong();
                    val lastId = control.computeIfAbsent( digestKey, h -> new AtomicLong( 0L ) );
                    int size = in.readInt();
                    String selector = in.readUTF();
                    if( size > bufferSize ) {
                        out.writeInt( SocketError.BUFFER_OVERFLOW.code );
                        val exception = new BufferOverflowException( hostName, clientId, selector, bufferSize, size );
                        fireError( exception );
                        throw exception;
                    }
                    in.readFully( buffer, 0, size );
                    if( !backend.isLoggingAvailable() ) {
                        out.writeInt( SocketError.BACKEND_UNAVAILABLE.code );
                        val exception = new BackendLoggingIsNotAvailableException( hostName, clientId );
                        fireError( exception );
                        throw exception;
                    }
                    if( lastId.get() < digestionId ) {
                        log.trace( "[{}/{}] logging ({}, {}, {})", hostName, clientId, digestionId, selector, size );
                        backend.log( hostName, selector, buffer, 0, size );
                        lastId.set( digestionId );
                    } else
                        log.warn( "[{}/{}] buffer ({}, {}, {}) already written. Last written buffer is ({})",
                            hostName, clientId, digestionId, selector, size, lastId );
                    out.writeInt( size );
                }
            } catch( EOFException e ) {
                fireError( "[" + hostName + "/" + clientId + "] " + socket + " ended, closed" );
                log.debug( "[{}/{}] {} ended, closed", hostName, clientId, socket );
            } catch( SocketTimeoutException e ) {
                fireError( "[" + hostName + "/" + clientId + "] no activity on socket for " + soTimeout + "ms, timeout, closing..." );
                log.info( "[{}/{}] no activity on socket for {}ms, timeout, closing...", hostName, clientId, soTimeout );
                log.trace( "[" + hostName + "/" + clientId + "] " + e.getMessage(), e );
            } catch( LoggerException e ) {
                log.error( "[" + hostName + "/" + clientId + "] " + e.getMessage(), e );
            } catch( Exception e ) {
                fireError( "[" + hostName + "/" + clientId + "] " );
                log.error( "[" + hostName + "/" + clientId + "] " + e.getMessage(), e );
            } finally {
                Sockets.close( socket );
            }
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }

}
