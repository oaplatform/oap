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

package oap.message;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedThread;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.util.Lists;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MessageServer implements Runnable, Closeable {
    public final HashMap<Byte, MessageListener> map = new HashMap<>();
    public final long hashTtl;
    public final int clientHashCacheSize = 1024;
    private final List<MessageListener> listeners;
    private final int port;
    private final Path controlStatePath;
    private final SynchronizedThread thread = new SynchronizedThread( this );
    private ThreadPoolExecutor executor;
    private final Counter rejectedCounter;
    private final Counter handledCounter;
    private final AtomicInteger activeCounter = new AtomicInteger();
    public long soTimeout = 60000;
    private ServerSocket serverSocket;
    private MessageHashStorage hashes;
    public int maximumPoolSize = 1024;

    public MessageServer( Path controlStatePath, int port, List<MessageListener> listeners, long hashTtl ) {
        this.controlStatePath = controlStatePath;
        this.port = port;
        this.listeners = listeners;
        this.hashTtl = hashTtl;

        rejectedCounter = Metrics.counter( "oap.message.server", "port", String.valueOf( port ), "type", "rejected" );
        handledCounter = Metrics.counter( "oap.message.server", "port", String.valueOf( port ), "type", "handled" );
        Metrics.gauge( "oap.message.server", Tags.of( "port", String.valueOf( port ), "type", "active" ), this, ms -> ms.activeCounter.doubleValue() );
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    public void start() {
        log.info( "maximumPoolSize = {}, port = {}, clientHashCacheSize = {}, listeners = {}",
            maximumPoolSize, port, clientHashCacheSize, Lists.map( listeners, MessageListener::getInfo ) );

        executor =
            new ThreadPoolExecutor( 0, maximumPoolSize, 100, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ThreadFactoryBuilder().setNameFormat( "socket-message-worker-%d" ).build(),
                new java.util.concurrent.ThreadPoolExecutor.AbortPolicy() );
        
        hashes = new MessageHashStorage( clientHashCacheSize );

        try {
            if( controlStatePath.toFile().exists() ) hashes.load( controlStatePath );
        } catch( Exception e ) {
            log.warn( e.getMessage() );
        }

        for( var listener : listeners ) {
            var d = this.map.put( listener.getId(), listener );
            if( d != null )
                throw new IllegalArgumentException( "duplicate [" + listener.getInfo() + ", " + d.getInfo() + "]" );
        }

        try {
            serverSocket = new ServerSocket( port );

            serverSocket.setReuseAddress( true );
            serverSocket.setSoTimeout( 5000 );
            log.debug( "ready to rock on {}", serverSocket.getLocalSocketAddress() );
            thread.start();

        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public void run() {
        try {
            Socket socket = null;
            while( thread.isRunning() && !serverSocket.isClosed() ) try {
                socket = serverSocket.accept();
                handledCounter.increment();
                log.debug( "accepted connection {}", socket );
                executor.execute( new MessageHandler( socket, soTimeout, map, hashes, hashTtl, activeCounter ) );
            } catch( RejectedExecutionException e ) {
                rejectedCounter.increment();
                try {
                    if( socket != null ) {
                        socket.close();
                        socket = null;
                    }
                } catch( IOException ioException ) {
                    log.error( ioException.getMessage() );
                }
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

    public void preStop() {
        Closeables.close( serverSocket );
        if( thread.isRunning() ) thread.stop();
        Closeables.close( executor );
    }

    @Override
    public void close() {
        try {
            preStop();
            hashes.store( controlStatePath );
            executor.shutdownNow();
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
