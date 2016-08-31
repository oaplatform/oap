package oap.storage;

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.json.Binder;
import oap.util.Stream;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by macchiatow on 8/30/16.
 */
@Slf4j
public class ChunkedStorage<T> implements Closeable {
   private final Map<Integer, Chunk> data = new ConcurrentHashMap<>();
   private final int chunks;
   private final long fsync;

   private final AtomicLong lastFSync = new AtomicLong( 0 );
   private final Function<String, Integer> hasher;
   private final Function<Integer, Path> chunkPath;
   private final Function<T, String> identity;

   private Scheduled fsyncScheduled;

   public ChunkedStorage( Function<T, String> identity, Path dataLocation, int chunks, long fsync ) {
      this.hasher = id -> Math.abs( Hashing.murmur3_32().hashBytes( id.getBytes() ).asInt() ) % chunks;
      this.chunkPath = i -> dataLocation.resolve( "chunk" + i + ".gz" );
      this.chunks = chunks;
      this.fsync = fsync;
      this.identity = identity;
   }

   public void put( T object ) {
      String id = identity.apply( object );
      Objects.requireNonNull( id, "[id] must not be null" );
      Integer chunkId = hasher.apply( id );
      this.put( object, chunkId );
   }

   public void put( T object, Integer chunkId ) {
      String id = identity.apply( object );
      data.computeIfAbsent( chunkId, s -> new Chunk() ).records.put( id, object );
   }

   public T get( String id ) {
      Objects.requireNonNull( id, "[id] must not be null" );
      Integer chunkId = hasher.apply( id );
      return this.get( id, chunkId );
   }

   public T get( String id, Integer chunkId ) {
      return Optional.ofNullable( data.get( chunkId ) ).map( c -> c.records.get( id ) ).orElse( null );
   }

   public Stream<T> stream() {
      return Stream.of( data.values() )
         .flatMap( chunk -> chunk.records.values().stream() );
   }

   public void start() {
      for( int c = 0; c < chunks; c++ ) {
         Path path = chunkPath.apply( c );
         if( path.toFile().exists() ) {
            data.put( c, Binder.json.unmarshal( Chunk.class, path ) );
         }
      }
      this.fsyncScheduled = Scheduler.scheduleWithFixedDelay( fsync, MILLISECONDS, this::fsync );
   }

   @Override
   public synchronized void close() {
      Scheduled.cancel( fsyncScheduled );
      fsync();
      data.clear();
   }

   private synchronized void fsync() {
      long current = DateTimeUtils.currentTimeMillis();
      long last = lastFSync.get();
      log.trace( "fsync current: {}, last: {}, data size: {}", current, last, stream().count() );

      data.entrySet().forEach( chunk -> {
         log.trace( "storing chunk {} with size {} profiles", chunk.getKey(), chunk.getValue().records.size() );
         Binder.json.marshal( chunkPath.apply( chunk.getKey() ), chunk.getValue() );
         log.trace( "storing chunk {} done", chunk.getKey() );
      } );

      lastFSync.set( current );
   }

   private class Chunk {
      Map<String, T> records = new HashMap<>();
   }

}
