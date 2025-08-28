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
package oap.storagev2;

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storagev2.Storage.DataListener.IdObject;
import oap.util.BiStream;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;
import org.apache.commons.lang3.mutable.MutableObject;

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
import static oap.storagev2.Storage.DataListener.IdObject.__io;

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

    @Override
    public Stream<Data> select() {
        return select( true );
    }

    public Stream<Data> select( boolean liveOnly ) {
        return selectMetadata( liveOnly ).map( metadata -> metadata.object );
    }

    public Stream<Metadata<Data>> selectMetadata( boolean liveOnly ) {
        return ( liveOnly ? memory.selectLive() : memory.selectAll() ).map( p -> p._2 );
    }

    @Override
    public Stream<Metadata<Data>> selectMetadata() {
        return selectMetadata( true );
    }

    Stream<Data> selectAll() {
        return memory.selectAll().map( p -> p._2.object );
    }

    @Override
    public List<Data> list() {
        return select().toList();
    }

    @Override
    public List<Metadata<Data>> listMetadata() {
        return selectMetadata().toList();
    }

    @Override
    public Data store( @Nonnull Data object, String modifiedBy ) {
//        this is not thread-safe
//        new acquired id does not lead to conflicts
        Id id = identifier.getOrInit( object, conflict );
        lock.synchronizedOn( id, () -> {
            if( memory.put( id, object, modifiedBy ) ) {
                fireAdded( id, memory.data.get( id ) );
            } else {
                fireUpdated( id, memory.data.get( id ) );
            }
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
                if( memory.put( id, object, MODIFIED_BY_SYSTEM ) ) {
                    fireAdded( id, memory.data.get( id ) );
                } else {
                    fireUpdated( id, memory.data.get( id ) );
                }

                return object;
            } else {
                return null;
            }
        } );
    }

    @Override
    public void store( Collection<Data> objects, String modifiedBy ) {
        List<IdObject<Id, Data>> added = new ArrayList<>();
        List<IdObject<Id, Data>> updated = new ArrayList<>();

        for( Data object : objects ) {
            Id id = identifier.getOrInit( object, conflict );
            lock.synchronizedOn( id, () -> {
                if( memory.put( id, object, modifiedBy ) ) {
                    added.add( __io( id, memory.data.get( id ) ) );
                } else {
                    updated.add( __io( id, memory.data.get( id ) ) );
                }
            } );
        }
        fireAdded( added );
        fireUpdated( updated );
    }

    @Override
    public Optional<Data> update( @Nonnull Id id, @Nonnull Function<Data, Data> update, String modifiedBy ) {
        requireNonNull( id );

        Optional<Metadata<Data>> result = memory.remap( id, update, modifiedBy );
        result.ifPresent( m -> fireUpdated( id, m ) );
        return result.map( m -> m.object );
    }

    @Override
    public Data update( Id id, @Nonnull Function<Data, Data> update, @Nonnull Supplier<Data> init, String modifiedBy ) {
        if( id == null ) return store( init.get(), modifiedBy );
        else return lock.synchronizedOn( id, () -> update( id, update, modifiedBy ).orElseGet( () -> store( init.get(), modifiedBy ) ) );
    }

    @Override
    public boolean tryUpdate( @Nonnull Id id, @Nonnull Function<Data, Data> tryUpdate, String modifiedBy ) {
        requireNonNull( id );

        Optional<Metadata<Data>> result = memory.tryRemap( id, tryUpdate, modifiedBy );
        result.ifPresent( m -> fireUpdated( id, m ) );
        return result.isPresent();
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
    public Data get( Id id, @Nonnull Supplier<Data> init, String modifiedBy ) {
        return id == null ? store( init.get(), modifiedBy )
            : lock.synchronizedOn( id, () -> get( id ).orElseGet( () -> store( init.get(), modifiedBy ) ) );
    }

    @Override
    public void deleteAll() {
        fireDeleted( Lists.map( memory.markDeletedAll(), p -> __io( p._1, p._2 ) ) );
    }

    @Override
    public Optional<Data> delete( @Nonnull Id id, String modifiedBy ) {
        return deleteMetadata( id, modifiedBy ).map( m -> m.object );
    }

    @Override
    public Optional<Metadata<Data>> deleteMetadata( @Nonnull Id id, String modifiedBy ) {
        requireNonNull( id );
        Optional<Metadata<Data>> old = memory.markDeleted( id, modifiedBy );
        old.ifPresent( o -> fireDeleted( id, o ) );
        return old;
    }

    @Override
    public Optional<Data> permanentlyDelete( @Nonnull Id id ) {
        requireNonNull( id );
        Optional<Metadata<Data>> old = memory.removePermanently( id );
        old.ifPresent( o -> firePermanentlyDeleted( id, o ) );
        return old.map( m -> m.object );
    }

    @Override
    public long size() {
        return memory.selectLiveIds().count();
    }

    protected void fireAdded( Id id, Metadata<Data> medatada ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners ) {
            dataListener.added( List.of( __io( id, medatada ) ) );
        }
    }

    protected void fireAdded( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() ) {
            for( DataListener<Id, Data> dataListener : this.dataListeners ) {
                dataListener.added( objects );
            }
        }
    }

    protected void fireUpdated( Id id, Metadata<Data> metadata ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners ) {
            dataListener.updated( List.of( __io( id, metadata ) ) );
        }
    }

    protected void fireUpdated( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() ) {
            for( DataListener<Id, Data> dataListener : this.dataListeners ) {
                dataListener.updated( objects );
            }
        }
    }

    protected void fireDeleted( List<IdObject<Id, Data>> objects ) {
        if( !objects.isEmpty() ) {
            for( DataListener<Id, Data> dataListener : this.dataListeners ) {
                dataListener.deleted( objects );
            }
        }
    }

    protected void fireDeleted( Id id, Metadata<Data> object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners ) {
            dataListener.deleted( List.of( __io( id, object ) ) );
        }
    }

    protected void firePermanentlyDeleted( Id id, Metadata<Data> object ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners ) {
            dataListener.permanentlyDeleted( __io( id, object ) );
        }
    }

    protected void fireChanged( List<DataListener.IdObject<Id, Data>> added,
                                List<DataListener.IdObject<Id, Data>> updated,
                                List<DataListener.IdObject<Id, Data>> deleted ) {
        for( DataListener<Id, Data> dataListener : this.dataListeners ) {
            dataListener.changed( added, updated, deleted );
        }
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

        public boolean put( @Nonnull I id, @Nonnull T object, String modifiedBy ) {
            requireNonNull( id );
            requireNonNull( object );
            requireNonNull( modifiedBy );

            return lock.synchronizedOn( id, () -> {
                // time: 123 - new Metadata()
                // time: 124 - fsync()
                // time: 125 - data.put( id, metadata )
                // lastmodified must be set after placing the metadata object in the "data"
                final Metadata<T> oldMetadata = data.get( id );
                if( oldMetadata == null ) {
                    Metadata<T> newMetadata = new Metadata<>( object, modifiedBy );
                    data.put( id, newMetadata );
                    newMetadata.refresh();
                } else {
                    oldMetadata.update( object, modifiedBy );
                }
                log.trace( "storing {}", oldMetadata );
                return oldMetadata == null;
            } );
        }

        public Optional<Metadata<T>> remap( @Nonnull I id, @Nonnull Function<T, T> update, String modifiedBy ) {
            return lock.synchronizedOn( id, () ->
                Optional.ofNullable( data.compute( id, ( anId, m ) -> m == null
                    ? null
                    : m.update( update.apply( m.object ), modifiedBy ) ) ) );
        }

        public Optional<Metadata<T>> tryRemap( I id, Function<T, T> tryUpdate, String modifiedBy ) {
            MutableObject<Metadata<T>> ret = new MutableObject<>();

            return lock.synchronizedOn( id, () -> {
                data.compute( id, ( anId, m ) -> {
                        if( m == null ) {
                            return null;
                        }

                        T apply = tryUpdate.apply( m.object );
                        if( apply != null ) {
                            m.update( apply, modifiedBy );
                            ret.setValue( m );
                        }

                        return m;
                    }
                );

                return Optional.ofNullable( ret.getValue() );
            } );
        }

        public List<Pair<I, Metadata<T>>> markDeletedAll() {
            List<Pair<I, Metadata<T>>> ms = selectLive().toList();
            ms.forEach( p -> p._2.delete( MODIFIED_BY_SYSTEM ) );
            return ms;
        }

        public Optional<Metadata<T>> markDeleted( @Nonnull I id, String modifiedBy ) {
            return lock.synchronizedOn( id, () -> {
                Metadata<T> metadata = data.get( id );
                if( metadata != null ) {
                    metadata.delete( modifiedBy );
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
