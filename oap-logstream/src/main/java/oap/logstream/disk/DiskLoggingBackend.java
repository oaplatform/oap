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

import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.io.Files;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DiskLoggingBackend implements LoggingBackend {
   public static final int DEFAULT_BUFFER = 1024 * 100;
   public static final String METRICS_LOGGING_DISK = "logging.disk";
   public static final long DEFAULT_FREE_SPACE_REQUIRED = 2000000000L;
   private final Path logDirectory;
   private final String ext;
   private final int bufferSize;
   private final boolean compress;
   private final ConcurrentHashMap<String, Writer> writers = new ConcurrentHashMap<>();
   private final int bucketsPerHour;
   private boolean closed;
   public long requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
   public boolean useClientHostPrefix = true;

   public DiskLoggingBackend( Path logDirectory, String ext, int bufferSize, int bucketsPerHour, boolean compress ) {
      this.logDirectory = logDirectory;
      this.ext = ext;
      this.bufferSize = bufferSize;
      this.bucketsPerHour = bucketsPerHour;
      this.compress = compress;
   }

   @Override
   public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {
      if( closed ) throw new UncheckedIOException( new IOException( "already closed!" ) );

      Metrics.measureCounterIncrement( Metrics.name( METRICS_LOGGING_DISK ).tag( "from", hostName ) );
      String fullFileName = useClientHostPrefix ? hostName + "/" + fileName : fileName;
      Writer writer = writers.computeIfAbsent( fullFileName,
         k -> new Writer( logDirectory, fullFileName, ext, bufferSize, bucketsPerHour, compress ) );
      log.trace( "logging {} bytes to {}", length, writer );
      writer.write( buffer, offset, length );
   }

   @Override
   public void close() {
      if( !closed ) {
         closed = true;
         writers.forEach( ( selector, writer ) -> Closeables.close( writer ) );
         writers.clear();
      }
   }

   @Override
   public boolean isLoggingAvailable() {
      return Files.usableSpaceAtDirectory( logDirectory ) > requiredFreeSpace;
   }
}
