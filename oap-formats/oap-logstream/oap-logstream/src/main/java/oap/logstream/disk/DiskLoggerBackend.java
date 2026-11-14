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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.concurrent.Executors;
import oap.concurrent.scheduler.ScheduledExecutorService;
import oap.google.JodaTicker;
import oap.io.Closeables;
import oap.io.Files;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.template.TemplateEngine;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Result;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.AvailabilityReport.State.OPERATIONAL;

/**
 * replica ENV:
 * <li>POD_NAME</li>
 */
@Slf4j
public class DiskLoggerBackend extends AbstractLoggerBackend implements FileWriterNotification, Cloneable, AutoCloseable {
    public static final int DEFAULT_BUFFER = 1024 * 100;
    public static final long DEFAULT_FREE_SPACE_REQUIRED = 2000000000L;
    public final LinkedHashMap<String, FilePatternConfiguration> filePatternByType = new LinkedHashMap<>();
    public final WriterConfiguration writerConfiguration;
    public final Path logDirectory;
    public final Timestamp timestamp;
    public final int bufferSize;
    public final LoadingCache<WriterKey, AbstractWriter> writers;
    public final ScheduledExecutorService pool;
    public final String hostname;
    protected final TemplateEngine templateEngine;
    public String filePattern = "{{ YEAR }}-{{ MONTH }}/{{ DAY }}/{{ LOG_TYPE }}_v{{ LOG_VERSION }}_{{ CLIENT_HOST }}-{{ YEAR }}-{{ MONTH }}-{{ DAY }}-{{ HOUR }}-{{ INTERVAL }}.tsv.gz";
    public long requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
    public int maxVersions = 20;
    public long refreshInitDelay = Dates.s( 10 );
    public long refreshPeriod = Dates.s( 10 );
    public long usableSpaceCheckPeriod = Dates.s( 1 );
    /**
     * when the written data is synced to the device, see {@link SyncMode}
     */
    public SyncMode syncMode = SyncMode.ALWAYS;
    /**
     * {@link SyncMode#PERIODIC} sync period
     */
    public long syncInterval = Dates.s( 1 );
    /**
     * repair the files left without .metadata.completed by a crash on start, see {@link LogFile#recover()}
     */
    public boolean recoverOnStart = true;
    public volatile boolean closed;
    private volatile UsableSpace usableSpace;
    private final ConcurrentHashMap<String, HostMetrics> hostMetrics = new ConcurrentHashMap<>();
    protected CopyOnWriteArrayList<FileWriterNotification> notifications = new CopyOnWriteArrayList<>();

    public DiskLoggerBackend( TemplateEngine templateEngine, Path logDirectory, Timestamp timestamp, int bufferSize, String hostname ) {
        this( templateEngine, logDirectory, new WriterConfiguration(), timestamp, bufferSize, hostname );
    }

    @SuppressWarnings( "unchecked" )
    public DiskLoggerBackend( TemplateEngine templateEngine, Path logDirectory, WriterConfiguration writerConfiguration, Timestamp timestamp, int bufferSize, String hostname ) {
        this.templateEngine = templateEngine;
        this.hostname = hostname;

        log.info( "logDirectory '{}' timestamp {} bufferSize {} writerConfiguration {} refreshInitDelay {} refreshPeriod {} hostname {}",
            logDirectory, timestamp, FileUtils.byteCountToDisplaySize( bufferSize ), writerConfiguration,
            Dates.durationToString( refreshInitDelay ), Dates.durationToString( refreshPeriod ), hostname );


        this.logDirectory = logDirectory.resolve( hostname );
        this.writerConfiguration = writerConfiguration;
        this.timestamp = timestamp;
        this.bufferSize = bufferSize;

        this.writers = CacheBuilder.newBuilder()
            .ticker( JodaTicker.JODA_TICKER )
            .expireAfterAccess( 60 / timestamp.bucketsPerHour * 3, TimeUnit.MINUTES )
            .removalListener( l -> {
                Closeables.close( ( Closeable ) l.getValue() );
            } )
            .build( new CacheLoader<>() {
                @Override
                public AbstractWriter load( WriterKey key ) {
                    LogId id = key.id();
                    FilePatternConfiguration fp = filePatternByType.getOrDefault( id.logType.toUpperCase(), new FilePatternConfiguration( filePattern ) );

                    log.trace( "new writer id '{}' compression {} filePattern '{}'", id, key.compression(), fp );

                    return new RowBinaryWriter( templateEngine, key.compression(), syncMode, DiskLoggerBackend.this.logDirectory, fp.path, id, bufferSize, timestamp, maxVersions, hostname, DiskLoggerBackend.this );
                }
            } );
        Metrics.gauge( "logstream_logging_disk_writers", List.of( Tag.of( "path", this.logDirectory.toString() ) ),
            writers, Cache::size );

        pool = Executors.newScheduledThreadPool( 1, "disk-logger-backend" );
    }

    public void start() {
        log.info( "default file pattern {}", filePattern );
        log.info( "file patterns by type {}", filePatternByType );
        log.info( "refreshInitDelay {} refreshPeriod {}", Dates.durationToString( refreshInitDelay ), Dates.durationToString( refreshPeriod ) );
        log.info( "syncMode {} syncInterval {} recoverOnStart {}", syncMode, Dates.durationToString( syncInterval ), recoverOnStart );

        filePatternValidation( "*", filePattern );
        filePatternByType.forEach( ( k, v ) -> filePatternValidation( k, v.path ) );

        filePatternByType.keySet().forEach( key -> Preconditions.checkArgument( key.equals( key.toUpperCase() ), key + " must be uppercase" ) );

        if( recoverOnStart ) recover();

        pool.scheduleWithFixedDelay( () -> refresh( false ), refreshInitDelay, refreshPeriod, MILLISECONDS );
        if( syncMode == SyncMode.PERIODIC ) {
            pool.scheduleWithFixedDelay( this::sync, syncInterval, syncInterval, MILLISECONDS );
        }
    }

    /**
     * Repairs the files under logDirectory left without .metadata.completed (crash), including the files of the previous
     * versions with .metadata.transaction, see {@link LogFile#recover()}. Files open by the writers are not touched.
     * Recovered files are reported to the {@link FileWriterNotification} listeners registered at that moment.
     */
    public void recover() {
        if( !java.nio.file.Files.isDirectory( logDirectory ) ) return;

        Set<Path> open = new HashSet<>();
        for( AbstractWriter writer : writers.asMap().values() ) {
            Path file = writer.currentFile();
            if( file != null ) open.add( file );
        }

        Set<Path> candidates = new LinkedHashSet<>();
        try( Stream<Path> files = java.nio.file.Files.walk( logDirectory ) ) {
            files.filter( java.nio.file.Files::isRegularFile ).forEach( path -> {
                String name = path.getFileName().toString();
                if( name.endsWith( ".tmp" ) ) return;
                if( name.endsWith( LogFile.EXTENSION_LOG_METADATA ) || name.endsWith( LogFile.EXTENSION_LOG_TRANSACTION )
                    || name.endsWith( LogFile.EXTENSION_LOG_COMPLETED ) || LogFile.compressionOf( path ).isPresent() ) {
                    candidates.add( LogFile.loadFromPath( path ).outFilename );
                }
            } );
        } catch( IOException | UncheckedIOException e ) {
            log.error( "recovery: cannot list {}", logDirectory, e );
            return;
        }

        int recovered = 0;
        int deleted = 0;
        for( Path candidate : candidates ) {
            if( open.contains( candidate ) ) continue;

            LogFile logFile = new LogFile( candidate );
            try {
                if( logFile.isCompleted() ) {
                    if( logFile.isLegacy() && java.nio.file.Files.exists( candidate ) ) {
                        OptionalLong committed = logFile.legacyCommittedPosition();
                        long size = logFile.getDataSize();
                        if( committed.isEmpty() || committed.getAsLong() != size ) {
                            log.warn( "[{}] completed legacy file: committed offset {} != size {}", candidate,
                                committed.isPresent() ? committed.getAsLong() : "<none>", size );
                        }
                    }
                    continue;
                }

                switch( logFile.recover() ) {
                    case RECOVERED -> {
                        recovered++;
                        fileClosed( candidate );
                    }
                    case DELETED -> deleted++;
                    case SKIPPED -> { }
                }
            } catch( Exception e ) {
                log.error( "[{}] recovery failed", candidate, e );
            }
        }

        if( recovered > 0 || deleted > 0 ) log.info( "recovery {}: {} recovered, {} deleted", logDirectory, recovered, deleted );
    }

    /**
     * fdatasync of the open files ({@link SyncMode#PERIODIC})
     */
    public void sync() {
        for( AbstractWriter writer : writers.asMap().values() ) {
            try {
                writer.sync();
            } catch( Exception e ) {
                log.error( "Cannot sync {}", writer, e );
            }
        }
    }

    private void filePatternValidation( String type, String filePattern ) {
        LogId logId = new LogId( "", type, "", Map.of(), new String[] {}, new byte[][] {} );

        DateTime time = Dates.nowUtc();
        String currentPattern = AbstractWriter.currentPattern( templateEngine, LogFormat.TSV_GZ, filePattern, logId, timestamp, 0, time, hostname );
        String previousPattern = AbstractWriter.currentPattern( templateEngine, LogFormat.TSV_GZ, filePattern, logId, timestamp, 0, time.minusMinutes( 60 / timestamp.bucketsPerHour ).minusSeconds( 1 ), hostname );

        if( currentPattern.equals( previousPattern ) ) {
            log.error( "cp {}", currentPattern );
            log.error( "pp {}", previousPattern );
            throw new IllegalArgumentException( "filepattern(" + type + ") must contain a variable ${INTERVAL} or ${MINUTE}" );
        }
    }

    /**
     * buffer - gzip
     */
    @Override
    public Result<String, LogstreamException> log( ProtocolVersion protocolVersion, String hostName, String filePreffix, Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        return log( protocolVersion, LogStreamCompression.GZIP, hostName, filePreffix, properties, logType, headers, types, buffer, offset, length );
    }

    /**
     * buffer - compressed by compression, appended to the file of the corresponding format as is
     */
    @Override
    @SneakyThrows
    public Result<String, LogstreamException> log( ProtocolVersion protocolVersion, LogStreamCompression compression, String hostName, String filePreffix,
                                                   Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        if( closed ) {
            throw new LoggerException( "already closed!" );
        }

        HostMetrics metrics = hostMetrics( hostName );
        metrics.counter.increment();
        metrics.buffers.record( length );
        LogId logId = new LogId( filePreffix, logType, hostName, properties, headers, types );
        AbstractWriter writer = writers.get( new WriterKey( logId, compression ) );

        log.trace( "logging {} bytes to {}", length, writer );
        try {
            return Result.success( writer.write( protocolVersion, buffer, offset, length ) );
        } catch( Exception e ) {
            var headersWithTypes = new ArrayList<String>();
            for( int i = 0; i < headers.length; i++ ) {
                headersWithTypes.add( headers[i] + " [" + Lists.map( List.of( ArrayUtils.toObject( types[i] ) ), oap.template.Types::valueOf ) + "]" );
            }

            log.error( "hostName {} filePrefix {} logType {} properties {} headers {} path {}",
                hostName, filePreffix, logType, properties, headersWithTypes, writer.currentPattern() );

            throw e;
        }
    }

    private HostMetrics hostMetrics( String hostName ) {
        HostMetrics metrics = hostMetrics.get( hostName );
        if( metrics != null ) {
            return metrics;
        }

        return hostMetrics.computeIfAbsent( hostName, h -> new HostMetrics(
            Metrics.counter( "logstream_logging_disk_counter", List.of( Tag.of( "from", h ) ) ),
            Metrics.summary( "logstream_logging_disk_buffers", List.of( Tag.of( "from", h ) ) ) ) );
    }

    @Override
    public void close() {
        if( !closed ) {
            closed = true;
            pool.shutdown( 20, SECONDS );
            Closeables.close( pool );
            writers.invalidateAll();
        }
    }

    @Override
    public AvailabilityReport availabilityReport() {
        long usableSpaceAtDirectory = usableSpace();
        boolean enoughSpace = usableSpaceAtDirectory > requiredFreeSpace;
        if( !enoughSpace ) {
            LogConsolidated.log( log, Level.ERROR, Dates.s( 30 ),
                "There is no enough space on device %s, required %s, but %s available".formatted( logDirectory.toString(), Files.byteCountToDisplaySize( requiredFreeSpace ), Files.byteCountToDisplaySize( usableSpaceAtDirectory ) ),
                null );
        }
        return new AvailabilityReport( enoughSpace ? OPERATIONAL : FAILED );
    }

    /**
     * statfs result cached for usableSpaceCheckPeriod (joda time, so tests can control it)
     */
    private long usableSpace() {
        long now = DateTimeUtils.currentTimeMillis();
        UsableSpace cached = usableSpace;
        if( cached != null && now >= cached.checkedAt && now - cached.checkedAt < usableSpaceCheckPeriod ) {
            return cached.bytes;
        }

        long bytes = Files.usableSpaceAtDirectory( logDirectory );
        usableSpace = new UsableSpace( now, bytes );
        return bytes;
    }

    public void refresh() {
        refresh( false );
    }

    public void refresh( boolean forceSync ) {
        log.trace( "refresh forceSync {}", forceSync );

        for( AbstractWriter writer : writers.asMap().values() ) {
            try {
                writer.refresh( forceSync );
            } catch( Exception e ) {
                log.error( "Cannot refresh ", e );
            }
        }

        writers.cleanUp();

        log.trace( "refresh forceSync {}... Done", forceSync );
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

    @Override
    public void fileClosed( Path outFilename ) {
        for( FileWriterNotification notification : notifications ) {
            notification.fileClosed( outFilename );
        }
    }

    public void addNotificationListener( FileWriterNotification notification ) {
        this.notifications.add( notification );
    }

    @ToString
    @EqualsAndHashCode
    public static class FilePatternConfiguration {
        public final String path;

        @JsonCreator
        public FilePatternConfiguration( String path ) {
            this.path = path;
        }
    }

    /**
     * the payload codec defines the file format, so a log id can have several files (one per codec) at the same time
     */
    public record WriterKey( LogId id, LogStreamCompression compression ) {}

    private record UsableSpace( long checkedAt, long bytes ) {}

    private record HostMetrics( Counter counter, DistributionSummary buffers ) {}
}
