package oap.storage;

import oap.util.CircularFifoQueue;
import oap.util.Lists;
import org.joda.time.DateTimeUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionLogImpl<Id, T> implements TransactionLog<Id, T> {
    public final CircularFifoQueue<Transaction<Id, Metadata<T>>> transactions;
    public final AtomicLong timestamp = new AtomicLong();
    public final ReentrantLock lock = new ReentrantLock();
    public long hash = DateTimeUtils.currentTimeMillis();

    public TransactionLogImpl( int transactionLogSize ) {
        this.transactions = new CircularFifoQueue<>( transactionLogSize );
    }

    @Override
    public void insert( Id id, Metadata<T> data ) {
        lock.lock();
        try {
            transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.INSERT, id, data ) );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update( Id id, Metadata<T> data ) {
        lock.lock();
        try {
            transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.UPDATE, id, data ) );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete( Id id, Metadata<T> data ) {
        lock.lock();
        try {
            transactions.add( new Transaction<>( timestamp.incrementAndGet(), Operation.DELETE, id, data ) );
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ReplicationResult<Id, Metadata<T>> updatedSince( long timestamp, long hash, Set<Map.Entry<Id, Metadata<T>>> fullData ) {
        lock.lock();
        try {
            int size = transactions.size();
            if( this.hash != hash || timestamp < 0 ) { // first sync && no modification
                return fullSync( fullData, this.timestamp.longValue() );
            }

            Transaction<Id, Metadata<T>> older = transactions.peek();
            if( older == null ) {
                return new ReplicationResult<>( this.timestamp.longValue(), this.hash, ReplicationResult.ReplicationStatusType.CHANGES, List.of() );
            }
            if( older.timestamp > timestamp ) {
                return fullSync( fullData, this.timestamp.longValue() );
            }

            LinkedHashMap<Id, Transaction<Id, Metadata<T>>> list = new LinkedHashMap<>( size );

            Iterator<Transaction<Id, Metadata<T>>> reverseIterator = transactions.reverseIterator();
            while( reverseIterator.hasNext() ) {
                Transaction<Id, Metadata<T>> transaction = reverseIterator.next();

                if( transaction.timestamp > timestamp ) {
                    list.putIfAbsent( transaction.id, transaction );
                } else {
                    break;
                }
            }

            return new ReplicationResult<>( this.timestamp.longValue(), this.hash, ReplicationResult.ReplicationStatusType.CHANGES, new ArrayList<>( list.values() ) );
        } finally {
            lock.unlock();
        }
    }

    private @NonNull ReplicationResult<Id, Metadata<T>> fullSync( Set<Map.Entry<Id, Metadata<T>>> fullData, long t ) {
        return new ReplicationResult<>(
            this.timestamp.longValue(),
            this.hash,
            ReplicationResult.ReplicationStatusType.FULL_SYNC,
            Lists.map( fullData, d -> new Transaction<>( t, Operation.UPDATE, d.getKey(), d.getValue() ) ) );
    }

    public void reset() {
        hash = DateTimeUtils.currentTimeMillis();
        timestamp.set( 0 );
        transactions.clear();
    }
}
