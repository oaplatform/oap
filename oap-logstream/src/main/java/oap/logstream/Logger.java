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

import oap.io.Closeables;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class Logger {
    public static final int DEFAULT_BUFFER = 1024 * 100;
    private ConcurrentHashMap<String, LogWriter> writers = new ConcurrentHashMap<>();
    final Path logDirectory;
    final String suffix;
    final int bufferSize;
    private int interval;
    private boolean closed;

    public Logger( Path logDirectory, String suffix, int bufferSize, int interval ) {
        this.logDirectory = logDirectory;
        this.suffix = suffix;
        this.bufferSize = bufferSize;
        this.interval = interval;
    }

    public void log( String hostName, String fileName, String line ) {
        log( hostName, fileName, line.getBytes() );
    }

    public void log( String hostName, String fileName, byte[] buffer ) {
        if( closed ) throw new UncheckedIOException( new IOException( "already closed!" ) );
        writers.computeIfAbsent( hostName + fileName,
            k -> new LogWriter( logDirectory.resolve( hostName ).toString(), fileName, suffix, bufferSize, interval ) )
            .write( buffer );
    }

    public void close() {
        if( !closed ) {
            closed = true;
            writers.forEach( ( k, writer ) -> Closeables.close( writer ) );
            writers.clear();
        }
    }

    public void flush() {
        writers.forEachValue( Long.MAX_VALUE, LogWriter::flush );
    }
}
