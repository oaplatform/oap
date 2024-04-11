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
import oap.concurrent.Stopwatch;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.util.Dates;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractWriter<T extends Closeable> implements Closeable {
    protected final Path logDirectory;
    protected final String filePattern;
    protected final LogId logId;
    protected final Timestamp timestamp;
    protected final int bufferSize;
    protected final Stopwatch stopwatch = new Stopwatch();
    protected final int maxVersions;
    protected T out;
    protected Path outFilename;
    protected String lastPattern;
    protected int fileVersion = 1;
    protected boolean closed = false;
    public final LogFormat logFormat;

    protected AbstractWriter( LogFormat logFormat, Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp,
                              int maxVersions ) {
        this.logFormat = logFormat;
        this.logDirectory = logDirectory;
        this.filePattern = filePattern;
        this.maxVersions = maxVersions;

        log.trace( "filePattern {}", filePattern );
        Preconditions.checkArgument( filePattern.contains( "<LOG_VERSION>" ) );

        this.logId = logId;
        this.bufferSize = bufferSize;
        this.timestamp = timestamp;
        this.lastPattern = currentPattern();
        log.debug( "spawning {}", this );
    }

    public synchronized void write( ProtocolVersion protocolVersion, byte[] buffer, Consumer<String> error ) throws LoggerException {
        write( protocolVersion, buffer, 0, buffer.length, error );
    }

    public abstract void write( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length, Consumer<String> error ) throws LoggerException;

    protected String currentPattern() {
        return currentPattern( logFormat, filePattern, logId, timestamp, fileVersion, Dates.nowUtc() );
    }

    protected String currentPattern( int version ) {
        return currentPattern( logFormat, filePattern, logId, timestamp, version, Dates.nowUtc() );
    }

    @SneakyThrows
    static String currentPattern( LogFormat logFormat, String filePattern, LogId logId, Timestamp timestamp, int version, DateTime time ) {
        var suffix = filePattern;
        if( filePattern.startsWith( "/" ) && filePattern.endsWith( "/" ) ) suffix = suffix.substring( 1 );
        else if( !filePattern.startsWith( "/" ) && !logId.filePrefixPattern.endsWith( "/" ) ) suffix = "/" + suffix;

        var pattern = logId.filePrefixPattern + suffix;
        if( pattern.startsWith( "/" ) ) pattern = pattern.substring( 1 );

        pattern = StringUtils.replace( pattern, "${", "<" );
        pattern = StringUtils.replace( pattern, "}", ">" );

        LogIdTemplate logIdTemplate = new LogIdTemplate( logId );

        logIdTemplate
            .addVariable( "LOG_FORMAT", logFormat.extension )
            .addVariable( "LOG_FORMAT_" + logFormat.name(), logFormat.extension );
        return logIdTemplate.render( StringUtils.replace( pattern, " ", "" ), time, timestamp, version );
    }

    public synchronized void refresh() {
        refresh( false );
    }

    public synchronized void refresh( boolean forceSync ) {
        log.debug( "refresh {}...", lastPattern );

        var currentPattern = currentPattern();

        if( forceSync || !Objects.equals( this.lastPattern, currentPattern ) ) {
            log.debug( "lastPattern {} currentPattern {} version {}", lastPattern, currentPattern, fileVersion );

            var patternWithPreviousVersion = currentPattern( fileVersion - 1 );
            if( !Objects.equals( patternWithPreviousVersion, this.lastPattern ) ) {
                fileVersion = 1;
            }
            currentPattern = currentPattern();

            log.debug( "force {} change pattern from '{}' to '{}'", forceSync, this.lastPattern, currentPattern );
            closeOutput();

            lastPattern = currentPattern;
        } else {
            log.debug( "refresh {}... SKIP", lastPattern );
        }
    }

    protected Path filename() {
        return logDirectory.resolve( lastPattern );
    }

    protected void closeOutput() throws LoggerException {
        if( out != null ) try {
            stopwatch.count( out::close );

            var fileSize = Files.size( outFilename );
            log.trace( "closing output {} ({} bytes)", this, fileSize );
            Metrics.summary( "logstream_logging_server_bucket_size" ).record( fileSize );
            Metrics.summary( "logstream_logging_server_bucket_time_seconds" ).record( Dates.nanosToSeconds( stopwatch.elapsed() ) );
        } catch( IOException e ) {
            throw new LoggerException( e );
        } finally {
            outFilename = null;
            out = null;
        }
    }

    @Override
    public synchronized void close() {
        log.debug( "closing {}", this );
        closed = true;
        closeOutput();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + filename();
    }
}
