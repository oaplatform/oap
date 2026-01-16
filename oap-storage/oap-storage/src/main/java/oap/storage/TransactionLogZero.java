package oap.storage;

import oap.util.Lists;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class TransactionLogZero<Id, T> implements TransactionLog<Id, T> {
    @Override
    public void insert( Id id, Metadata<T> data ) {

    }

    @Override
    public void update( Id id, Metadata<T> data ) {

    }

    @Override
    public void delete( Id id, Metadata<T> data ) {

    }

    @Override
    public ReplicationResult<Id, Metadata<T>> updatedSince( long timestamp, long hash, Set<Map.Entry<Id, Metadata<T>>> fullData ) {
        throw new IllegalStateException();
    }

    private @Nonnull ReplicationResult<Id, Metadata<T>> fullSync( Set<Map.Entry<Id, Metadata<T>>> fullData ) {
        return new ReplicationResult<>( -1, 0L, ReplicationResult.ReplicationStatusType.FULL_SYNC, Lists.map( fullData, d -> new Transaction<>( 0L, Operation.UPDATE, d.getKey(), d.getValue() ) ) );
    }
}
