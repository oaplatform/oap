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

import oap.io.Files;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

public class Archiver implements Runnable {
    private final Path sourceDirectory;
    private final Path destinationDirectory;
    private final int safeInterval;
    private String mask;
    private static Logger logger = getLogger( Archiver.class );


    public Archiver( Path sourceDirectory, Path destinationDirectory, int safeInterval, String mask ) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
    }

    @Override
    public void run() {
        logger.debug( "let's start packing of " + mask + " in " + sourceDirectory + " into " +destinationDirectory);
        for( Path path : Files.wildcard( sourceDirectory, mask ) )
            if( path.toFile().lastModified() < DateTime.now().minusMinutes( safeInterval ).getMillis() ) {
                logger.debug( "archiving " + path );
                Files.copy( path, PLAIN,
                    destinationDirectory.resolve( sourceDirectory.relativize( path ) + ".gz" ), GZIP );
                Files.delete( path );
            } else logger.debug( "skipping(not safe yet) " + path );
        logger.debug( "packing is done" );
    }
}
