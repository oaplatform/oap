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
import oap.io.IoStreams;
import oap.logstream.Timestamp;
import oap.metrics.Metrics;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class Writer implements Closeable {
   private final String ext;
   private final Path logDirectory;
   private final String filename;
   private int bufferSize;
   private int bucketsPerHour;
   private boolean compress;
   private CountingOutputStream out;
   private String lastPattern;
   private Scheduled refresher;
   private Stopwatch stopwatch = new Stopwatch();

   public Writer( Path logDirectory, String filename, String ext, int bufferSize, int bucketsPerHour, boolean compress ) {
      this.logDirectory = logDirectory;
      this.filename = filename;
      this.ext = ext;
      this.bufferSize = bufferSize;
      this.bucketsPerHour = bucketsPerHour;
      this.compress = compress;
      this.lastPattern = currentPattern();
      this.refresher = Scheduler.scheduleWithFixedDelay( 10, SECONDS, this::refresh );
      log.debug( "spawning {}", this );
   }


   @Override
   public void close() throws IOException {
      log.debug( "closing {}", this );
      Scheduled.cancel( refresher );
      closeOutput();
   }

   private void closeOutput() throws IOException {
      if( out != null ) {
         log.trace( "closing output {} ({} bytes)", this, out.getCount() );
         stopwatch.measure( out::flush );
         stopwatch.measure( out::close );
         Metrics.measureHistogram( "logging.server_bucket_size", out.getCount() );
         Metrics.measureHistogram( "logging.server_bucket_time", stopwatch.elapsed() / 1000000L );
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
            out = new CountingOutputStream( IoStreams.out( filename(), compress ? GZIP : PLAIN, bufferSize, true ) );
         log.trace( "writing {} bytes to {}", length, this );
         out.write( buffer, offset, length );

      } catch( IOException e ) {
         log.error( e.getMessage(), e );
         try {
            closeOutput();
         } catch( IOException e1 ) {
            log.error( e1.getMessage(), e1 );
         } finally {
            out = null;
         }
         throw new UncheckedIOException( e );
      }
   }

   private Path filename() {
      return Paths.get( Timestamp.path( logDirectory.toString(), lastPattern, filename, ext ) );
   }

   private synchronized void refresh() {
      String currentPattern = currentPattern();
      if( !Objects.equals( this.lastPattern, currentPattern ) ) try {
         closeOutput();
         lastPattern = currentPattern;
      } catch( IOException e ) {
         log.error( e.getMessage(), e );
      }
   }

   private String currentPattern() {
      return Timestamp.format( DateTime.now(), bucketsPerHour );
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "@" + filename();
   }
}
