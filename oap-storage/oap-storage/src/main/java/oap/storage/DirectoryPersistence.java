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
import oap.application.ServiceName;
import oap.concurrent.scheduler.ScheduledExecutorService;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.migration.JsonMetadata;
import oap.storage.migration.Migration;
import oap.storage.migration.MigrationException;
import oap.util.Lists;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.concurrent.Threads.synchronizedOn;
import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

public class DirectoryPersistence<I, T> implements Closeable {
    private final Path path;
    private final BiFunction<Path, T, Path> fsResolve;
    private final int version;
    private final List<Migration> migrations;
    private final Logger log;
    private final Lock lock = new ReentrantLock();
    private final MemoryStorage<I, T> storage;
    @ServiceName
    public String serviceName;
    protected long fsync;
    private volatile ScheduledExecutorService scheduler;
    private volatile long lastExecuted = -1;

    public DirectoryPersistence( Path path, long fsync, int version, List<Migration> migrations, MemoryStorage<I, T> storage ) {
        this( path, plainResolve(), fsync, version, migrations, storage );
    }

    public DirectoryPersistence( Path path, MemoryStorage<I, T> storage ) {
        this( path, plainResolve(), 60000, 0, Lists.of(), storage );
    }

    public DirectoryPersistence( Path path, BiFunction<Path, T, Path> fsResolve, long fsync,
                                 int version, List<Migration> migrations, MemoryStorage<I, T> storage ) {
        this.path = path;
        this.fsResolve = fsResolve;
        this.fsync = fsync;
        this.version = version;
        this.migrations = migrations;
        this.storage = storage;
        this.log = getLogger( toString() );
    }

    public static <T> BiFunction<Path, T, Path> plainResolve() {
        return ( p, object ) -> p;
    }

    public void preStart() {
        scheduler = oap.concurrent.Executors.newScheduledThreadPool( 1, serviceName );
        synchronizedOn( lock, () -> {
            this.load();
            scheduler.scheduleWithFixedDelay( this::fsync, fsync, fsync, TimeUnit.MILLISECONDS );
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

            var metadata = Binder.json.unmarshal( new TypeRef<Metadata<T>>() {}, file ).orElseThrow();

            Path newPath = pathFor( metadata.object );
            var id = storage.identifier.get( metadata.object );
            if( !newPath.equals( file ) ) {
                log.trace( "moving {} => {}", file, newPath );
                Files.delete( file );
                persist( id, metadata );
            }

            storage.memory.put( id, metadata );
        }

        log.info( storage.size() + " object(s) loaded." );
    }

    @SneakyThrows
    private Path migration( Path path ) {
        return synchronizedOn( lock, () -> {
            JsonMetadata oldV = new JsonMetadata( Binder.json.unmarshal( new TypeRef<Map<String, Object>>() {}, path )
                .orElseThrow() );

            Persisted fn = Persisted.valueOf( path );

            log.debug( "migration {}", fn );

            var migration = Lists.find( migrations, m -> m.fromVersion() == fn.version )
                .orElseThrow( () -> new MigrationException( "migration from version " + fn + " not found" ) );


            Path name = fn.toVersion( migration.fromVersion() + 1 );
            JsonMetadata newV = migration.run( oldV );

            long writeLen = -1;
            while( name.toFile().length() != writeLen )
                try( var out = new CountingOutputStream( IoStreams.out( name, PLAIN, DEFAULT_BUFFER, false, true ) ) ) {
                    Binder.json.marshal( out, newV.underlying );
                    writeLen = out.getCount();
                }

            Files.delete( path );
            return name;
        } );

    }

    private void fsync() {
        synchronizedOn( lock, () -> {
            var time = DateTimeUtils.currentTimeMillis();

            log.trace( "fsyncing, last: {}, objects in storage: {}", lastExecuted, storage.size() );
            storage.memory.selectUpdatedSince( lastExecuted - 1 ).forEach( this::persist );

            lastExecuted = time;
        } );
    }

    @SneakyThrows
    private void persist( I id, Metadata<T> metadata ) {
        Path path = pathFor( metadata.object );
        if( metadata.isDeleted() ) {
            log.trace( "delete {}", path );
            Files.delete( path );
            storage.memory.removePermanently( id );
        } else try( OutputStream outputStream = IoStreams.out( path, PLAIN, DEFAULT_BUFFER, false, true ) ) {
            log.trace( "storing {} with modification time {}", path, metadata.modified );
            Binder.json.marshal( outputStream, metadata );
            log.trace( "storing {} done", path );
        }
    }

    @Override
    public void close() {
        log.debug( "closing {}...", this );
        if( scheduler != null && storage != null ) {
            synchronizedOn( lock, () -> {
                Closeables.close( scheduler );
                fsync();
            } );
        } else {
            log.debug( "This {} wasn't started or already closed", this );
        }

        log.debug( "closing {}... Done.", this );
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
}
