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
import oap.io.Closeables;
import oap.io.Files;
import oap.logstream.AvailabilityReport;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;
import oap.metrics.Name;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.AvailabilityReport.State.OPERATIONAL;

@Slf4j
public class DiskLoggingBackend implements LoggingBackend {
    public static final int DEFAULT_BUFFER = 1024 * 100;
    public static final String METRICS_LOGGING_DISK = "logging.disk";
    public static final String METRICS_LOGGING_DISK_BUFFERS = "logging.disk.buffers";
    public static final long DEFAULT_FREE_SPACE_REQUIRED = 2000000000L;
    private final Path logDirectory;
    private final String ext;
    private final int bufferSize;
    private final LoadingCache<String, Writer> writers;
    private final int bucketsPerHour;
    private final Name writersMetric;
    private boolean closed;
    public long requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
    public boolean useClientHostPrefix = true;

    public DiskLoggingBackend( Path logDirectory, String ext, int bufferSize, int bucketsPerHour ) {
        this.logDirectory = logDirectory;
        this.ext = ext;
        this.bufferSize = bufferSize;
        this.bucketsPerHour = bucketsPerHour;
        this.writers = CacheBuilder.newBuilder()
            .expireAfterAccess( 60 / bucketsPerHour * 3, TimeUnit.MINUTES )
            .removalListener( notification -> Closeables.close( ( Writer ) notification.getValue() ) )
            .build( new CacheLoader<String, Writer>() {
                @Override
                public Writer load( String fullFileName ) throws Exception {
                    return new Writer( logDirectory, fullFileName, ext, bufferSize, bucketsPerHour );
                }
            } );
        this.writersMetric = Metrics.measureGauge(
            Metrics.name( METRICS_LOGGING_DISK + logDirectory.toString().replace( "/", "." ) + ".writers" ),
            writers::size );

    }

    @Override
    @SneakyThrows
    public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {
        if( closed ) throw new IOException( "already closed!" );

        Metrics.measureCounterIncrement( Metrics.name( METRICS_LOGGING_DISK ).tag( "from", hostName ) );
        Metrics.measureHistogram( Metrics.name( METRICS_LOGGING_DISK_BUFFERS ).tag( "from", hostName ), length );
        String fullFileName = useClientHostPrefix ? hostName + "/" + fileName : fileName;
        Writer writer = writers.get( fullFileName );
        log.trace( "logging {} bytes to {}", length, writer );
        writer.write( buffer, offset, length );
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
            .add( "ext", ext )
            .add( "buffer", bufferSize )
            .add( "bucketsPerHour", bucketsPerHour )
            .add( "hostPrefix", useClientHostPrefix )
            .add( "writers", writers.size() )
            .toString();
    }
}
