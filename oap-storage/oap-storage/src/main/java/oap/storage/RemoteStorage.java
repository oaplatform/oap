package oap.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public interface RemoteStorage<Id, Data> extends ReplicationMaster<Id, Data> {
    Optional<Data> get( @Nonnull Id id );

    Optional<Metadata<Data>> getMetadata( @Nonnull Id id );

    long size();

    /**
     * @param object
     * @return created object or null if the hash does not match
     * @see RemoteStorage#getMetadata(Id)
     */
    @Nullable
    Data store( @Nonnull Data object );

    default Data findAndModify( Id id, Function<Data, Data> func, int retry ) throws StorageException {
        requireNonNull( id );

        Data stored = null;

        for( int i = 0; i < retry && stored == null; i++ ) {
            Metadata<Data> metadata = getMetadata( id ).orElse( null );

            Data modified = func.apply( metadata != null ? metadata.object : null );
            requireNonNull( modified );

            stored = store( modified );
        }

        if( stored == null ) {
            throw new StorageException();
        }

        return stored;
    }
}
