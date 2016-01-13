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
import oap.metrics.Metrics;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class Archiver implements Runnable {
    private final Path sourceDirectory;
    private final Path destinationDirectory;
    private final long safeInterval;
    private String mask;


    public Archiver( Path sourceDirectory, Path destinationDirectory, long safeInterval, String mask ) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
    }

    @Override
    public void run() {
        log.debug( "let's start packing of " + mask + " in " + sourceDirectory + " into " + destinationDirectory );
        for( Path path : Files.wildcard( sourceDirectory, mask ) )
            if( path.toFile().lastModified() < DateTimeUtils.currentTimeMillis() - safeInterval ) {
                log.debug( "archiving " + path );
                Metrics.measureTimer( Metrics.name( "archiver_time" ), () -> {
                    Metrics.measureHistogram( "archiver_size_from", path.toFile().length() );

                    Path targetFile = destinationDirectory.resolve( sourceDirectory.relativize( path ) + ".gz" );
                    Path targetTemp = destinationDirectory.resolve( sourceDirectory.relativize( path ) + ".gz.tmp" );
                    Files.copy( path, PLAIN, targetTemp, GZIP );
                    Files.rename( targetTemp, targetFile );
                    Files.delete( path );

                    Metrics.measureHistogram( "archiver_size_to", targetFile.toFile().length() );
                } );
            } else log.debug( "skipping (not safe yet) " + path );
        log.debug( "packing is done" );
    }
}
