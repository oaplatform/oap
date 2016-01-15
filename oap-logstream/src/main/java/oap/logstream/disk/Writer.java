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
import oap.concurrent.Stopwatch;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.IoStreams;
import oap.logstream.Filename;
import oap.metrics.Metrics;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

public class Writer implements Closeable {
    private static Logger logger = getLogger( Writer.class );
    private final String ext;
    private final Path logDirectory;
    private final String root;
    private int bufferSize;
    private int bucketsPerHour;
    private boolean compress;
    private CountingOutputStream out;
    private String lastPattern;
    private Scheduled scheduled;
    private Stopwatch stopwatch = new Stopwatch();

    public Writer( Path logDirectory, String root, String ext, int bufferSize, int bucketsPerHour, boolean compress ) {
        this.logDirectory = logDirectory;
        this.root = root;
        this.ext = ext;
        this.bufferSize = bufferSize;
        this.bucketsPerHour = bucketsPerHour;
        this.compress = compress;
        this.lastPattern = currentPattern();
        this.scheduled = Scheduler.scheduleWithFixedDelay( 30, SECONDS, this::fsync );
    }

    private void fsync() {
        flush();
        refresh();
    }


    @Override
    public void close() throws IOException {
        logger.debug( "closing " + this );
        Scheduled.cancel( scheduled );
        closeOutput();
    }

    private void closeOutput() throws IOException {
        if( out != null ) {
            stopwatch.measure( out::flush );
            stopwatch.measure( out::close );
            Metrics.measureHistogram( "logger_server_bucket_size", out.getCount() );
            Metrics.measureHistogram( "logger_server_bucket_time", stopwatch.elapsed() / 1000000L );
            out = null;
        }
    }

    public synchronized void write( byte[] buffer ) {
        write( buffer, 0, buffer.length );
    }

    public synchronized void write( byte[] buffer, int offset, int length ) {
        try {
            refresh();
            if( out == null )
                out = new CountingOutputStream(
                    IoStreams.out( filename(), compress ? GZIP : PLAIN, bufferSize, true )
                );
            out.write( buffer, offset, length );

        } catch( IOException e ) {
            logger.error( e.getMessage(), e );
            try {
                closeOutput();
            } catch( IOException e1 ) {
                logger.error( e.getMessage(), e );
            } finally {
                out = null;
            }
        }
    }

    private Path filename() {
        return logDirectory.resolve( Filename.directoryName( lastPattern ) )
            .resolve( root + "-" + lastPattern + "." + ext );
    }

    private synchronized void refresh() {
        String currentPattern = currentPattern();
        if( !Objects.equals( this.lastPattern, currentPattern ) ) try {
            lastPattern = currentPattern;
            closeOutput();
        } catch( IOException e ) {
            logger.error( e.getMessage(), e );
        }
    }

    private String currentPattern() {
        return Filename.formatDate( DateTime.now(), bucketsPerHour );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + filename();
    }

    public synchronized void flush() {
        try {
            if( out != null ) out.flush();
        } catch( IOException e ) {
            logger.error( e.getMessage(), e );
        }
    }

}
