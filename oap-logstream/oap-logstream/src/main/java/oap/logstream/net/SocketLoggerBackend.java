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

import io.micrometer.core.instrument.Metrics;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.message.MessageAvailabilityReport;
import oap.message.MessageSender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.AvailabilityReport.State.OPERATIONAL;
import static oap.logstream.LogStreamProtocol.MESSAGE_TYPE;
import static oap.util.Dates.durationToString;

@Slf4j
@ToString
public class SocketLoggerBackend extends AbstractLoggerBackend {
    public static final String FAILURE_IO_STATE = "IO";
    public static final String FAILURE_BUFFERS_STATE = "BUFFERS";
    public static final String FAILURE_SHUTDOWN_STATE = "SHUTDOWN";

    private final MessageSender sender;
    private final Scheduled scheduled;
    private final Buffers buffers;
    public int maxBuffers = 5000;
    private volatile boolean closed = false;

    public SocketLoggerBackend( MessageSender sender, int bufferSize, long flushInterval ) {
        this( sender, BufferConfigurationMap.defaultMap( bufferSize ), flushInterval );
    }

    public SocketLoggerBackend( MessageSender sender, BufferConfigurationMap configurations, long flushInterval ) {
        log.info( "flushInterval = {}", durationToString( flushInterval ) );

        this.sender = sender;
        this.buffers = new Buffers( configurations );
        this.scheduled = flushInterval > 0
            ? Scheduler.scheduleWithFixedDelay( flushInterval, TimeUnit.MILLISECONDS, this::sendAsync )
            : null;
        configurations.forEach( ( name, conf ) -> Metrics.gauge( "logstream_logging_buffers_cache",
            buffers.cache,
            c -> c.size( conf.bufferSize )
        ) );
        log.info( "SocketLoggerBackend '{}' is ready", sender == null ? "no-name" : sender.name );
    }

    public SocketLoggerBackend( MessageSender sender, int bufferSize ) {
        this( sender, BufferConfigurationMap.defaultMap( bufferSize ) );
    }

    public SocketLoggerBackend( MessageSender sender, BufferConfigurationMap configurations ) {
        this( sender, configurations, 5000 );
    }

    public synchronized boolean sendAsync() {
        return sendAsync( false );
    }

    private boolean sendAsync( boolean shutdown ) {
        if( shutdown || !closed ) {
            buffers.forEachReadyData( b -> {
                log.trace( "Sending {}", b );
                sender.send( MESSAGE_TYPE, ( short ) b.protocolVersion.version, b.data(), 0, b.length() );
            } );
            log.trace( "Data sent to server" );
            return true;
        }

        return false;
    }

    @Override
    public void log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                     String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        buffers.put( new LogId( filePreffix, logType, hostName, properties, headers, types ), version, buffer, offset, length );
    }

    @Override
    public synchronized void close() {
        closed = true;
        Scheduled.cancel( scheduled );
        Closeables.close( buffers );
        sendAsync( true );
    }

    @Override
    public AvailabilityReport availabilityReport() {
        var ioFailed = sender.availabilityReport( MESSAGE_TYPE ).state != MessageAvailabilityReport.State.OPERATIONAL;
        var buffersFailed = this.buffers.readyBuffers() >= maxBuffers;
        var operational = !ioFailed && !closed && !buffersFailed;
        if( operational ) {
            return new AvailabilityReport( OPERATIONAL );
        }
        var state = new HashMap<String, AvailabilityReport.State>();
        state.put( FAILURE_IO_STATE, ioFailed ? FAILED : OPERATIONAL );
        state.put( FAILURE_BUFFERS_STATE, buffersFailed ? FAILED : OPERATIONAL );
        state.put( FAILURE_SHUTDOWN_STATE, closed ? FAILED : OPERATIONAL );
        if( buffersFailed ) this.buffers.report();
        return new AvailabilityReport( FAILED, state );
    }
}
