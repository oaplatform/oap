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
import oap.json.Binder;
import oap.reflect.TypeRef;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static oap.util.Collections.anyMatch;
import static org.slf4j.LoggerFactory.getLogger;

public class FilePersistence<T> implements Closeable, Storage.DataListener<T> {
    private final long fsync;
    private final MemoryStorage<T> storage;
    private final Lock lock = new ReentrantLock();
    private final Logger log;
    private PeriodicScheduled scheduled;
    private Path path;


    public FilePersistence( Path path, long fsync, MemoryStorage<T> storage ) {
        this.path = path;
        this.fsync = fsync;
        this.storage = storage;
        this.log = getLogger( toString() );
    }

    public void start() {
        storage.addDataListener( this );
        load();
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
    }

    private void load() {
        Threads.synchronously( lock, () -> {
            Binder.json.unmarshal( path, new TypeRef<List<Metadata<T>>>() {} )
                .forEach( m -> {
                    String id = storage.identifier.get( m.object );
                    storage.data.put( id, m );
                } );
            log.info( storage.data.size() + " object(s) loaded." );
        } );
    }

    @SneakyThrows
    private synchronized void fsync( long last ) {
        Threads.synchronously( lock, () -> {
            log.trace( "fsync: last: {}, storage length: {}", last, storage.data.size() );

            if( anyMatch( storage.data.values(), m -> m.modified > last ) ) {
                log.debug( "fsync storing {}...", path );

                Binder.json.marshal( path, storage.data.values() );

                log.debug( "fsync storing {}... done", path );
            }
        } );
    }

    @Override
    public void close() {
        storage.removeDataListener( this );
        Threads.synchronously( lock, () -> Scheduled.cancel( scheduled ) );
        fsync( scheduled.lastExecuted() );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + path;
    }

    @Override
    public void fsync() {
        fsync( scheduled.lastExecuted() );
    }
}
