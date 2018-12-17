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

package oap.storage;

import lombok.SneakyThrows;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.util.Collections.anyMatch;
import static org.slf4j.LoggerFactory.getLogger;

public class FilePersistence<T> implements Closeable {
    private static final byte[] BEGIN_ARRAY = "[".getBytes();
    private static final byte[] END_ARRAY = "]".getBytes();
    private static final byte[] ITEM_SEP = ",".getBytes();
    private PeriodicScheduled scheduled;
    private Path path;
    private final long fsync;
    private final MemoryStorage<T> storage;
    private final Lock lock = new ReentrantLock();
    private final Logger log;


    public FilePersistence( Path path, long fsync, MemoryStorage<T> storage ) {
        this.path = path;
        this.fsync = fsync;
        this.storage = storage;
        this.log = getLogger( toString() );
    }

    public void start() {
        load();
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
    }

    private void load() {
        Threads.synchronously( lock, () -> {
            Files.ensureFile( path );

            if( Files.exists( path ) ) {
                Binder.json.unmarshal( new TypeRef<List<Metadata<T>>>() {
                }, IoStreams.in( path ) )
                    .forEach( m -> {
                        String id = storage.identifier.get( m.object );
                        storage.data.put( id, m );
                    } );
            }
            log.info( storage.data.size() + " object(s) loaded." );
        } );
    }

    @SneakyThrows
    private synchronized void fsync( long last ) {
        Threads.synchronously( lock, () -> {
            log.trace( "fsync: last: {}, storage length: {}", last, storage.data.size() );

            if( anyMatch( storage.data.values(), m -> m.modified > last ) ) {
                log.debug( "fsync storing {}...", path );

                OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), DEFAULT_BUFFER, false, true );
                out.write( BEGIN_ARRAY );

                Iterator<Metadata<T>> it = storage.data.values().iterator();
                while( it.hasNext() ) {
                    Binder.json.marshal( out, it.next() );
                    if( it.hasNext() ) out.write( ITEM_SEP );
                }
                out.write( END_ARRAY );

                out.close();
                log.debug( "fsync storing {}... done", path );
            }
        } );
    }

    @Override
    public void close() {
        Threads.synchronously( lock, () -> {
            Scheduled.cancel( scheduled );
            fsync( scheduled.lastExecuted() );
        } );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + path;
    }
}
