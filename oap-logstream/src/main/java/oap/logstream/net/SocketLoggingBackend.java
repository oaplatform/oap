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
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketLoggingBackend implements LoggingBackend {

    private final String host;
    private final int port;
    private final Scheduled scheduled;
    protected int maxBuffers = 5000;
    private Connection connection;
    private Buffers buffers;
    protected long flushInterval = 5000;
    private boolean loggingAvailable = true;
    private boolean closed = false;
    protected long timeout = 10000;

    public SocketLoggingBackend( String host, int port, Path location, int bufferSize ) {
        this.host = host;
        this.port = port;
        this.buffers = new Buffers( location, bufferSize );
        this.scheduled = Scheduler.scheduleWithFixedDelay( flushInterval, TimeUnit.MILLISECONDS, this::send );
        Metrics.measureGauge( Metrics.name( "logging_buffers_cache" ), () -> buffers.cache.size() );

    }

    public synchronized void send() {
        if( !closed ) try {
            if( buffers.isEmpty() ) loggingAvailable = true;

            log.debug( "sending data to server..." );
            refreshConnection();

            buffers.forEachReadyData( buffer -> {
                if( !sendBuffer( buffer ) ) {
                    refreshConnection();
                    return sendBuffer( buffer );
                }
                return true;

            } );
            log.debug( "sending done" );
        } catch( Exception e ) {
            loggingAvailable = false;
            log.warn( e.getMessage() );
            log.trace( e.getMessage(), e );
            Closeables.close( connection );
        }

        if( !loggingAvailable ) log.debug( "logging unavailable" );

    }

    private void refreshConnection() {
        if( this.connection == null || !connection.isConnected() ) {
            Closeables.close( connection );
            log.debug( "opening connection..." );
            this.connection = new ChannelConnection( host, port, timeout );
        }
    }

    private Boolean sendBuffer( Buffer buffer ) {
        return Metrics.measureTimer( Metrics.name( "logging_buffer_send_time" ), () -> {
            try {
                log.trace( "sending {}", buffer );
                connection.write( buffer.data(), 0, buffer.length() );
                Metrics.measureCounterIncrement( Metrics.name( "logging_socket" ), buffer.length() );
                loggingAvailable = true;
                return true;
            } catch( Exception e ) {
                loggingAvailable = false;
                log.warn( e.getMessage() );
                log.trace( e.getMessage(), e );
                Closeables.close( connection );
                return false;
            }
        } );
    }

    @Override
    public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {
        buffers.put( fileName, buffer, offset, length );
    }

    @Override
    public synchronized void close() {
        closed = true;
        Scheduled.cancel( scheduled );
        Closeables.close( connection );
        Closeables.close( buffers );
    }

    @Override
    public boolean isLoggingAvailable() {
        return loggingAvailable && !closed && buffers.readyBuffers() < maxBuffers;
    }
}
