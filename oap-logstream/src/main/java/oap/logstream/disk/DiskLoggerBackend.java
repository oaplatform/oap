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

package oap.logstream.disk;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.dictionary.LogConfiguration;
import oap.io.Closeables;
import oap.io.Files;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LoggerBackend;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.metrics.Metrics;
import oap.metrics.Metrics2;
import oap.metrics.Name;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.AvailabilityReport.State.OPERATIONAL;

@Slf4j
public class DiskLoggerBackend extends LoggerBackend {
    public static final int DEFAULT_BUFFER = 1024 * 100;
    public static final String METRICS_LOGGING_DISK = "logging.disk";
    public static final String METRICS_LOGGING_DISK_BUFFERS = "logging.disk.buffers";
    public static final long DEFAULT_FREE_SPACE_REQUIRED = 2000000000L;
    public String filePattern = "${LOG_NAME}/${YEAR}-${MONTH}/${DAY}/${LOG_TYPE}_v${LOG_VERSION}_${CLIENT_HOST}-${YEAR}-${MONTH}-${DAY}-${HOUR}-${INTERVAL}.tsv.gz";
    private final Path logDirectory;
    private final Timestamp timestamp;
    private final int bufferSize;
    private final LoadingCache<LogId, Writer> writers;
    private final Name writersMetric;
    public long requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
    private boolean closed;

    public DiskLoggerBackend( Path logDirectory, Timestamp timestamp, int bufferSize, LogConfiguration logConfiguration ) {
        this.logDirectory = logDirectory;
        this.timestamp = timestamp;
        this.bufferSize = bufferSize;
        this.writers = CacheBuilder.newBuilder()
            .expireAfterAccess( 60 / timestamp.bucketsPerHour * 3, TimeUnit.MINUTES )
            .removalListener( notification -> Closeables.close( ( Writer ) notification.getValue() ) )
            .build( new CacheLoader<>() {
                @Override
                public Writer load( LogId id ) {
                    return new Writer( logDirectory, filePattern, id, bufferSize, timestamp, logConfiguration );
                }
            } );
        this.writersMetric = Metrics.measureGauge(
            Metrics.name( METRICS_LOGGING_DISK + logDirectory.toString().replace( "/", "." ) + ".writers" ),
            writers::size );

    }

    @Override
    @SneakyThrows
    public void log( String hostName, String fileName, String logType, int shard, int version, byte[] buffer, int offset, int length ) {
        if( closed ) {
            var exception = new LoggerException( "already closed!" );
            listeners.fireError( exception );
            throw exception;
        }

        Metrics.measureCounterIncrement( Metrics.name( METRICS_LOGGING_DISK ).tag( "from", hostName ) );
        Metrics2.measureHistogram( Metrics.name( METRICS_LOGGING_DISK_BUFFERS ).tag( "from", hostName ), length );
        Writer writer = writers.get( new LogId( fileName, logType, hostName, shard, version ) );
        log.trace( "logging {} bytes to {}", length, writer );
        writer.write( buffer, offset, length, this.listeners::fireError );
    }

    @Override
    public void close() {
        if( !closed ) {
            closed = true;
            Metrics.unregister( writersMetric );
            writers.invalidateAll();
        }
    }

    @Override
    public AvailabilityReport availabilityReport() {
        boolean enoughSpace = Files.usableSpaceAtDirectory( logDirectory ) > requiredFreeSpace;
        return new AvailabilityReport( enoughSpace ? OPERATIONAL : FAILED );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper( this )
            .add( "path", logDirectory )
            .add( "filePattern", filePattern )
            .add( "buffer", bufferSize )
            .add( "bucketsPerHour", timestamp.bucketsPerHour )
            .add( "writers", writers.size() )
            .toString();
    }
}
