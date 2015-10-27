/**
 * Copyright
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

    public void log( String path, String line ) {
        log( path, line.getBytes() );
    }

    public void log( String path, byte[] buffer ) {
        if( closed ) throw new UncheckedIOException( new IOException( "already closed!" ) );
        writers.computeIfAbsent( path,
            k -> new LogWriter( logDirectory.resolve( path ).toString(), suffix, bufferSize, interval ) )
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
