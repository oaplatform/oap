/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.logstream;

import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class LogWriter implements Closeable {
    private static Logger logger = getLogger( LogWriter.class );
    private final String suffix;
    private final String prefix;
    private int bufferSize;
    private int interval;
    private OutputStream out;
    private String lastPattern;
    private Scheduled scheduled;

    public LogWriter( String prefix, String suffix, int bufferSize, int interval ) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.bufferSize = bufferSize;
        this.interval = interval;
        this.lastPattern = currentPattern();
        this.scheduled = Scheduler.scheduleWithFixedDelay( 1, TimeUnit.MINUTES, this::refresh );
    }


    @Override
    public void close() throws IOException {
        logger.trace( "closing writer " + prefix );
        Scheduled.cancel( scheduled );
        closeOutput();
    }

    private void closeOutput() throws IOException {
        if( out != null ) {
            out.flush();
            out.close();
            out = null;
        }
    }

    public synchronized void write( byte[] buffer ) {
        try {
            refresh();
            if( out == null ) out = IoStreams.out( filename(), IoStreams.Encoding.PLAIN, bufferSize, true );
            out.write( buffer );

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
        return Files.path( prefix + "-" + lastPattern + "." + suffix );
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
        return Filename.formatDate( DateTime.now(), interval );
    }

    @Override
    public String toString() {
        return "LogWriter@" + filename();
    }

    public void flush() {
        try {
            if( out != null ) out.flush();
        } catch( IOException e ) {
            logger.error( e.getMessage(), e );
        }
    }

}
