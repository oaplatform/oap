# oap-storage

Core storage abstractions for the OAP platform. Provides the `Storage<Id,Data>` interface, the `MemoryStorage` in-memory implementation, and supporting types for change tracking and schema migration.

## `Storage<Id, Data>`

The central interface. All operations take an `Id` key and operate on `Data` objects wrapped in `Metadata<Data>`.

### Reading

| Method | Returns | Description |
|---|---|---|
| `select()` | `Stream<Data>` | All objects as a lazy stream |
| `selectMetadata()` | `Stream<Metadata<Data>>` | All objects with metadata |
| `list()` | `List<Data>` | All objects materialised |
| `listMetadata()` | `List<Metadata<Data>>` | All objects with metadata, materialised |
| `getNullable(id)` | `Data` or `null` | Fetch by id; null if absent |
| `get(id)` | `Optional<Data>` | Fetch by id |
| `get(id, init, modifiedBy)` | `Data` | Fetch or create with supplier if absent |
| `size()` | `long` | Number of stored objects |

### Writing

| Method | Description |
|---|---|
| `store(object, modifiedBy)` | Insert or replace; returns stored object |
| `store(collection, modifiedBy)` | Bulk insert or replace |
| `update(id, fn, modifiedBy)` | Apply function to existing object; returns `Optional` (empty if not found) |
| `update(id, fn, init, modifiedBy)` | Apply function or create with supplier if absent |
| `tryUpdate(id, fn, modifiedBy)` | Apply function; returns `false` if not found |

### Deleting

| Method | Description |
|---|---|
| `delete(id)` | Remove by id; returns `Optional<Data>` of removed object |
| `deleteMetadata(id)` | Remove; returns `Optional<Metadata<Data>>` |
| `deleteAll()` | Remove all objects |

### Constant `MODIFIED_BY_SYSTEM`

Pass `Storage.MODIFIED_BY_SYSTEM` as `modifiedBy` for system-initiated writes.

---

## `MemoryStorage<Id, Data>`

In-memory `ConcurrentHashMap`-backed implementation. Also implements `ReplicationMaster` and `RemoteStorage` for diff-based replication between nodes.

### Construction

```java
// Define an identifier — extracts/assigns the String key from your object
Identifier<String, MyData> id = Identifier
    .forId( d -> d.id, ( d, newId ) -> d.id = newId )
    .suggestion( d -> d.name )   // optional: derive id from name if field is blank
    .build();

// Concurrent — no per-id locking
MemoryStorage<String, MyData> storage = new MemoryStorage<>( id, Lock.CONCURRENT );

// Serialized — lock per id string (safe for compare-and-update)
MemoryStorage<String, MyData> storage = new MemoryStorage<>( id, Lock.SERIALIZED );

// With transaction log (required by MongoPersistence for incremental sync)
MemoryStorage<String, MyData> storage = new MemoryStorage<>( id, Lock.SERIALIZED, 10_000 );
```

### `Lock`

| Constant | Behaviour |
|---|---|
| `Lock.CONCURRENT` | No locking — highest throughput; `update` is not atomic |
| `Lock.SERIALIZED` | Per-id `synchronized` block — safe for concurrent updates to the same object |

### `transactionLogSize`

When `> 0`, the storage maintains a ring-buffer change log of that capacity. `MongoPersistence` uses it to produce incremental bulk writes instead of full scans. Set it to at least a few times the expected write rate per sync interval.

---

## `Metadata<T>`

Wraps every stored object.

| Field | Type | Description |
|---|---|---|
| `object` | `T` | The stored data object |
| `created` | `long` | Creation timestamp (epoch ms) |
| `modified` | `long` | Last modification timestamp (epoch ms) |
| `createdBy` | `String` | Who created the object |
| `modifiedBy` | `String` | Who last modified the object |

`looksUnmodified(other)` returns `true` if `modified` timestamps are equal — used for optimistic sync.

---

## `DataListener<I, D>`

Register with `storage.addDataListener(listener)` to receive change notifications. All callbacks are default (no-op) so implement only what you need.

| Method | When called |
|---|---|
| `added(List<IdObject>)` | Objects newly inserted |
| `updated(List<IdObject>)` | Objects replaced or mutated |
| `deleted(List<IdObject>)` | Objects soft-deleted |
| `permanentlyDeleted(IdObject)` | Object hard-deleted (e.g. after MongoDB `deleteOne`) |
| `changed(added, updated, deleted)` | All three lists at once — default dispatches to the three above |

`IdObject<DI,D>` has two fields: `id` and `metadata`.

```java
storage.addDataListener( new Storage.DataListener<String, MyData>() {
    @Override
    public void added( List<IdObject<String, MyData>> objects ) {
        objects.forEach( o -> index.put( o.id, o.metadata.object ) );
    }
} );
```

---

## `Migration`

Implement this interface to upgrade stored JSON payloads when the schema changes.

```java
public class AddDefaultStatus implements Migration {
    @Override
    public long fromVersion() { return 2; }   // run when stored version == 2

    @Override
    public JsonMetadata run( JsonMetadata old ) {
        old.object().put( "status", "ACTIVE" );
        return old;
    }
}
```

`JsonMetadata` wraps `JsonObject` (mutable BSON-like document). The migration chain is applied in version order on load. Used by `MongoPersistence` / `MigrationUtils`.

---

## Replication (`ReplicationMaster` / `RemoteStorage`)

`MemoryStorage` implements both sides of a diff-based replication protocol:

- **`ReplicationMaster`** — exposes `updatedSince(timestamp, hash)` which returns objects changed after the given watermark.
- **`RemoteStorage`** — applies a received `Sync` batch of added/updated/deleted objects.

Replication is used by `StatsDB` and other distributed components; for typical application use you only need `Storage`.
