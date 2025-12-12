package oap.storage;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TransactionLog<Id, T> {
    void insert( Id id, Metadata<T> data );

    void update( Id id, Metadata<T> data );

    void delete( Id id, Metadata<T> data );

    ReplicationResult<Id, Metadata<T>> updatedSince( long timestamp, Set<Map.Entry<Id, Metadata<T>>> fullData );

    enum Operation {
        INSERT,
        UPDATE,
        DELETE
    }

    @ToString
    class Transaction<Id, T> implements Serializable {
        @Serial
        private static final long serialVersionUID = 6671170052040953303L;
        public final long timestamp;
        public final Operation operation;
        public final Id id;
        public final T object;

        public Transaction( long timestamp, Operation operation, Id id, T object ) {
            this.timestamp = timestamp;
            this.operation = operation;
            this.id = id;
            this.object = object;
        }
    }

    class ReplicationResult<Id, T> implements Serializable {
        @Serial
        private static final long serialVersionUID = 467235422368462526L;

        public final long timestamp;
        public final ReplicationStatusType type;
        public final List<Transaction<Id, T>> data;

        public ReplicationResult( long timestamp, ReplicationStatusType type, List<Transaction<Id, T>> data ) {
            this.timestamp = timestamp;
            this.type = type;
            this.data = data;
        }

        public enum ReplicationStatusType {
            FULL_SYNC,
            CHANGES
        }
    }
}
