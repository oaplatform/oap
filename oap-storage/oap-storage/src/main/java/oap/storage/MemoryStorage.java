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

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.Storage.DataListener.IdObject;
import oap.util.BiStream;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static oap.storage.Storage.DataListener.IdObject.__io;

@Slf4j
public class MemoryStorage<Id, Data> implements Storage<Id, Data>, ReplicationMaster<Id, Data>, RemoteStorage<Id, Data> {
    public final Identifier<Id, Data> identifier;
    protected final Lock lock;
    protected final List<DataListener<Id, Data>> dataListeners = new CopyOnWriteArrayList<>();
    protected final Memory<Data, Id> memory;
    private final Predicate<Id> conflict = Identifier.toConflict( this::get );

    public MemoryStorage( Identifier<Id, Data> identifier, Lock lock ) {
        this.identifier = identifier;
        this.lock = lock;
        this.memory = new Memory<>( lock );
    }

    public Stream<Data> select( boolean liveOnly ) {
        return ( liveOnly ? memory.selectLive() : memory.selectAll() ).map( p -> p._2.object );
    }

    public Stream<Data> select() {
        return select( true );
    }

    Stream<Data> selectAll() {
        return memory.selectAll().map( p -> p._2.object );
    }

    @Override
    public List<Data> list() {
        return select().toList();
    }

    @Override
    public Data store( @Nonnull Data object ) {
//        this is not thread-safe
//        new acquired id does not lead to conflicts
        Id id = identifier.getOrInit( object, conflict );
        lock.synchronizedOn( id, () -> {
            if( memory.put( id, object ) ) fireAdded( id, object );
            else fireUpdated( id, object );
        } );
        return object;
    }

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    @Nullable
    @Override
    public Data store( @Nonnull Data object, long hash ) {
        Id id = identifier.getOrInit( object, conflict );
        return lock.synchronizedOn( id, () -> {
            Metadata<Data> metadata = memory.get( id ).orElse( null );
            if( ( metadata == null && hash == 0L ) || ( metadata != null && metadata.hash == hash ) ) {
                if( memory.put( id, object ) ) fireAdded( id, object );
                else fireUpdated( id, object );

                return object;
            } else {
                return null;
            }
        } );
    }

    @Override
    public void store( Collection<Data> objects ) {
        List<IdObject<Id, Data>> added = new ArrayList<>();
        List<IdObject<Id, Data>> updated = new ArrayList<>();

        for( Data object : objects ) {
            Id id = identifier.getOrInit( object, conflict );
            lock.synchronizedOn( id, () -> {
                if( memory.put( id, object ) ) added.add( __io( id, object ) );
                else updated.add( __io( id, object ) );
            } );
        }
        fireAdded( added );
        fireUpdated( updated );
    }

    @Override
    public Optional<Data> update( @Nonnull Id id, @Nonnull Function<Data, Data> update ) {
        requireNonNull( id );

        Optional<Metadata<Data>> result = memory.remap( id, update );
        result.ifPresent( m -> fireUpdated( id, m.object ) );
        return result.map( m -> m.object );
    }

    @Override
    public Data update( Id id, @Nonnull Function<Data, Data> update, @Nonnull Supplier<Data> init ) {
        if( id == null ) return store( init.get() );
        else return lock.synchronizedOn( id, () -> update( id, update ).orElseGet( () -> store( init.get() ) ) );
    }

    @Override
    public Optional<Metadata<Data>> getMetadata( @Nonnull Id id ) {
        return memory.get( id );
    }

    @Override
    public Optional<Data> get( @Nonnull Id id ) {
        return getMetadata( id ).map( m -> m.object );
    }

    @Override
    public Data get( Id id, @Nonnull Supplier<Data> init ) {
        return id == null ? store( init.get() )
            : lock.synchronizedOn( id, () -> get( id ).orElseGet( () -> store( init.get() ) ) );
    }

    @Override
    public void deleteAll() {
        fireDeleted( Lists.map( memory.markDeletedAll(), p -> __io( p._1, p._2.object ) ) );
    }

    @Override
    public Optional<Data> delete( @Nonnull Id id ) {
        requireNonNull( id );
        Optional<Data> old = memory.markDeleted( id ).map( m -> m.object );
        old.ifPresent( o -> fireDeleted( id, o ) );
        return old;
    }

    @Override
    public Optional<Data> permanentlyDelete( @Nonnull Id id ) {
        requireNonNull( id );
        Optional<Data> old = memory.removePermanently( id ).map( m -> m.object );
        old.ifPresent( o -> firePermanentlyDeleted( id, o ) );
        return old;
    }

    @Override
    public long size() {
        return memory.selectLiveIds().count();
    }

    protected void fireAdded( Id id, Data object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners )
            dataListener.added( List.of( __io( id, object ) ) );
    }

    protected void fireAdded( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<Id, Data> dataListener : this.dataListeners ) dataListener.added( objects );
    }

    protected void fireUpdated( Id id, Data object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners )
            dataListener.updated( List.of( __io( id, object ) ) );
    }

    protected void fireUpdated( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<Id, Data> dataListener : this.dataListeners ) dataListener.updated( objects );
    }

    protected void fireDeleted( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<Id, Data> dataListener : this.dataListeners ) dataListener.deleted( objects );
    }

    protected void fireDeleted( Id id, Data object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners )
            dataListener.deleted( List.of( __io( id, object ) ) );
    }

    protected void firePermanentlyDeleted( Id id, Data object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners )
            dataListener.permanentlyDeleted( __io( id, object ) );
    }

    protected void fireChanged( List<DataListener.IdObject<Id, Data>> added,
                                List<DataListener.IdObject<Id, Data>> updated,
                                List<DataListener.IdObject<Id, Data>> deleted ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners )
            dataListener.changed( added, updated, deleted );
    }

    @Override
    public void addDataListener( DataListener<Id, Data> dataListener ) {
        this.dataListeners.add( dataListener );
    }

    @Override
    public void removeDataListener( DataListener<Id, Data> dataListener ) {
        this.dataListeners.remove( dataListener );
    }

    @Override
    public Identifier<Id, Data> identifier() {
        return identifier;
    }

    @Override
    @Nonnull
    public Iterator<Data> iterator() {
        return select().iterator();
    }

    @Override
    public void forEach( Consumer<? super Data> action ) {
        select().forEach( action );
    }

    @Override
    public Stream<Metadata<Data>> updatedSince( long since ) {
        log.trace( "requested updated objects since={}, total objects={}", since, memory.data.size() );
        return memory.selectLive()
            .mapToObj( ( id, m ) -> m )
            .filter( m -> m.modified >= since );
    }

    @Override
    public List<Id> ids() {
        return memory.selectLiveIds().toList();
    }

    protected static class Memory<T, I> {
        final ConcurrentMap<I, Metadata<T>> data = new ConcurrentHashMap<>();
        private final Lock lock;

        public Memory( Lock lock ) {
            this.lock = lock;
        }

        public BiStream<I, Metadata<T>> selectLive() {
            return BiStream.of( data ).filter( ( id, m ) -> !m.isDeleted() );
        }

        public BiStream<I, Metadata<T>> selectAll() {
            return BiStream.of( data );
        }

        public BiStream<I, Metadata<T>> selectUpdatedSince( long since ) {
            return BiStream.of( data ).filter( ( id, m ) -> m.modified > since );
        }

        public Optional<Metadata<T>> get( @Nonnull I id ) {
            requireNonNull( id );
            return Optional.ofNullable( data.get( id ) )
                .filter( m -> !m.isDeleted() );
        }

        public boolean put( @Nonnull I id, @Nonnull Metadata<T> m ) {
            requireNonNull( id );
            requireNonNull( m );
            log.trace( "storing {}", m );
            return data.put( id, m ) == null;
        }

        public boolean put( @Nonnull I id, @Nonnull T object ) {
            requireNonNull( id );
            requireNonNull( object );
            return lock.synchronizedOn( id, () -> {
                // time: 123 - new Metadata()
                // time: 124 - fsync()
                // time: 125 - data.put( id, metadata )
                // lastmodified must be set after placing the metadata object in the "data"
                final Metadata<T> oldMetadata = data.get( id );
                if( oldMetadata == null ) {
                    Metadata<T> newMetadata = new Metadata<>( object );
                    data.put( id, newMetadata );
                    newMetadata.refresh();
                } else {
                    oldMetadata.update( object );
                }
                log.trace( "storing {}", oldMetadata );
                return oldMetadata == null;
            } );
        }

        public Optional<Metadata<T>> remap( @Nonnull I id, @Nonnull Function<T, T> update ) {
            return lock.synchronizedOn( id, () ->
                Optional.ofNullable( data.compute( id, ( anId, m ) -> m == null
                    ? null
                    : m.update( update.apply( m.object ) ) ) ) );
        }

        public List<Pair<I, Metadata<T>>> markDeletedAll() {
            List<Pair<I, Metadata<T>>> ms = selectLive().toList();
            ms.forEach( p -> p._2.delete() );
            return ms;
        }

        public Optional<Metadata<T>> markDeleted( @Nonnull I id ) {
            return lock.synchronizedOn( id, () -> {
                Metadata<T> metadata = data.get( id );
                if( metadata != null ) {
                    metadata.delete();
                    return Optional.of( metadata );
                } else return Optional.empty();
            } );
        }

        public Optional<Metadata<T>> removePermanently( @Nonnull I id ) {
            return Optional.ofNullable( data.remove( id ) );
        }

        public void clear() {
            data.clear();
        }

        public Stream<I> selectLiveIds() {
            return selectLive().mapToObj( ( id, m ) -> id );
        }

    }
}
