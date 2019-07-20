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

import com.google.common.io.CountingOutputStream;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Stopwatch;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.dictionary.Dictionary;
import oap.dictionary.LogConfiguration;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.logstream.LogId;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.metrics.Metrics2;
import oap.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

@Slf4j
public class Writer implements Closeable {
    public static final String LOG_TAG = "LOG";
    private final Path logDirectory;
    private final String filePattern;
    private final LogId logId;
    private final Timestamp timestamp;
    private final LogConfiguration logConfiguration;
    private int bufferSize;
    private CountingOutputStream out;
    private String lastPattern;
    private Scheduled refresher;
    private Stopwatch stopwatch = new Stopwatch();

    public Writer( Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp, LogConfiguration logConfiguration ) {
        this.logDirectory = logDirectory;
        this.filePattern = filePattern;
        this.logId = logId;
        this.bufferSize = bufferSize;
        this.timestamp = timestamp;
        this.logConfiguration = logConfiguration;
        this.lastPattern = currentPattern();
        this.refresher = Scheduler.scheduleWithFixedDelay( 10, SECONDS, this::refresh );
        log.debug( "spawning {}", this );
    }


    @Override
    public void close() {
        log.debug( "closing {}", this );
        Scheduled.cancel( refresher );
        closeOutput();
    }

    private void closeOutput() throws LoggerException {
        if( out != null ) try {
            log.trace( "closing output {} ({} bytes)", this, out.getCount() );
            stopwatch.measure( out::flush );
            stopwatch.measure( out::close );
            Metrics2.measureHistogram( "logging.server_bucket_size", out.getCount() );
            Metrics2.measureHistogram( "logging.server_bucket_time", stopwatch.elapsed() / 1000000L );
            out = null;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public synchronized void write( byte[] buffer, Consumer<String> error ) throws LoggerException {
        write( buffer, 0, buffer.length, error );
    }

    public synchronized void write( byte[] buffer, int offset, int length, Consumer<String> error ) throws LoggerException {
        try {
            refresh();
            Path filename = filename();
            if( out == null ) {
                boolean exists = Files.exists( filename );

                if( !exists ) {
                    out = new CountingOutputStream( IoStreams.out( filename, Encoding.from( filename ), bufferSize ) );
                    var headers = getHeaders( logId.logType, logId.version );
                    out.write( headers.getBytes() );
                    log.debug( "[{}] write headers {}", filename, headers );
                } else {
                    log.trace( "[{}] file exists", filename );

                    if( Files.isFileEncodingValid( filename ) )
                        out = new CountingOutputStream( IoStreams.out( filename, Encoding.from( filename ), bufferSize, true ) );
                    else {
                        error.accept( "corrupted file, cannot append " + filename );
                        log.error( "corrupted file, cannot append {}", filename );
                        Files.rename( filename, logDirectory.resolve( ".corrupted" )
                            .resolve( logDirectory.relativize( filename ) ) );
                        out = new CountingOutputStream( IoStreams.out( filename, Encoding.from( filename ), bufferSize ) );
                    }
                }
            }
            log.trace( "writing {} bytes to {}", length, this );
            out.write( buffer, offset, length );

        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            try {
                closeOutput();
            } finally {
                out = null;
            }
            throw new LoggerException( e );
        }
    }

    private String getHeaders( String logType, int version ) {
        var versionDictionary = logConfiguration.getDictionary( version );
        var logTypeDictionary = versionDictionary.getValue( logType.toUpperCase() );
        if( logTypeDictionary == null ) {
            throw new LoggerException( "Unknown log type " + logType.toUpperCase() );
        }

        var header = logTypeDictionary
            .getValues( d -> d.getTags().contains( LOG_TAG ) )
            .stream()
            .filter( field -> field.containsProperty( "path" ) )
            .map( Dictionary::getId )
            .collect( joining( "\t" ) );
        return Strings.isEmpty( header ) ? header : header + '\n';
    }

    private Path filename() {
        return logDirectory.resolve( lastPattern );
    }

    private synchronized void refresh() {
        String currentPattern = currentPattern();
        if( !Objects.equals( this.lastPattern, currentPattern ) ) {
            log.trace( "change pattern from '{}' to '{}'", this.lastPattern, currentPattern );
            closeOutput();
            lastPattern = currentPattern;
        }
    }

    private String currentPattern() {
        return logId.fileName( filePattern, new DateTime( DateTimeZone.UTC ), timestamp );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + filename();
    }
}
