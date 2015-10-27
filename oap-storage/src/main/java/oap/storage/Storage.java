/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.replication.ReplicationMaster;
import oap.util.Stream;
import oap.util.Try;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;
import static org.slf4j.LoggerFactory.getLogger;

public class Storage<T> implements ReplicationMaster<Metadata<T>> {
    private final org.slf4j.Logger logger = getLogger( getClass() );
    private final AtomicLong lastSync = new AtomicLong( 0 );
    protected Function<T, String> identify;
    protected ConcurrentMap<String, Metadata<T>> data = new ConcurrentHashMap<>();
    protected long fsync = 60 * 1000; //ms
    private Path path;
    private Scheduled scheduled;
    private List<DataListener<T>> dataListeners = new ArrayList<>();

    public Storage( Path path, Function<T, String> identify ) {
        this.path = path;
        this.identify = identify;
    }

    @SuppressWarnings( "unchecked" )
    protected void load() {
        data = Files.wildcard( path, "*.json" )
            .stream()
            .map( Try.map(
                f -> (Metadata<T>) Binder.unmarshal( new TypeReference<Metadata<T>>() {
                }, f ) ) )
            .sorted( reverseOrder() )
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
        logger.info( data.size() + " object(s) loaded." );
    }

    public void start() {
        if( fsync > 0 ) {
            path.toFile().mkdirs();
            load();
            scheduled = Scheduler.scheduleWithFixedDelay( fsync, TimeUnit.MILLISECONDS, this::persist );
        }
    }

    private synchronized void persist() {
        if( fsync > 0 ) {
            long current = DateTimeUtils.currentTimeMillis() - 1000;
            long last = lastSync.get();

            data.values()
                .stream()
                .filter( m -> m.modified > last )
                .forEach( Try.consume( m -> Binder.marshal( path.resolve( m.id + ".json" ), m ) ) );

            lastSync.set( current );
        }
    }

    public synchronized void stop() {
        Scheduled.cancel( scheduled );
        persist();
        data.clear();
    }

    public Stream<T> select() {
        return Stream.of( data.values() )
            .filter( m -> !m.deleted )
            .map( m -> m.object );
    }

    public void store( T object ) {
        String id = this.identify.apply( object );
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null )
                metadata.update( object );
            else
                data.put( id, new Metadata<>( id, object ) );
            fireUpdated( object );
        }
    }

    public void store( List<T> objects ) {
        for( T object : objects ) {
            String id = this.identify.apply( object );
            synchronized( id.intern() ) {
                Metadata<T> metadata = data.get( id );
                if( metadata != null )
                    metadata.update( object );
                else
                    data.put( id, new Metadata<>( id, object ) );
            }
        }
        fireUpdated( objects );
    }

    public T update( String id, Consumer<T> update ) {
        return update( id, update, null );
    }

    public T update( String id, Consumer<T> update, Supplier<T> init ) {
        synchronized( id.intern() ) {
            Metadata<T> m = data.get( id );
            if( m == null ) {
                if( init == null ) return null;
                T object = init.get();
                m = new Metadata<>( identify.apply( object ), object );
                data.put( m.id, m );
            } else update.accept( m.object );
            fireUpdated( m.object );
            return m.object;
        }
    }

    public Optional<T> get( String id ) {
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata == null ) return Optional.empty();
            else return Optional.of( metadata.object );
        }
    }

    public void clear() {
        List<T> objects = new ArrayList<>();
        for( String id : data.keySet() ) remove( id ).ifPresent( m -> objects.add( m.object ) );
        fireDeleted( objects );
    }

    private Optional<Metadata<T>> remove( String id ) {
        synchronized( id.intern() ) {
            path.resolve( id + ".json" ).toFile().delete();
            return Optional.ofNullable( data.remove( id ) );
        }
    }

    protected void deletePermanently( String id ) {
        remove( id ).ifPresent( m -> fireDeleted( m.object ) );
    }

    public long size() {
        return data.size();
    }

    protected void fireUpdated( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.updated( object );
    }

    protected void fireUpdated( List<T> objects ) {
        for( DataListener<T> dataListener : this.dataListeners )
            dataListener.updated( objects );
    }


    protected void fireDeleted( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( object );
    }

    protected void fireDeleted( List<T> objects ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( objects );
    }

    public void addDataListener( DataListener<T> dataListener ) {
        this.dataListeners.add( dataListener );
    }

    public void removeDataListener( DataListener<T> dataListener ) {
        this.dataListeners.remove( dataListener );
    }

    public List<Metadata<T>> get( long lastSyncTime ) {
        return data.values().stream().filter( m -> m.modified > lastSyncTime ).collect( toList() );
    }

    public interface DataListener<T> {
        void updated( T object );

        void updated( List<T> objects );

        void deleted( T object );

        void deleted( List<T> objects );

    }
}
