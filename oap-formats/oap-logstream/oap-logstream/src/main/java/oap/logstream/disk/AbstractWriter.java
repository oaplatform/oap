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

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.template.TemplateEngine;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractWriter implements Closeable {
    public final LogFormat logFormat;
    protected final TemplateEngine templateEngine;
    protected final Path logDirectory;
    protected final String filePattern;
    protected final LogId logId;
    protected final Timestamp timestamp;
    protected final int bufferSize;
    protected final int maxVersions;
    protected final String hostname;
    protected final FileWriterNotification notification;
    /**
     * shard index of the (LogId, codec) key; shard k &gt; 0 adds {@code -s<k>} before the codec extension
     */
    protected final int shard;
    protected final ReentrantLock lock = new ReentrantLock();
    protected LogFile logFile;
    protected String lastPattern;
    protected int fileVersion = 1;
    protected boolean closed = false;
    private volatile PatternCache patternCache;

    protected AbstractWriter( TemplateEngine templateEngine, LogFormat logFormat, Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp,
                              int maxVersions, String hostname, FileWriterNotification notification ) {
        this( templateEngine, logFormat, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification, 0 );
    }

    protected AbstractWriter( TemplateEngine templateEngine, LogFormat logFormat, Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp,
                              int maxVersions, String hostname, FileWriterNotification notification, int shard ) {
        Preconditions.checkArgument( shard >= 0, "shard must not be negative: %s", shard );
        this.shard = shard;
        this.templateEngine = templateEngine;
        this.logFormat = logFormat;
        this.logDirectory = logDirectory;
        this.filePattern = filePattern;
        this.maxVersions = maxVersions;
        this.hostname = hostname;
        this.notification = notification;

        log.trace( "filePattern {}", filePattern );
        Preconditions.checkArgument( filePattern.matches( ".*[${]\\{\\s*LOG_VERSION\\s*}}?.*" ), "file pattern must contains LOG_VERSION variable" );

        this.logId = logId;
        this.bufferSize = bufferSize;
        this.timestamp = timestamp;
        this.lastPattern = currentPattern();
        log.debug( "spawning {}", this );
    }

    static String currentPattern( TemplateEngine templateEngine, LogFormat logFormat, String filePattern, LogId logId, Timestamp timestamp, int version, DateTime time, String hostname ) {
        return currentPattern( templateEngine, logFormat, filePattern, logId, timestamp, version, time, hostname, 0 );
    }

    /**
     * @param shard k &gt; 0 adds {@code -s<k>} before the codec extension; 0 - no suffix
     */
    @SneakyThrows
    static String currentPattern( TemplateEngine templateEngine, LogFormat logFormat, String filePattern, LogId logId, Timestamp timestamp, int version, DateTime time, String hostname, int shard ) {
        String pattern = filePattern;
        if( pattern.startsWith( "/" ) ) pattern = pattern.substring( 1 );

        // files are always RowBinary; for other formats logFormat is only a template variable (LOG_FORMAT)
        LogFormat fileFormat = switch( logFormat ) {
            case ROW_BINARY_LZ4, ROW_BINARY_ZSTD -> logFormat;
            case ROW_BINARY_GZ, TSV_GZ, TSV_ZSTD, PARQUET -> LogFormat.ROW_BINARY_GZ;
        };
        pattern = pattern + ( shard == 0 ? "" : "-s" + shard ) + "." + fileFormat.extension;

        LogIdTemplate logIdTemplate = new LogIdTemplate( logId );

        logIdTemplate
            .addVariable( "LOG_FORMAT", logFormat.extension )
            .addVariable( "LOG_FORMAT_" + logFormat.name(), logFormat.extension );
        return logIdTemplate.render( templateEngine, pattern, time, timestamp, version, hostname );
    }

    /**
     * File pattern variables have at most minute granularity, so the rendered pattern is cached per (epoch minute, version).
     */
    protected String currentPattern( int version ) {
        long now = DateTimeUtils.currentTimeMillis();
        long minute = now / 60_000L;
        PatternCache cache = patternCache;
        if( cache != null && cache.minute == minute && cache.version == version ) {
            return cache.pattern;
        }

        String pattern = currentPattern( templateEngine, logFormat, filePattern, logId, timestamp, version, new DateTime( now, DateTimeZone.UTC ), hostname, shard );
        patternCache = new PatternCache( minute, version, pattern );
        return pattern;
    }

    protected String currentPattern() {
        return currentPattern( fileVersion );
    }

    public void write( ProtocolVersion protocolVersion, byte[] buffer ) throws LoggerException {
        write( protocolVersion, buffer, 0, buffer.length );
    }

    public abstract String write( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException;

    public void refresh() {
        refresh( false );
    }

    public void refresh( boolean forceSync ) {
        lock.lock();
        try {
            if( logFile != null ) {
                log.debug( "refresh {}...", lastPattern );
            }

            String currentPattern = currentPattern();

            if( forceSync || !Objects.equals( this.lastPattern, currentPattern ) ) {
                log.trace( "lastPattern {} currentPattern {} version {}", lastPattern, currentPattern, fileVersion );

                String patternWithPreviousVersion = currentPattern( fileVersion - 1 );
                if( !Objects.equals( patternWithPreviousVersion, this.lastPattern ) ) {
                    fileVersion = 1;
                }
                currentPattern = currentPattern();

                log.debug( "force {} change pattern from '{}' to '{}'", forceSync, this.lastPattern, currentPattern );
                closeOutput();

                lastPattern = currentPattern;
            } else {
                log.trace( "refresh {}... SKIP", lastPattern );
            }
        } finally {
            lock.unlock();
        }
    }

    protected Path filename() {
        return logDirectory.resolve( lastPattern );
    }

    /**
     * the file open for writing, null - none
     */
    @Nullable
    public LogFile getLogFile() {
        lock.lock();
        try {
            return logFile;
        } finally {
            lock.unlock();
        }
    }

    /**
     * the file open for writing, null - none
     */
    public Path currentFile() {
        lock.lock();
        try {
            return logFile != null ? logFile.outFilename : null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * fdatasync of the data written without sync ({@link SyncMode#PERIODIC}, {@link SyncMode#ON_CLOSE}).
     * A failed sync fails the file: it is closed without {@code .metadata.completed} at once,
     * {@link FileWriterNotification#fileFailed(Path, Throwable)} is called, the next write goes to the next version.
     */
    public void sync() throws LoggerException {
        lock.lock();
        try {
            if( logFile != null ) try {
                logFile.sync();
            } catch( LoggerException e ) {
                if( logFile.isFailed() ) failOutput( e );
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * under lock: the sync of the current file failed - closes it without {@code .metadata.completed}
     * ({@link DiskLoggerBackend#recover()} repairs it on the next start) and notifies
     */
    protected void failOutput( Exception cause ) {
        LogFile failed = logFile;
        logFile = null;
        try {
            failed.closeQuietlyWithoutCompleted();
        } finally {
            fileFailed( failed.outFilename, cause );
        }
    }

    private void fileFailed( Path outFilename, Exception cause ) {
        log.error( "[{}] sync failed, the file is closed without completed, the next write goes to the next version: {}",
            outFilename, cause.getMessage(), cause );
        Metrics.counter( "logstream_logging_disk_sync_failures" ).increment();
        if( notification != null ) try {
            notification.fileFailed( outFilename, cause );
        } catch( Exception e ) {
            log.error( "[{}] fileFailed notification", outFilename, e );
        }
    }

    protected void closeOutput() throws LoggerException {
        lock.lock();
        try {
            if( logFile != null ) try {
                try {
                    logFile.close();
                } catch( LoggerException e ) {
                    // the sync on close failed: the file is closed without .completed
                    if( logFile.isFailed() ) fileFailed( logFile.outFilename, e );
                    throw e;
                }

                long fileSize = logFile.getDataSize();
                log.trace( "closing output {} ({} bytes)", this, fileSize );
                Metrics.summary( "logstream_logging_server_bucket_size" ).record( fileSize );

                logFile.readyForUpload();

                if( notification != null ) {
                    notification.fileClosed( logFile.outFilename );
                }
            } finally {
                logFile = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if( logFile != null ) {
                log.debug( "closing {}", this );
            }
            closed = true;
            closeOutput();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + filename();
    }

    private record PatternCache( long minute, int version, String pattern ) {}
}
