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
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.logstream.Filename;
import oap.metrics.Metrics;
import oap.util.Try;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LogWriter implements Closeable {
    private final String ext;
    private final Path logDirectory;
    private final String root;
    private final boolean compress;
    private int flushInterval = 30000;
    private int bufferSize;
    private int bucketsPerHour;
    private String lastPattern;
    private Scheduled scheduled;
    private Out out;

    public LogWriter( Path logDirectory, String root, String ext, int bufferSize, int bucketsPerHour, boolean compress ) {
        this.logDirectory = logDirectory;
        this.root = root;
        this.ext = ext;
        this.bufferSize = bufferSize;
        this.bucketsPerHour = bucketsPerHour;
        this.compress = compress;
        this.lastPattern = currentPattern();
        this.scheduled = Scheduler.scheduleWithFixedDelay( flushInterval, TimeUnit.MILLISECONDS, this::fsync );
    }

    private void fsync() {
        flush();
        refresh();
    }


    @Override
    public void close() throws IOException {
        log.debug( "closing {}", this );
        if( flushInterval > 0 ) Scheduled.cancel( scheduled );
        closeOutput();
    }

    private void closeOutput() throws IOException {
        if( out != null ) {
            out.close();
            out = null;
        }
    }

    public synchronized void write( byte[] buffer ) {
        write( buffer, 0, buffer.length );
    }

    public synchronized void write( byte[] buffer, int offset, int length ) {
        try {
            refresh();
            if( out == null ) out = new Out();

            out.write( buffer, offset, length );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            try {
                closeOutput();
            } catch( IOException e1 ) {
                log.error( e.getMessage(), e );
            } finally {
                out = null;
            }
        }
    }

    private synchronized void refresh() {
        String currentPattern = currentPattern();
        if( !Objects.equals( this.lastPattern, currentPattern ) ) try {
            lastPattern = currentPattern;
            closeOutput();
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }

    private String currentPattern() {
        return Filename.formatDate( DateTime.now(), bucketsPerHour );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + out;
    }

    public synchronized void flush() {
        if( out != null ) out.flush();
    }

    private class Out {
        public final CountingOutputStream out;
        public final Path outTmpName;
        public final Path outName;

        private long outTime = 0;

        public Out() {
            outTmpName = filename( true );
            outName = filename( false );

            out = new CountingOutputStream(
                IoStreams.out( outTmpName,
                    compress ? IoStreams.Encoding.GZIP : IoStreams.Encoding.PLAIN,
                    bufferSize, true )
            );
        }

        private Path filename( boolean temp ) {
            return logDirectory.resolve( Filename.directoryName( lastPattern ) )
                .resolve( root + "-" + lastPattern + "." + ext + ( temp && compress ? ".tmp" : "" ) );
        }

        public final void close() {
            try {
                Metrics.measureHistogram( "logger_server_size_from", out.getCount() );

                flush();
                measureTime( out::close );

                if( compress ) {
                    measureTime( () -> Files.rename( outTmpName, outName ) );
                }

                Metrics.measureHistogram( "logger_server_time", outTime / 1000000L );
                Metrics.measureHistogram( "logger_server_size_to", outName.toFile().length() );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            }
        }

        public final void write( byte[] buffer, int offset, int length ) throws IOException {
            measureTime( () -> out.write( buffer, offset, length ) );
        }

        @Override
        public final String toString() {
            return outTmpName.toString();
        }

        public final void flush() {
            try {
                measureTime( out::flush );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            }
        }

        private void measureTime( Try.ThrowingRunnable2<IOException> run ) throws IOException {
            final long time = System.nanoTime();
            try {
                run.run();
            } finally {
                outTime += ( System.nanoTime() - time );
            }
        }
    }
}
