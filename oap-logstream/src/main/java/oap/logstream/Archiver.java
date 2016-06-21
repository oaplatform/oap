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
package oap.logstream;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.metrics.Metrics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class Archiver implements Runnable {
   private final Path sourceDirectory;
   private final Path destinationDirectory;
   private final long safeInterval;
   private final IoStreams.Encoding encoding;
   private final String mask;
   private int bucketsPerHour;
   private int bufferSize = 1024 * 256 * 4 * 4;


   public Archiver( Path sourceDirectory, Path destinationDirectory, long safeInterval, String mask, IoStreams.Encoding encoding, int bucketsPerHour ) {
      this.sourceDirectory = sourceDirectory;
      this.destinationDirectory = destinationDirectory;
      this.safeInterval = safeInterval;
      this.mask = mask;
      this.encoding = encoding;
      this.bucketsPerHour = bucketsPerHour;
   }

   @Override
   public void run() {
      log.debug( "let's start packing of {} in {} into {}", mask, sourceDirectory, destinationDirectory );
      String timestamp = Timestamp.format( DateTime.now(), bucketsPerHour );

      log.debug( "current timestamp is {}", timestamp );
      long elapsed = DateTimeUtils.currentTimeMillis() - Timestamp.currentBucketStartMillis( bucketsPerHour );
      if( elapsed < safeInterval )
         log.debug( "not safe to process yet ({}ms), some of the files could still be open, waiting...", elapsed );
      else for( Path path : Files.wildcard( sourceDirectory, mask ) ) {
         if( path.getFileName().toString().contains( timestamp ) ) {
            log.debug( "skipping (current timestamp) {}", path );
            continue;
         }

         Path targetFile = destinationDirectory.resolve( sourceDirectory.relativize( path )
            + encoding.extension.map( e -> "." + e ).orElse( "" ) );

         Metrics.measureTimer( Metrics.name( "archive" ), () -> {
            if( encoding.compress ) {
               log.debug( "compressing {} ({} bytes)", path, path.toFile().length() );
               Path targetTemp = destinationDirectory.resolve( sourceDirectory.relativize( path ) + ".tmp" );
               Files.copy( path, PLAIN, targetTemp, encoding, bufferSize );
               Files.rename( targetTemp, targetFile );
               log.debug( "compressed {} ({} bytes)", path, targetFile.toFile().length() );
               Files.delete( path );
            } else {
               log.debug( "moving {} ({} bytes)", path, path.toFile().length() );
               Files.ensureFile( targetFile );
               Files.rename( path, targetFile );
            }
         } );
      }
      log.debug( "packing is done" );
   }
}
