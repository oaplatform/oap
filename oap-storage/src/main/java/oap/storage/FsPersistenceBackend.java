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
import lombok.ToString;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.storage.migration.FileStorageMigration;
import oap.storage.migration.FileStorageMigrationException;
import oap.storage.migration.JsonMetadata;
import oap.util.Try;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.reverseOrder;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;
import static org.slf4j.LoggerFactory.getLogger;

class FsPersistenceBackend<T> implements PersistenceBackend<T>, Closeable, Storage.DataListener<T> {
    private final Path path;
    private final BiFunction<Path, T, Path> fsResolve;
    private final int version;
    private final List<FileStorageMigration> migrations;
    private final Logger log;
    private MemoryStorage<T> storage;
    private PeriodicScheduled scheduled;

    public FsPersistenceBackend( Path path, BiFunction<Path, T, Path> fsResolve, long fsync, int version, List<FileStorageMigration> migrations, MemoryStorage<T> storage ) {
        this.path = path;
        this.fsResolve = fsResolve;
        this.version = version;
        this.migrations = migrations;
        this.storage = storage;
        this.log = getLogger( toString() );
        this.load();
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
        this.storage.addDataListener( this );
    }

    private void load() {
        Files.ensureDirectory( path );
        List<Path> paths = Files.deepCollect( path, p -> p.getFileName().toString().endsWith( ".json" ) );
        log.debug( "found {} files", paths.size() );

        storage.data = paths
            .stream()
            .map( Try.map(
                f -> {
                    final Persisted persisted = Persisted.valueOf( f );

                    Path file = f;
                    for( long version = persisted.version; version < this.version; version++ ) {
                        file = migration( file );
                    }

                    final Metadata<T> unmarshal = Binder.json.unmarshal( new TypeReference<Metadata<T>>() {}, file );

                    final Path newPath = filenameFor( unmarshal.object, this.version );

                    if ( java.nio.file.Files.exists( f )
                        && !java.nio.file.Files.isDirectory( f )
                        && !newPath.toString().equals( f.toString() ) ) {

                        Files.move(f, newPath, StandardCopyOption.REPLACE_EXISTING );
                    }

                    return unmarshal;
                } ) )
            .sorted( reverseOrder() )
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
        log.info( storage.data.size() + " object(s) loaded." );
    }

    private Path migration( Path path ) {
        JsonMetadata oldV = new JsonMetadata( Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {
        }, path ) );

        Persisted fn = Persisted.valueOf( path );

        log.debug( "migration {}", fn );

        Optional<FileStorageMigration> migration = migrations
            .stream()
            .filter( m -> m.fromVersion() == fn.version )
            .findAny();

        return migration
            .map( m -> {
                Path name = fn.toVersion( m.fromVersion() + 1 );
                JsonMetadata newV = m.run( oldV );
                Binder.json.marshal( name, newV.underlying );
                Files.delete( path );
                return name;
            } )
            .orElseThrow( () -> new FileStorageMigrationException( "migration from version " + fn + " not found" ) );
    }

    private synchronized void fsync( long last ) {
        log.trace( "fsync: last: {}, storage size: {}", last, storage.data.size() );

        storage.data.values()
            .stream()
            .filter( m -> m.modified >= last )
            .forEach( m -> {
                final Path fn = filenameFor( m.object, version );
                log.trace( "fsync storing {} with modification time {}", fn.getFileName(), m.modified );
                Binder.json.marshal( fn, m );
                log.trace( "fsync storing {} done", fn.getFileName() );

            } );
    }

    //todo refactor to Persisted
    private Path filenameFor( T object, long version ) {
        final String ver = this.version > 0 ? ".v" + version : "";
        return fsResolve.apply( this.path, object )
            .resolve( this.storage.identifier.getOrInit( object, storage ) + ver + ".json" );
    }

    public synchronized void delete( T id ) {
        Path path = filenameFor( id, version );
        Files.delete( path );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + path;
    }

    @Override
    public synchronized void close() {
        Scheduled.cancel( scheduled );
        fsync( scheduled.lastExecuted() );
    }

    @Override
    public void deleted( T object ) {
        delete( object );
    }

    @Override
    public void deleted( Collection<T> objects ) {
        objects.forEach( this::deleted );
    }

    @ToString
    private static class Persisted {
        private static final Pattern PATTERN_VERSION = Pattern.compile( "(.+)\\.v(\\d+)\\.json" );
        private final Path path;
        private final String id;
        private final long version;

        Persisted( Path path, String id, long version ) {
            this.path = path;
            this.id = id;
            this.version = version;
        }

        static Persisted valueOf( Path path ) {
            String name = path.getFileName().toString();
            final Matcher matcher = PATTERN_VERSION.matcher( name );
            return matcher.matches()
                ? new Persisted( path.getParent(), matcher.group( 1 ), Long.parseLong( matcher.group( 2 ) ) )
                : new Persisted( path.getParent(), name.substring( 0, name.length() - ".json".length() ), 0L );
        }


        public Path toVersion( long version ) {
            return path.resolve( id + ".v" + version + ".json" );
        }
    }
}
