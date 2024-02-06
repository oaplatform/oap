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

@Slf4j
public class ChunkedStorage<T> {
    private static final String pattern = "chunk*.gz";

    private final Function<T, String> identify;
    private final Path dataLocation;

    public ChunkedStorage( Function<T, String> identify, Path dataLocation ) {
        this.dataLocation = dataLocation;
        this.identify = identify;
    }

    @SuppressWarnings( "unchecked" )
    public void mergeAll( Collection<T> objects, Integer chunkId, BiFunction<T, T, T> remappingFunction ) {
        Path chunkPath = dataLocation.resolve( pattern.replace( "*", String.valueOf( chunkId ) ) );
        Chunk chunk = chunkPath.toFile().exists() ? Binder.json.unmarshal( Chunk.class, chunkPath ) : new Chunk();
        objects.forEach( o -> chunk.records.merge( identify.apply( o ), o,
            ( a, b ) -> remappingFunction.apply( ( T ) a, ( T ) b ) ) );
        Binder.json.marshal( chunkPath, chunk );
    }

    @SuppressWarnings( "unchecked" )
    public Stream<T> stream() {
        return Stream.of( Files.fastWildcard( dataLocation, pattern ) )
            .map( f -> Binder.json.unmarshal( Chunk.class, f ) )
            .flatMap( c -> Stream.of( ( Collection<T> ) c.records.values() ) );
    }

    private static class Chunk {
        private final Map<String, Object> records = new HashMap<>();

        private Chunk( Map<String, Object> records ) {
            this.records.putAll( records );
        }

        private Chunk() {
        }
    }

}
