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

import lombok.val;
import oap.json.Binder;
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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MemoryStorage<T> implements Storage<T>, ReplicationMaster<T> {
    protected final Identifier<T> identifier;
    protected final LockStrategy lockStrategy;
    private final List<DataListener<T>> dataListeners = new ArrayList<>();
    private final ArrayList<Constraint<T>> constraints = new ArrayList<>();
    volatile protected ConcurrentMap<String, Item<T>> data = new ConcurrentHashMap<>();

    public MemoryStorage( Identifier<T> identifier, LockStrategy lockStrategy ) {
        this.identifier = identifier;
        this.lockStrategy = lockStrategy;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <TMetadata> Stream<T> select( Predicate<TMetadata> metadataFilter ) {
        return Stream.of( data.values() ).filter( i -> metadataFilter.test( ( TMetadata ) i.metadata ) ).map( i -> i.object );
    }

    @Override
    public <TMetadata> T store( T object, Function<T, TMetadata> metadata ) {
        String id = identifier.getOrInit( object, this );
        lockStrategy.synchronizedOn( id, () -> {
            val item = data.get( id );
            val m = metadata.apply( object );

            if( item != null ) {

                checkConstraints( object );

                item.update( object );
                item.metadata = m;
                item.metadata = m;
            } else {
                checkConstraints( object );
                data.computeIfAbsent( id, id1 -> new Item<>( object, m ) );
            }
            fireUpdated( object, item == null );
        } );

        return object;
    }

    private void checkConstraints( T object ) {
        constraints.forEach( c -> c.check( object, this, identifier::get ) );
    }

    @Override
    public <TMetadata> Optional<T> update( String id, T object, Function<T, TMetadata> metadata ) {
        return lockStrategy.synchronizedOn( id, () -> {
            val item = data.get( id );
            if( item != null ) {
                identifier.set( object, id );

                val m = metadata.apply( object );

                checkConstraints( object );

                item.update( object, m );
                fireUpdated( object, false );
                return Optional.of( item.object );
            } else return Optional.empty();
        } );
    }

    @Override
    public <TMetadata> void store( Collection<T> objects, Function<T, TMetadata> metadata ) {
        ArrayList<T> newObjects = new ArrayList<>();
        ArrayList<T> updatedObjects = new ArrayList<>();

        for( T object : objects ) {
            String id = identifier.getOrInit( object, this );
            lockStrategy.synchronizedOn( id, () -> {
                val item = data.get( id );
                if( item != null ) {
                    item.update( object );
                    val m = metadata.apply( object );
                    if( m != null ) item.metadata = m;
                    updatedObjects.add( object );
                } else {
                    data.computeIfAbsent( id, ( id1 ) -> new Item<>( object, metadata.apply( object ) ) );
                    newObjects.add( object );
                }
            } );
        }
        if( !newObjects.isEmpty() ) fireUpdated( newObjects, true );
        if( !updatedObjects.isEmpty() ) fireUpdated( updatedObjects, false );
    }

    @Override
    public <TMetadata> Optional<T> update( String id, BiPredicate<T, TMetadata> predicate,
                                           BiFunction<T, TMetadata, T> update,
                                           Supplier<T> init,
                                           Function<T, TMetadata> initMetadata ) {
        return updateObject( id, predicate, update, init, initMetadata )
            .map( m -> {
                fireUpdated( m.object, false );
                return m.object;
            } );
    }

    protected <TMetadata> Optional<? extends Item<T>> updateObject( String id,
                                                                    BiPredicate<T, TMetadata> predicate,
                                                                    BiFunction<T, TMetadata, T> update,
                                                                    Supplier<T> init,
                                                                    Function<T, TMetadata> initMetadata ) {
        return lockStrategy.synchronizedOn( id, () -> {
            Item<T> item = data.get( id );
            if( item == null ) {
                if( init == null ) return Optional.empty();

                item = data.computeIfAbsent( id, ( id1 ) -> {
                    val object = init.get();
                    identifier.set( object, id );

                    val m = initMetadata.apply( object );

                    checkConstraints( object );

                    return new Item<>( object, m );
                } );
                data.put( id, item );
                item.setUpdated();
            } else {
                if( predicate.test( item.object, ( TMetadata ) item.metadata ) ) {
                    val newObject = update.apply( Binder.json.clone( item.object ), ( TMetadata ) item.metadata );

                    val m = initMetadata.apply( newObject );

                    checkConstraints( newObject );

                    identifier.set( newObject, id );
                    item.update( newObject );
                    if( m != null ) item.metadata = m;
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of( item );
        } );
    }

    protected Optional<? extends Item<T>> updateObject( String id,
                                                        Predicate<T> predicate,
                                                        Function<T, T> update,
                                                        Supplier<T> init ) {
        return updateObject( id, ( o, m ) -> predicate.test( o ),
            ( o, m ) -> update.apply( o ),
            init,
            this::getDefaultMetadata );
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
        final Optional<Item<T>> item = deleteObject( id );
        item.ifPresent( m -> fireDeleted( m.object ) );

        return item.map( m -> m.object );
    }

    protected Optional<Item<T>> deleteObject( String id ) {
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
    public void addConstraint( Constraint<T> constraint ) {
        this.constraints.add( constraint );
    }

    @Override
    public List<Item<T>> updatedSince( long time ) {
        return Stream.of( data.values() )
            .filter( m -> m.modified > time )
            .toList();
    }

    @Override
    public List<Item<T>> updatedSince( long time, int limit, int offset ) {
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

    @Override
    @SuppressWarnings( "unchecked" )
    public <M> M updateMetadata( String id, Function<M, M> func ) {
        return lockStrategy.synchronizedOn( id, () -> {
            val item = data.get( id );
            if( item != null ) {
                item.metadata = func.apply( ( M ) item.metadata );
                item.setUpdated();
                return ( M ) item.metadata;
            }

            return null;
        } );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <M> M getMetadata( String id ) {
        val item = data.get( id );
        return item != null ? ( M ) item.metadata : null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <M> Stream<M> selectMetadata() {
        return Stream.of( data.values().stream().map( item -> ( M ) item.metadata ) );
    }

    Stream<Item<T>> selectItem() {
        return Stream.of( data.values().stream() );
    }
}
