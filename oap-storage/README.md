# oap-storage

Persistent, in-memory storage layer for the OAP platform. Objects are kept in a `ConcurrentHashMap`-backed `MemoryStorage` and optionally synced to MongoDB or cloud object stores.

## Architecture

```
                  ┌─────────────────────────────┐
                  │      MemoryStorage<Id,Data>  │
                  │  (ConcurrentHashMap + Lock)  │
                  └──────────┬──────────────────┘
                             │  TransactionLog (change log)
              ┌──────────────┴──────────────────┐
              │                                 │
   MongoPersistence<I,T>          ReplicationMaster / RemoteStorage
   (periodic bulk write,          (diff-based replication
    change stream watch)           between nodes)
```

Cloud storage (`FileSystem`) is a separate, stateless API over object stores — it does not integrate with `MemoryStorage`.

## Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-storage](oap-storage/README.md) | `Storage<Id,Data>`, `MemoryStorage`, `Metadata`, `DataListener`, `Migration` | `oap-stdlib` |
| [oap-storage-mongo](oap-storage-mongo/README.md) | `MongoPersistence`, `MongoClient`, `MongoIndex`, `Version` | `oap-storage` |
| [oap-storage-cloud](oap-storage-cloud/README.md) | `FileSystem`, `CloudURI`, `FileSystemConfiguration`, `FileSystemCloudApi` | `oap-stdlib` |
| [oap-storage-cloud-aws-s3](oap-storage-cloud-aws-s3/) | AWS S3 backend (`s3://` scheme) | `oap-storage-cloud` |
| [oap-storage-mongo-test](oap-storage-mongo-test/README.md) | `MongoFixture` — in-memory MongoDB for tests | `oap-storage-mongo` |
| [oap-storage-cloud-test](oap-storage-cloud-test/README.md) | `S3MockFixture` — LocalStack S3 for tests | `oap-storage-cloud-aws-s3` |

## Quick start

```java
// Define an identifier — extracts/assigns the String key from your object
Identifier<String, MyData> id = Identifier.forId( d -> d.id, ( d, newId ) -> d.id = newId )
    .suggestion( d -> d.name )
    .build();

// In-memory store, concurrent reads and writes
MemoryStorage<String, MyData> storage = new MemoryStorage<>( id, Lock.CONCURRENT );

// Store
storage.store( new MyData( "item-1", "hello" ), "system" );

// Read
Optional<MyData> found = storage.getNullable( "item-1" );

// Update in place
storage.update( "item-1", d -> { d.name = "world"; return d; } );

// Listen to changes
storage.addDataListener( new Storage.DataListener<String, MyData>() {
    @Override
    public void updated( IdObject<String, MyData> previous, IdObject<String, MyData> updated ) {
        System.out.println( "changed: " + updated.id );
    }
} );
```
