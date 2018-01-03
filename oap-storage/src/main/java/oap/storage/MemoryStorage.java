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

import oap.util.Maps;
import oap.util.Optionals;
import oap.util.Stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MemoryStorage<T> implements Storage<T>, ReplicationMaster<T> {
    protected final Identifier<T> identifier;
    protected final LockStrategy lockStrategy;
    private final List<DataListener<T>> dataListeners = new ArrayList<>();
    volatile protected ConcurrentMap<String, Metadata<T>> data = new ConcurrentHashMap<>();

    public MemoryStorage( Identifier<T> identifier, LockStrategy lockStrategy ) {
        this.identifier = identifier;
        this.lockStrategy = lockStrategy;
    }

    @Override
    public Stream<T> select() {
        return Stream.of( data.values() ).map( m -> m.object );
    }

    @Override
    public T store( T object ) {
        String id = identifier.getOrInit( object, this );
        lockStrategy.synchronizedOn( id, () -> {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) metadata.update( object );
            else data.computeIfAbsent( id, id1 -> new Metadata<>( object ) );
            fireUpdated( object, metadata == null );
        } );

        return object;
    }

    @Override
    public Optional<T> update( String id, T object ) {
        return lockStrategy.synchronizedOn( id, () -> {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) {
                identifier.set( object, id );
                metadata.update( object );
                fireUpdated( object, false );
                return Optional.of( metadata.object );
            } else return Optional.empty();
        } );
    }

    @Override
    public void store( Collection<T> objects ) {
        ArrayList<T> newObjects = new ArrayList<>();
        ArrayList<T> updatedObjects = new ArrayList<>();

        for( T object : objects ) {
            String id = identifier.getOrInit( object, this );
            lockStrategy.synchronizedOn( id, () -> {
                Metadata<T> metadata = data.get( id );
                if( metadata != null ) {
                    metadata.update( object );
                    updatedObjects.add( object );
                } else {
                    data.computeIfAbsent( id, ( id1 ) -> new Metadata<>( object ) );
                    newObjects.add( object );
                }
            } );
        }
        if( !newObjects.isEmpty() ) fireUpdated( newObjects, true );
        if( !updatedObjects.isEmpty() ) fireUpdated( updatedObjects, false );
    }

    @Override
    public Optional<T> update( String id, Predicate<T> predicate, Function<T, T> update, Supplier<T> init ) {
        return updateObject( id, predicate, update, init )
            .map( m -> {
                fireUpdated( m.object, false );
                return m.object;
            } );
    }

    protected Optional<? extends Metadata<T>> updateObject( String id, Predicate<T> predicate, Function<T, T> update, Supplier<T> init ) {
        return lockStrategy.synchronizedOn( id, () -> {
            Metadata<T> m = data.get( id );
            if( m == null ) {
                if( init == null ) return Optional.empty();

                m = data.computeIfAbsent( id, ( id1 ) -> new Metadata<>( init.get() ) );
                data.put( id, m );
                m.update( m.object ); // fix modification time
            } else {
                if( predicate.test( m.object ) ) {
                    m.update( update.apply( m.object ) );
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of( m );
        } );
    }

    @Override
    public void update( Collection<String> ids, Predicate<T> predicate, Function<T, T> update, Supplier<T> init ) {
        fireUpdated( Stream.of( ids )
            .flatMap( id -> Optionals.toStream( updateObject( id, predicate, update, init )
                .map( m -> m.object ) ) )
            .toList(), false );
    }

    @Override
    public Optional<T> get( String id ) {
        return Maps.get( data, id ).map( m -> m.object );

    }

    @Override
    public void deleteAll() {
        List<T> objects = select().toList();
        data.clear();
        fireDeleted( objects );
    }

    public Optional<T> delete( String id ) {
        final Optional<Metadata<T>> metadata = deleteObject( id );
        metadata.ifPresent( m -> fireDeleted( m.object ) );

        return metadata.map( m -> m.object );
    }

    protected Optional<Metadata<T>> deleteObject( String id ) {
        return lockStrategy.synchronizedOn( id, () -> Optional.ofNullable( data.remove( id ) ) );
    }

    @Override
    public long size() {
        return data.size();
    }

    @Override
    public synchronized MemoryStorage<T> copyAndClean() {
        final MemoryStorage<T> ms = new MemoryStorage<>( identifier, lockStrategy );
        ms.data = data;
        this.data = new ConcurrentHashMap<>();
        return ms;
    }

    @Override
    public synchronized Map<String, T> toMap() {
        return data.entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, entry -> entry.getValue().object ) );
    }

    @Override
    public void fsync() {

    }

    protected void fireUpdated( T object, boolean isNew ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.updated( object, isNew );
    }

    protected void fireUpdated( Collection<T> objects, boolean isNew ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.updated( objects, isNew );
    }

    protected void fireDeleted( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( object );
    }

    protected void fireDeleted( List<T> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.deleted( objects );
    }

    @Override
    public void addDataListener( DataListener<T> dataListener ) {
        this.dataListeners.add( dataListener );
    }

    @Override
    public void removeDataListener( DataListener<T> dataListener ) {
        this.dataListeners.remove( dataListener );
    }

    @Override
    public List<Metadata<T>> updatedSince( long time ) {
        return Stream.of( data.values() )
            .filter( m -> m.modified > time )
            .toList();
    }

    @Override
    public List<Metadata<T>> updatedSince( long time, int limit, int offset ) {
        return Stream.of( data.values() )
            .filter( m -> m.modified > time )
            .skip( offset )
            .limit( limit )
            .toList();
    }

    @Override
    public List<String> ids() {
        return new ArrayList<>( data.keySet() );
    }

    public void clear() {
        List<String> keys = new ArrayList<>( data.keySet() );
        List<T> deleted = Stream.of( keys ).flatMapOptional( this::deleteObject ).map( m -> m.object ).toList();
        fireDeleted( deleted );
    }

    @Override
    public void close() {
    }

    @Override
    public Iterator<T> iterator() {
        return select().iterator();
    }

    @Override
    public void forEach( Consumer<? super T> action ) {
        data.forEach( ( k, v ) -> action.accept( v.object ) );
    }
}
