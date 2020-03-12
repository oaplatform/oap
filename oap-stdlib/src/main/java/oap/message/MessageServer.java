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
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedThread;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.util.Lists;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 2019-12-10.
 */
@Slf4j
public class MessageServer implements Runnable, Closeable {
    public final HashMap<Byte, MessageListener> map = new HashMap<>();
    public final long hashTtl;
    public final int clientHashCacheSize = 1024;
    private final List<MessageListener> listeners;
    private final int port;
    private final Path controlStatePath;
    private final SynchronizedThread thread = new SynchronizedThread( this );
    private final ThreadPoolExecutor executor =
        new ThreadPoolExecutor( 0, 1024, 100, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "socket-message-worker-%d" ).build() );
    protected int soTimeout = 60000;
    private ServerSocket serverSocket;
    private MessageHashStorage hashes;

    public MessageServer( Path controlStatePath, int port, List<MessageListener> listeners, long hashTtl ) {
        this.controlStatePath = controlStatePath;
        this.port = port;
        this.listeners = listeners;
        this.hashTtl = hashTtl;
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    public void start() {
        log.info( "port = {}, clientHashCacheSize = {}, listeners = {}", port, clientHashCacheSize, Lists.map( listeners, MessageListener::getInfo ) );

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
            while( thread.isRunning() && !serverSocket.isClosed() ) try {
                var socket = serverSocket.accept();
                log.debug( "accepted connection {}", socket );
                executor.execute( new MessageHandler( socket, soTimeout, map, hashes, hashTtl ) );
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
        Closeables.close( executor, 10, TimeUnit.SECONDS );
    }

    @Override
    public void close() {
        preStop();
        try {
            hashes.store( controlStatePath );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
