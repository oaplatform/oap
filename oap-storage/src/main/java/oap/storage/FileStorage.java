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
import lombok.val;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.storage.migration.FileStorageMigration;
import oap.storage.migration.FileStorageMigrationException;
import oap.storage.migration.JsonMetadata;
import oap.util.Lists;
import oap.util.Stream;
import oap.util.Try;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;
import static org.slf4j.LoggerFactory.getLogger;


public class FileStorage<T> extends MemoryStorage<T> implements Closeable, ReplicationMaster<T> {
   private static final int VERSION = 0;
   private static final Pattern PATTERN_VERSION = Pattern.compile( ".+\\.v(\\d+)\\.json" );

   private final AtomicLong lastFSync = new AtomicLong( 0 );
   private final long fsync;
   private final int version;
   private final List<FileStorageMigration> migrations;
   private Scheduled fsyncScheduled;
   private Path path;
   private final Logger log;

   public FileStorage( Path path, Function<T, String> identify, long fsync, int version, List<String> migrations ) {
      super( identify );
      this.path = path;
      this.fsync = fsync;
      this.version = version;
      this.migrations = Lists.map( migrations,
         Try.map( clazz -> ( FileStorageMigration ) Class.forName( clazz ).newInstance() ) );
      this.log = getLogger( toString() );
   }

   public FileStorage( Path path, Function<T, String> identify, long fsync ) {
      this( path, identify, fsync, VERSION, emptyList() );
   }

   public FileStorage( Path path, Function<T, String> identify ) {
      this( path, identify, VERSION, emptyList() );
   }

   public FileStorage( Path path, Function<T, String> identify, int version, List<String> migrations ) {
      this( path, identify, 60000, version, migrations );
   }

   public void start() {
      load();
      this.fsyncScheduled = Scheduler.scheduleWithFixedDelay( fsync, MILLISECONDS, this::fsync );
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

      val version = getVersion( path.getFileName().toString() );

      log.debug( "migration {}", path );

      final String id = oldV.id();

      final Optional<FileStorageMigration> any = migrations
         .stream()
         .filter( m -> m.fromVersion() == version )
         .findAny();

      return any.map( m -> {
         final Path fn = fileName( id, m.fromVersion() + 1 );

         final JsonMetadata newV = m.run( oldV );

         Binder.json.marshal( fn, newV.underlying );
         Files.delete( path );

         return fn;
      } ).orElseThrow( () -> new FileStorageMigrationException( "migration from version " + version + " not found" ) );
   }

   @Override
   protected Optional<Metadata<T>> deleteObject( String id ) {
      Optional<Metadata<T>> metadata = super.deleteObject( id );
      metadata.ifPresent( m -> Files.delete( fileName( id, version ) ) );
      return metadata;
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


   @Override
   public synchronized void close() {
      Scheduled.cancel( fsyncScheduled );
      fsync();
      data.clear();
   }

   @Override
   public List<Metadata<T>> updatedSince( long time ) {
      return Stream.of( data.values() ).filter( m -> m.modified > time ).toList();
   }

   @Override
   public List<String> ids() {
      return new ArrayList<>( data.keySet() );
   }

   @Override
   public String toString() {
      return FileStorage.class.getName() + "[" + path + "]";
   }
}
