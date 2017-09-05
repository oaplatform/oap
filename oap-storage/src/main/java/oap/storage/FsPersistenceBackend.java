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
import com.google.common.io.CountingOutputStream;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.storage.migration.FileStorageMigration;
import oap.storage.migration.FileStorageMigrationException;
import oap.storage.migration.JsonMetadata;
import oap.util.Lists;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

class FsPersistenceBackend<T> implements PersistenceBackend<T>, Closeable, Storage.DataListener<T> {
    private final Path path;
    private final BiFunction<Path, T, Path> fsResolve;
    private final int version;
    private final List<FileStorageMigration> migrations;
    private final Logger log;
    private MemoryStorage<T> storage;
    private final Lock lock = new ReentrantLock();

    private PeriodicScheduled scheduled;

    FsPersistenceBackend( Path path, BiFunction<Path, T, Path> fsResolve, long fsync, int version, List<FileStorageMigration> migrations, MemoryStorage<T> storage ) {
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


    @SneakyThrows
    private void load() {

        Threads.synchronously( lock, () -> {
            Files.ensureDirectory( path );
            List<Path> paths = Files.deepCollect( path, p -> p.getFileName().toString().endsWith( ".json" ) );
            log.debug( "found {} files", paths.size() );

            for( Path file : paths ) {
                final Persisted persisted = Persisted.valueOf( file );

                for( long version = persisted.version; version < this.version; version++ ) {
                    file = migration( file );
                }

                final Metadata<T> unmarshal = Binder.json.unmarshal( new TypeReference<Metadata<T>>() {
                }, file );

                final Path newPath = filenameFor( unmarshal.object, this.version );

                if( !java.nio.file.Files.exists( newPath ) || !java.nio.file.Files.isSameFile( file, newPath ) ) {
                    Files.move( file, newPath, StandardCopyOption.REPLACE_EXISTING );
                }

                storage.data.put( unmarshal.id, unmarshal );

            }

            log.info( storage.data.size() + " object(s) loaded." );
        } );
    }

    @SneakyThrows
    private Path migration( Path path ) {

        return Threads.synchronously( lock, () -> {
            JsonMetadata oldV = new JsonMetadata( Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {
            }, path ) );

            Persisted fn = Persisted.valueOf( path );

            log.debug( "migration {}", fn );

            val migration = Lists.find2( migrations, m -> m.fromVersion() == fn.version );
            if( migration == null )
                throw new FileStorageMigrationException( "migration from version " + fn + " not found" );


            Path name = fn.toVersion( migration.fromVersion() + 1 );
            JsonMetadata newV = migration.run( oldV );

            long writeLen = -1;
            while( name.toFile().length() != writeLen ) {
                try( val out = new CountingOutputStream( IoStreams.out( name, PLAIN, DEFAULT_BUFFER, false, true ) ) ) {
                    Binder.json.marshal( out, newV.underlying );
                    writeLen = out.getCount();
                }
            }

            Files.delete( path );
            return name;
        } );

    }

    @SneakyThrows
    private void fsync( long last ) {

        Threads.synchronously( lock, () -> {
            log.trace( "fsync: last: {}, storage size: {}", last, storage.data.size() );

            for( val value : storage.data.values() ) {
                if( value.modified < last ) continue;

                final Path fn = filenameFor( value.object, version );
                try( OutputStream outputStream = IoStreams.out( fn, PLAIN, DEFAULT_BUFFER, false, true ) ) {
                    log.trace( "fsync storing {} with modification time {}", fn.getFileName(), value.modified );
                    Binder.json.marshal( outputStream, value );
                    log.trace( "fsync storing {} done", fn.getFileName() );
                }
            }
        } );
    }

    public void delete( T id ) {
        Threads.synchronously( lock, () -> {
            Path path = filenameFor( id, version );
            Files.delete( path );
        } );
    }

    @Override
    public void close() {
        Threads.synchronously( lock, () -> {
            Scheduled.cancel( scheduled );
            fsync( scheduled.lastExecuted() );
        } );
    }

    private Path filenameFor( T object, long version ) {
        return Threads.synchronously( lock, () -> {
            final String ver = this.version > 0 ? ".v" + version : "";
            return fsResolve.apply( this.path, object )
                .resolve( this.storage.identifier.get( object ) + ver + ".json" );
        } );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + path;
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

        Path toVersion( long version ) {
            return path.resolve( id + ".v" + version + ".json" );
        }
    }
}
