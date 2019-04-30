/*
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

import com.google.common.io.CountingOutputStream;
import lombok.SneakyThrows;
import lombok.ToString;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.migration.JsonMetadata;
import oap.storage.migration.Migration;
import oap.storage.migration.MigrationException;
import oap.util.Lists;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

public class DirectoryPersistence<T> implements Closeable, Storage.DataListener<T> {
    private final Path path;
    private final BiFunction<Path, T, Path> fsResolve;
    private long fsync;
    private final int version;
    private final List<Migration> migrations;
    private final Logger log;
    private final Lock lock = new ReentrantLock();
    private MemoryStorage<T> storage;
    private PeriodicScheduled scheduled;

    public DirectoryPersistence( Path path, long fsync, int version, List<Migration> migrations, MemoryStorage<T> storage ) {
        this( path, plainResolve(), fsync, version, migrations, storage );
    }

    public DirectoryPersistence( Path path, MemoryStorage<T> storage ) {
        this( path, plainResolve(), 60000, 0, Lists.of(), storage );
    }

    public DirectoryPersistence( Path path, BiFunction<Path, T, Path> fsResolve, long fsync,
                                 int version, List<Migration> migrations, MemoryStorage<T> storage ) {
        this.path = path;
        this.fsResolve = fsResolve;
        this.fsync = fsync;
        this.version = version;
        this.migrations = migrations;
        this.storage = storage;
        this.log = getLogger( toString() );
    }


    public void start() {
        Threads.synchronously( lock, () -> {
            this.load();
            this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
            this.storage.addDataListener( this );
        } );
    }

    @SneakyThrows
    private void load() {
        log.debug( "loading data from {}", path );

        Files.ensureDirectory( path );
        List<Path> paths = Files.deepCollect( path, p -> p.getFileName().toString().endsWith( ".json" ) );
        log.debug( "found {} files", paths.size() );

        for( Path file : paths ) {
            Persisted persisted = Persisted.valueOf( file );

            for( long version = persisted.version; version < this.version; version++ ) file = migration( file );

            Metadata<T> metadata = Binder.json.unmarshal( new TypeRef<>() {}, file );

            Path newPath = pathFor( metadata.object );
            if( !newPath.equals( file ) ) {
                log.trace( "moving {} => {}", file, newPath );
                Files.delete( file );
                persist( metadata );
            }

            var id = storage.identifier.get( metadata.object );

            storage.data.put( id, metadata );
        }

        log.info( storage.data.size() + " object(s) loaded." );
    }

    @SneakyThrows
    private Path migration( Path path ) {

        return Threads.synchronously( lock, () -> {
            JsonMetadata oldV = new JsonMetadata( Binder.json.unmarshal( new TypeRef<>() {
            }, path ) );

            Persisted fn = Persisted.valueOf( path );

            log.debug( "migration {}", fn );

            var migration = Lists.find2( migrations, m -> m.fromVersion() == fn.version );
            if( migration == null )
                throw new MigrationException( "migration from version " + fn + " not found" );


            Path name = fn.toVersion( migration.fromVersion() + 1 );
            JsonMetadata newV = migration.run( oldV );

            long writeLen = -1;
            while( name.toFile().length() != writeLen ) {
                try( var out = new CountingOutputStream( IoStreams.out( name, PLAIN, DEFAULT_BUFFER, false, true ) ) ) {
                    Binder.json.marshal( out, newV.underlying );
                    writeLen = out.getCount();
                }
            }

            Files.delete( path );
            return name;
        } );

    }

    private void fsync( long last ) {
        Threads.synchronously( lock, () -> {
            log.trace( "fsyncing, last: {}, storage length: {}", last, storage.data.size() );
            for( var value : storage.data.values() )
                if( value.modified >= last ) persist( value );
        } );
    }

    @SneakyThrows
    private void persist( Metadata<T> value ) {
        Path path = pathFor( value.object );
        try( OutputStream outputStream = IoStreams.out( path, PLAIN, DEFAULT_BUFFER, false, true ) ) {
            log.trace( "storing {} with modification time {}", path, value.modified );
            Binder.json.marshal( outputStream, value );
            log.trace( "storing {} done", path );
        }
    }

    @Override
    public void close() {
        log.debug( "closing {}...", this );
        if( scheduled != null && storage != null ) {
            Threads.synchronously( lock, () -> {
                Scheduled.cancel( scheduled );
                fsync( scheduled.lastExecuted() );
                storage.close();
            } );
        } else {
            log.debug( "This {} was't started or already closed", this );
        }
    }

    @Override
    public void fsync() {
        fsync( scheduled.lastExecuted() );
    }

    private Path pathFor( T object ) {
        String ver = this.version > 0 ? ".v" + this.version : "";
        return fsResolve.apply( this.path, object )
            .resolve( this.storage.identifier.get( object ) + ver + ".json" );
    }

    @Override
    public String toString() {
        return String.join( "/", getClass().getSimpleName(), path.toString(), Integer.toString( hashCode() ) );
    }

    @Override
    public void deleted( T object ) {
        Threads.synchronously( lock, () -> Files.delete( pathFor( object ) ) );
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
            Matcher matcher = PATTERN_VERSION.matcher( name );
            return matcher.matches()
                ? new Persisted( path.getParent(), matcher.group( 1 ), Long.parseLong( matcher.group( 2 ) ) )
                : new Persisted( path.getParent(), name.substring( 0, name.length() - ".json".length() ), 0L );
        }

        Path toVersion( long version ) {
            return path.resolve( id + ".v" + version + ".json" );
        }
    }

    public static <T> BiFunction<Path, T, Path> plainResolve() {
        return ( p, object ) -> p;
    }
}
