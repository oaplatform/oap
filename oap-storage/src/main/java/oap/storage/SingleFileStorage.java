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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static oap.concurrent.Threads.synchronizedOn;
import static oap.io.IoStreams.DEFAULT_BUFFER;

/**
 * Created by igor.petrenko on 23.09.2016.
 */
@Slf4j
public class SingleFileStorage<T> extends MemoryStorage<T> {
    private static final byte[] BEGIN_ARRAY = "[".getBytes();
    private static final byte[] END_ARRAY = "]".getBytes();
    private static final byte[] ITEM_SEP = ",".getBytes();
    private final PeriodicScheduled scheduled;
    private Path path;
    private AtomicBoolean modified = new AtomicBoolean( false );


    public SingleFileStorage( Path path, Identifier<T> identifier, long fsync, LockStrategy lockStrategy ) {
        super( identifier, lockStrategy );
        this.path = path;

        load();
        addDataListener( new SFSDataListener<>() );
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
    }

    private void load() {
        Files.ensureFile( path );

        if( java.nio.file.Files.exists( path ) ) {
            Binder.json.unmarshal( new TypeReference<List<Metadata<T>>>() {
            }, IoStreams.in( path ) )
                .forEach( m -> {
                    val id = getIdentifier().get( m.object );
                    data.put( id, m );
                } );
        }
        log.info( data.size() + " object(s) loaded." );
    }

    @SneakyThrows
    private synchronized void fsync( long last ) {
        log.trace( "fsync: last: {}, storage size: {}", last, data.size() );

        if( modified.getAndSet( false ) ) {
            log.debug( "fsync storing {}...", path );

            OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), DEFAULT_BUFFER, false, true );
            out.write( BEGIN_ARRAY );

            Iterator<Metadata<T>> it = data.values().iterator();
            while( it.hasNext() ) {
                Metadata<T> metadata = it.next();
                val id = getIdentifier().get( metadata.object );
                synchronizedOn( id, () -> Binder.json.marshal( out, metadata ) );
                if( it.hasNext() ) out.write( ITEM_SEP );
            }
            out.write( END_ARRAY );

            out.close();
            log.debug( "fsync storing {}... done", path );
        }
    }

    @Override
    public synchronized void close() {
        Scheduled.cancel( scheduled );
        fsync();
    }

    @Override
    public void fsync() {
        super.fsync();

        fsync( scheduled.lastExecuted() );
    }

    public final Path getPath() {
        return path;
    }

    private class SFSDataListener<T> implements DataListener<T> {
        @Override
        public void updated( T object, boolean added ) {
            modified.set( true );
        }

        @Override
        public void updated( Collection<T> objects, boolean added ) {
            modified.set( true );
        }

        @Override
        public void deleted( T object ) {
            modified.set( true );
        }

        @Override
        public void deleted( Collection<T> objects ) {
            modified.set( true );
        }
    }
}
