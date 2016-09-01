package oap.storage;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by macchiatow on 8/30/16.
 */
@Slf4j
public class ChunkedStorage<T> {
   private static final String pattern = "chunk*.gz";

   private final Function<T, String> identify;
   private Path dataLocation;

   public ChunkedStorage( Function<T, String> identify, Path dataLocation ) {
      this.dataLocation = dataLocation;
      this.identify = identify;
   }

   @SuppressWarnings( "unchecked" )
   public void mergeAll( Collection<T> objects, Integer chunkId, BiFunction<T, T, T> remappingFunction ) {
      Path chunkPath = dataLocation.resolve( pattern.replace( "*", String.valueOf( chunkId ) ) );
      Chunk<T> chunk = chunkPath.toFile().exists() ? Binder.json.unmarshal( Chunk.class, chunkPath ) : new Chunk();
      objects.forEach( o -> chunk.records.merge( identify.apply( o ), o, remappingFunction ) );
      Binder.json.marshal( chunkPath, chunk );
   }

   public Stream<T> stream() {
      return Stream.of( Files.fastWildcard( dataLocation, pattern ) )
         .map( f -> Binder.json.unmarshal( Chunk.class, f ) )
         .flatMap( c -> Stream.of( c.records.values() ) );
   }

   private static class Chunk<T> {
      private Map<String, T> records = new HashMap<>();
   }

}
