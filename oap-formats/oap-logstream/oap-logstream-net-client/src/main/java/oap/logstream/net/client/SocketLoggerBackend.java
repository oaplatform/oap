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

package oap.logstream.net.client;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.disk.LogstreamException;
import oap.logstream.net.BufferCompressor;
import oap.logstream.net.BufferConfigurationMap;
import oap.logstream.net.Buffers;
import oap.message.client.MessageAvailabilityReport;
import oap.message.client.MessageSender;
import oap.util.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
    private final Buffers buffers;
    @ToString.Exclude
    private final BufferCompressor compressor = new BufferCompressor();
    /**
     * guards scheduled / flushInterval; not the object monitor - sendAsync() holds it while sending
     */
    @ToString.Exclude
    private final ReentrantLock scheduleLock = new ReentrantLock();
    /**
     * runtime parameter: the backend reports FAILED when the number of ready (not sent) buffers reaches it
     */
    @Getter
    public volatile int maxBuffers = 5000;
    private volatile boolean closed = false;
    @ToString.Exclude
    private Scheduled scheduled;
    /**
     * runtime parameter, ms; &lt;= 0 - no automatic sending
     */
    @Getter
    private volatile long flushInterval;
    /**
     * runtime parameters, see {@link LogStreamCompression} for level ranges
     */
    @Getter
    private volatile LogStreamCompression compression = LogStreamCompression.ZSTD;
    @Getter
    private volatile int gzipLevel = LogStreamCompression.GZIP.defaultLevel;
    @Getter
    private volatile int lz4Level = LogStreamCompression.LZ4.defaultLevel;
    @Getter
    private volatile int zstdLevel = LogStreamCompression.ZSTD.defaultLevel;

    public SocketLoggerBackend( MessageSender sender, int bufferSize, long flushInterval ) {
        this( sender, BufferConfigurationMap.defaultMap( bufferSize ), flushInterval );
    }

    public SocketLoggerBackend( MessageSender sender, BufferConfigurationMap configurations, long flushInterval ) {
        log.info( "flushInterval = {}", durationToString( flushInterval ) );

        this.sender = sender;
        this.buffers = new Buffers( configurations );
        reschedule( flushInterval );
        Metrics.gauge( "logstream_logging_buffers_cache", buffers.cache, Buffers.BufferCache::size );
        log.info( "SocketLoggerBackend '{}' is ready", sender == null ? "no-name" : sender.uniqueName );
    }

    public SocketLoggerBackend( MessageSender sender, int bufferSize ) {
        this( sender, BufferConfigurationMap.defaultMap( bufferSize ) );
    }

    public SocketLoggerBackend( MessageSender sender, BufferConfigurationMap configurations ) {
        this( sender, configurations, 5000 );
    }

    /**
     * configuration name ({@link BufferConfigurationMap}) -&gt; buffer size
     */
    public Map<String, Integer> getBufferSizes() {
        return buffers.bufferSizes();
    }

    /**
     * runtime change: the next buffer of every log id of the configuration has the new size,
     * the current non-empty one is sent with the next {@link #sendAsync()}
     */
    public void setBufferSize( String configurationName, int bufferSize ) {
        buffers.setBufferSize( configurationName, bufferSize );
    }

    /**
     * {@link #setBufferSize(String, int)} for all configurations
     */
    public void setBufferSize( int bufferSize ) {
        buffers.setBufferSize( bufferSize );
    }

    /**
     * runtime change, applied starting from the next {@link #availabilityReport()}
     */
    public void setMaxBuffers( int maxBuffers ) {
        Preconditions.checkArgument( maxBuffers > 0, "maxBuffers must be > 0, but %s", maxBuffers );
        log.info( "maxBuffers {} -> {}", this.maxBuffers, maxBuffers );
        this.maxBuffers = maxBuffers;
    }

    /**
     * runtime change: the periodic sending is rescheduled immediately
     *
     * @param flushInterval ms; &lt;= 0 - no automatic sending ({@link #sendAsync()} is called by the application)
     */
    public void setFlushInterval( long flushInterval ) {
        log.info( "flushInterval {} -> {}", durationToString( this.flushInterval ), durationToString( flushInterval ) );
        reschedule( flushInterval );
    }

    private void reschedule( long flushInterval ) {
        scheduleLock.lock();
        try {
            if( closed ) throw new IllegalStateException( "already closed" );

            Scheduled.cancel( scheduled );
            scheduled = flushInterval > 0
                ? Scheduler.scheduleWithFixedDelay( flushInterval, TimeUnit.MILLISECONDS, this::sendAsync )
                : null;
            this.flushInterval = flushInterval;
        } finally {
            scheduleLock.unlock();
        }
    }

    /**
     * runtime switch, applied starting from the next {@link #sendAsync()}
     */
    public void setCompression( LogStreamCompression compression ) {
        Preconditions.checkNotNull( compression );
        log.info( "compression {} -> {}", this.compression, compression );
        this.compression = compression;
    }

    public void setGzipLevel( int gzipLevel ) {
        LogStreamCompression.GZIP.checkLevel( gzipLevel );
        log.info( "gzipLevel {} -> {}", this.gzipLevel, gzipLevel );
        this.gzipLevel = gzipLevel;
    }

    public void setLz4Level( int lz4Level ) {
        LogStreamCompression.LZ4.checkLevel( lz4Level );
        log.info( "lz4Level {} -> {}", this.lz4Level, lz4Level );
        this.lz4Level = lz4Level;
    }

    public void setZstdLevel( int zstdLevel ) {
        LogStreamCompression.ZSTD.checkLevel( zstdLevel );
        log.info( "zstdLevel {} -> {}", this.zstdLevel, zstdLevel );
        this.zstdLevel = zstdLevel;
    }

    public int getCompressionLevel( LogStreamCompression compression ) {
        return switch( compression ) {
            case GZIP -> gzipLevel;
            case LZ4 -> lz4Level;
            case ZSTD -> zstdLevel;
        };
    }

    public synchronized boolean sendAsync() {
        return sendAsync( false );
    }

    private boolean sendAsync( boolean shutdown ) {
        if( shutdown || !closed ) {
            LogStreamCompression compression = this.compression;
            int level = getCompressionLevel( compression );
            buffers.forEachReadyData( b -> {
                log.trace( "Sending {} {}/{}", b, compression, level );
                byte[] data = compressor.compress( b, compression, level );
                sender.send( MESSAGE_TYPE, ( short ) ProtocolVersion.ROW_BINARY_V4.version, data, 0, data.length );
            } );
            log.trace( "Data sent to server" );
            return true;
        }

        return false;
    }

    @Override
    public Result<String, LogstreamException> log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        LogId logId = new LogId( filePreffix, logType, hostName, properties, headers, types );
        buffers.put( logId, version, buffer, offset, length );
        return Result.success( logId.toString() );
    }

    @Override
    public synchronized void close() {
        scheduleLock.lock();
        try {
            closed = true;
            Scheduled.cancel( scheduled );
            scheduled = null;
        } finally {
            scheduleLock.unlock();
        }
        Closeables.close( buffers );
        try {
            sendAsync( true );
        } finally {
            compressor.close();
        }
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
