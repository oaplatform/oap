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
import oap.util.Optionals;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;

@Slf4j
public abstract class Archiver implements Runnable {
    public static final String CORRUPTED_DIRECTORY = ".corrupted";
    public final Path sourceDirectory;
    public final long safeInterval;
    public final Encoding encoding;
    public final String mask;
    public final Path corruptedDirectory;
    private final Timestamp timestamp;
    protected int bufferSize = 1024 * 256 * 4 * 4;


    protected Archiver( Path sourceDirectory, long safeInterval, String mask, Encoding encoding, Timestamp timestamp ) {
        this.sourceDirectory = sourceDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
        this.encoding = encoding;
        this.corruptedDirectory = sourceDirectory.resolve( CORRUPTED_DIRECTORY );
        this.timestamp = timestamp;
    }

    @Override
    public void run() {
        log.debug( "let's start packing of {} in {}", mask, sourceDirectory );
        var timestampStr = timestamp.format( DateTime.now() );

        log.debug( "current timestamp is {}", timestampStr );
        final long bucketStartTime = timestamp.currentBucketStartMillis();
        long elapsed = DateTimeUtils.currentTimeMillis() - bucketStartTime;
        if( elapsed < safeInterval )
            log.debug( "not safe to process yet ({}ms), some of the files could still be open, waiting...", elapsed );
        else for( Path path : Files.wildcard( sourceDirectory, mask ) ) {
            if( path.startsWith( corruptedDirectory ) ) continue;
            Optionals.fork( timestamp.parse( path ) )
                .ifAbsent( () -> log.error( "what a hell is that {}", path ) )
                .ifPresent( dt -> {
                    if( dt.isBefore( bucketStartTime ) ) archive( path );
                    else log.debug( "skipping (current timestamp) {}", path );
                } );
        }

        cleanup();

        log.debug( "packing is done" );
    }

    protected abstract void cleanup();

    protected abstract void archive( Path path );
}
