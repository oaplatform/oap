package oap.storage;

import oap.util.Lists;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionLogImpl<Id, T> implements TransactionLog<Id, T> {
    public final CircularFifoQueue<Transaction<Id, Metadata<T>>> transactions;
    public final AtomicLong timestamp = new AtomicLong();
    public final long hash = System.currentTimeMillis();

    public TransactionLogImpl( int transactionLogSize ) {
        this.transactions = new CircularFifoQueue<>( transactionLogSize );
    }

    @Override
    public synchronized void insert( Id id, Metadata<T> data ) {
        transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.INSERT, id, data ) );
    }

    @Override
    public synchronized void update( Id id, Metadata<T> data ) {
        transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.UPDATE, id, data ) );
    }

    @Override
    public synchronized void delete( Id id, Metadata<T> data ) {
        transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.DELETE, id, data ) );
    }

    @Override
    public synchronized ReplicationResult<Id, Metadata<T>> updatedSince( long timestamp, long hash, Set<Map.Entry<Id, Metadata<T>>> fullData ) {
        int size = transactions.size();
        if( this.hash != hash || size == 0 && timestamp < 0 ) { // first sync && no modification
            return fullSync( fullData, this.timestamp.longValue() );
        }

        Transaction<Id, Metadata<T>> older = transactions.peek();
        if( older == null ) {
            return new ReplicationResult<>( this.timestamp.longValue(), this.hash, ReplicationResult.ReplicationStatusType.CHANGES, List.of() );
        }
        if( older.timestamp > timestamp ) {
            return fullSync( fullData, this.timestamp.longValue() );
        }

        ArrayList<Transaction<Id, Metadata<T>>> list = new ArrayList<>( size );

        for( Transaction<Id, Metadata<T>> transaction : transactions ) {
            if( transaction.timestamp > timestamp ) {
                list.add( transaction );
            }
        }

        return new ReplicationResult<>( this.timestamp.longValue(), this.hash, ReplicationResult.ReplicationStatusType.CHANGES, list );
    }

    private @NonNull ReplicationResult<Id, Metadata<T>> fullSync( Set<Map.Entry<Id, Metadata<T>>> fullData, long t ) {
        return new ReplicationResult<>(
            this.timestamp.longValue(),
            this.hash,
            ReplicationResult.ReplicationStatusType.FULL_SYNC,
            Lists.map( fullData, d -> new Transaction<>( t, Operation.UPDATE, d.getKey(), d.getValue() ) ) );
    }
}
