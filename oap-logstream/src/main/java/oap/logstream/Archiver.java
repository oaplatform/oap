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
import oap.io.IoStreams.Encoding;
import oap.metrics.Metrics;
import oap.util.Optionals;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;

@Slf4j
public class Archiver implements Runnable {
    public static final String CORRUPTED_DIRECTORY = ".corrupted";
    public final Path sourceDirectory;
    public final Path destinationDirectory;
    public final long safeInterval;
    public final Encoding encoding;
    public final String mask;
    public final Path corruptedDirectory;
    private int bufferSize = 1024 * 256 * 4 * 4;


    public Archiver( Path sourceDirectory, Path destinationDirectory, long safeInterval, String mask, Encoding encoding ) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
        this.encoding = encoding;
        this.corruptedDirectory = sourceDirectory.resolve( CORRUPTED_DIRECTORY );
    }

    @Override
    public void run() {
        log.debug( "let's start packing of {} in {} into {}", mask, sourceDirectory, destinationDirectory );
        String timestamp = Timestamp.format( DateTime.now() );

        log.debug( "current timestamp is {}", timestamp );
        final long bucketStartTime = Timestamp.currentBucketStartMillis();
        long elapsed = DateTimeUtils.currentTimeMillis() - bucketStartTime;
        if( elapsed < safeInterval )
            log.debug( "not safe to process yet ({}ms), some of the files could still be open, waiting...", elapsed );
        else for( Path path : Files.wildcard( sourceDirectory, mask ) ) {
            if( path.startsWith( corruptedDirectory ) ) continue;
            Optionals.fork( Timestamp.parse( path ) )
                .ifAbsent( () -> log.error( "what a hell is that {}", path ) )
                .ifPresent( dt -> {
                    if( dt.isBefore( bucketStartTime ) ) archive( path );
                    else log.debug( "skipping (current timestamp) {}", path );
                } );
        }

        Files.deleteEmptyDirectories( sourceDirectory );
        Files.deleteEmptyDirectories( destinationDirectory );

        log.debug( "packing is done" );
    }

    private void archive( Path path ) {
        Encoding from = Encoding.from( path );

        Path destination = destinationDirectory.resolve(
            encoding.resolve( sourceDirectory.relativize( path ) ) );

        Metrics.measureTimer( Metrics.name( "archive" ), () -> {
            if( !Files.isFileEncodingValid( path ) ) {
                Files.rename( path, corruptedDirectory.resolve( sourceDirectory.relativize( path ) ) );
                log.debug( "corrupted {}", path );
            } else if( from != encoding ) {
                log.debug( "compressing {} ({} bytes)", path, path.toFile().length() );
                Files.copy( path, from, destination, encoding, bufferSize );
                log.debug( "compressed {} ({} bytes)", path, destination.toFile().length() );
                Files.delete( path );
            } else {
                log.debug( "moving {} ({} bytes)", path, path.toFile().length() );
                Files.rename( path, destination );
            }
        } );
    }
}
