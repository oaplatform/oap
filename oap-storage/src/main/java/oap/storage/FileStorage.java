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
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.storage.migration.FileStorageMigration;
import oap.storage.migration.FileStorageMigrationException;
import oap.storage.migration.JsonMetadata;
import oap.util.Stream;
import oap.util.Try;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;

@Slf4j
public class FileStorage<T> implements Storage<T>, Closeable {
    private static final int VERSION = 0;
    private static final Pattern PATTERN_VERSION = Pattern.compile( ".+\\.v(\\d+)\\.json" );

    private final AtomicLong lastFSync = new AtomicLong( 0 );
    private final AtomicLong lastRSync = new AtomicLong( 0 );
    private final Storage<T> master;
    private final Scheduled rsync;
    private final Scheduled fsync;
    private final long version;
    private final List<FileStorageMigration> migrations;
    protected long rsyncSafeInterval = 1000;
    protected Function<T, String> identify;
    protected ConcurrentMap<String, Metadata<T>> data = new ConcurrentHashMap<>();
    private Path path;
    private List<DataListener<T>> dataListeners = new ArrayList<>();

    public FileStorage( Path path, Function<T, String> identify, long fsync, Storage<T> master, long rsync ) {
        this( path, identify, fsync, master, rsync, VERSION, emptyList() );
    }

    public FileStorage( Path path, Function<T, String> identify, long fsync, Storage<T> master, long rsync, long version, List<String> migrations ) {
        this.path = path;
        this.identify = identify;
        this.version = version;
        this.migrations = migrations
            .stream()
            .map( Try.map( cn -> ( FileStorageMigration ) Class.forName( cn ).newInstance() ) )
            .collect( toList() );

        load();
        this.fsync = fsync > 0 ? Scheduler.scheduleWithFixedDelay( fsync, MILLISECONDS, this::fsync ) : null;
        this.master = master;
        this.rsync = master != null ? Scheduler.scheduleWithFixedDelay( rsync, MILLISECONDS, this::rsync ) : null;
    }

    public FileStorage( Path path, Function<T, String> identify, long fsync ) {
        this( path, identify, fsync, null, 0, VERSION, emptyList() );
    }

    public FileStorage( Path path, Function<T, String> identify, long fsync, long version, List<String> migrations ) {
        this( path, identify, fsync, null, 0, version, migrations );
    }

    public FileStorage( Path path, Function<T, String> identify ) {
        this( path, identify, VERSION, emptyList() );
    }

    public FileStorage( Path path, Function<T, String> identify, long version, List<String> migrations ) {
        this( path, identify, 60000, version, migrations );
    }

    @SuppressWarnings( "unchecked" )
    protected void load() {
        path.toFile().mkdirs();
        data = Files.wildcard( path, "*.json" )
            .stream()
            .map( Try.map(
                f -> {
                    long version = getVersion( f.getFileName().toString() );

                    Path file = f;
                    for( long v = version; v < this.version; v++ ) file = migration( file );

                    return ( Metadata<T> ) Binder.json.unmarshal( new TypeReference<Metadata<T>>() {
                    }, file );
                } ) )
            .sorted( reverseOrder() )
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
        log.info( data.size() + " object(s) loaded." );
    }

    private long getVersion( String fileName ) {
        final Matcher matcher = PATTERN_VERSION.matcher( fileName );
        return matcher.matches() ? Long.parseLong( matcher.group( 1 ) ) : 0;
    }

    private Path migration( Path path ) {
        final JsonMetadata oldV = new JsonMetadata( Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {
        }, path ) );

        log.debug( "migration {}", path );

        final String id = oldV.id();

        final Optional<FileStorageMigration> any = migrations
            .stream()
            .filter( m -> m.fromVersion() == oldV.version() )
            .findAny();

        return any.map( m -> {
            final Path fn = fileName( id, m.fromVersion() + 1 );

            final JsonMetadata newV = m.run( oldV );

            newV.incVersion();
            Binder.json.marshal( fn, newV.underlying );
            Files.delete( path );

            return fn;
        } ).orElseThrow( () -> new FileStorageMigrationException( "migration from version " + oldV.version() + " not found" ) );
    }

    private synchronized void fsync() {
        long current = DateTimeUtils.currentTimeMillis();
        long last = lastFSync.get();
        log.trace( "fsync current: {}, last: {}, storage size: {}", current, last, data.size() );

        data.values()
            .stream()
            .filter( m -> m.modified >= last )
            .forEach( m -> {
                final Path fn = fileName( m.id, version );
                log.trace( "fsync storing {} with modification time {}", fn.getFileName(), m.modified );
                Binder.json.marshal( fn, m );
                log.trace( "fsync storing {} done", fn.getFileName() );

            } );

        lastFSync.set( current );
    }

    private Path fileName( String id, long version ) {
        final String ver = this.version > 0 ? ".v" + version : "";
        return path.resolve( id + ver + ".json" );
    }

    private void rsync() {
        long current = DateTimeUtils.currentTimeMillis() - rsyncSafeInterval;
        long last = lastRSync.get();
        List<Metadata<T>> updates = master.updatedSince( last );
        log.trace( "rsync current: {}, last: {}, to sync {}", current, last, updates.size() );
        for( Metadata<T> metadata : updates ) {
            log.debug( "rsync {}", metadata );
            data.put( metadata.id, metadata );
        }
        fireUpdated( Stream.of( updates ).map( m -> m.object ).toList() );
        lastRSync.set( current );
    }


    @Override
    public Stream<T> select() {
        return Stream.of( data.values() )
            .filter( m -> !m.deleted )
            .map( m -> m.object );
    }

    @Override
    public void store( T object ) {
        String id = this.identify.apply( object );
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) {
                metadata.update( object );
                metadata.deleted = false;
            } else {
                data.put( id, new Metadata<>( id, object ) );
            }
            fireUpdated( object );
        }
    }

    @Override
    public void store( Collection<T> objects ) {
        for( T object : objects ) {
            String id = this.identify.apply( object );
            synchronized( id.intern() ) {
                Metadata<T> metadata = data.get( id );
                if( metadata != null ) {
                    metadata.update( object );
                    metadata.deleted = false;
                } else {
                    data.put( id, new Metadata<>( id, object ) );
                }
            }
        }
        fireUpdated( objects );
    }

    @Override
    public T update( String id, Consumer<T> update ) {
        return update( id, update, null );
    }

    @Override
    public T update( String id, Consumer<T> update, Supplier<T> init ) {
        return update( id, update, init, true );
    }

    private T update( String id, Consumer<T> update, Supplier<T> init, boolean notification ) {
        synchronized( id.intern() ) {
            Metadata<T> m = data.get( id );
            if( m == null ) {
                if( init == null ) return null;
                T object = init.get();
                m = new Metadata<>( identify.apply( object ), object );
                data.put( m.id, m );
            } else {
                update.accept( m.object );
                m.update( m.object );
            }
            if( notification ) fireUpdated( m.object );
            return m.object;
        }
    }

    @Override
    public void update( Collection<String> ids, Consumer<T> update ) {
        update( ids, update, null );
    }

    @Override
    public void update( Collection<String> ids, Consumer<T> update, Supplier<T> init ) {
        final List<T> collect = ids.stream().map( id -> update( id, update, init, false ) ).collect( toList() );

        fireUpdated( collect );
    }

    @Override
    public Optional<T> get( String id ) {
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata == null || metadata.deleted ) {
                return Optional.empty();
            } else return Optional.of( metadata.object );
        }
    }

    @Override
    public void removeAll() {
        List<T> objects = new ArrayList<>();
        for( String id : data.keySet() ) remove( id, false ).ifPresent( m -> objects.add( m.object ) );
        fireDeleted( objects );
    }


    void vacuum() {
        data.values()
            .stream()
            .filter( metadata -> metadata.deleted )
            .forEach( metadata -> remove( metadata.id, true ) );
    }

    @Override
    public void clear() {
        removeAll();
        vacuum();
    }

    private Optional<Metadata<T>> remove( String id, boolean expunge ) {
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) {
                if( !expunge && !metadata.deleted ) {
                    metadata.delete();
                    return Optional.of( metadata );
                } else if( expunge ) {
                    data.remove( metadata.id );
                    Path resolve = path.resolve( metadata.id + ".json" );
                    if( java.nio.file.Files.exists( resolve ) ) Files.delete( resolve );
                    return Optional.of( metadata );
                }
            }
            return Optional.empty();
        }
    }

    public void delete( String id ) {
        remove( id, false ).ifPresent( m -> fireDeleted( m.object, false ) );
    }

    public void expunge( String id ) {
        remove( id, true ).ifPresent( m -> fireDeleted( m.object, true ) );
    }

    @Override
    public long size() {
        return data.size();
    }

    protected void fireUpdated( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.updated( object );
    }

    protected void fireUpdated( Collection<T> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.updated( objects );
    }


    protected void fireDeleted( T object, boolean expunge ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( object, expunge );
    }

    protected void fireDeleted( List<T> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.deleted( objects );
    }

    public void addDataListener( DataListener<T> dataListener ) {
        this.dataListeners.add( dataListener );
    }

    public void removeDataListener( DataListener<T> dataListener ) {
        this.dataListeners.remove( dataListener );
    }

    @Override
    public List<Metadata<T>> updatedSince( long time ) {
        return Stream.of( data.values() ).filter( m -> m.modified > time ).toList();
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public synchronized void close() {
        Scheduled.cancel( rsync );
        Scheduled.cancel( fsync );
        fsync();
        data.clear();
    }

    public interface DataListener<T> {
        default void updated( T object ) {
        }


        default void updated( Collection<T> objects ) {
        }


        default void deleted( T object, boolean expunge ) {
        }

        default void deleted( Collection<T> objects ) {
        }

    }
}
