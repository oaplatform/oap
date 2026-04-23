# oap-storage-mongo

MongoDB persistence for `MemoryStorage`. Changes are flushed on a schedule via bulk write, and optionally streamed back in real time via MongoDB change streams.

Depends on: `oap-storage`

## `MongoClient`

OAP's thin wrapper around the MongoDB Java driver. Manages the connection and runs schema migrations via Mongock on startup.

```java
// Basic — no migrations
MongoClient client = new MongoClient( "mongodb://localhost:27017/mydb" );

// With Mongock migration package(s)
MongoClient client = new MongoClient(
    "mongodb://localhost:27017/mydb",
    List.of( "com.example.migrations" )
);
client.preStart();  // runs migrations
```

| Parameter | Default | Description |
|---|---|---|
| `connectionString` | — | Standard MongoDB URI including database name |
| `migrationPackage` | `null` | Package(s) scanned by Mongock for `@ChangeUnit` classes |
| `throwIfMigrationFailed` | `true` | Rethrow migration exceptions; set `false` to log-and-continue |

`preStart()` is called automatically by the OAP Kernel if registered as a supervised service.

### Migrations with Mongock

Annotate your migration classes with `@ChangeUnit` and place them in the package configured in `migrationPackage`:

```java
@ChangeUnit( id = "add-status-field", order = "001", author = "team" )
public class AddStatusField {
    @Execution
    public void execute( MongoDatabase db ) {
        db.getCollection( "users" )
          .updateMany( new Document(), Updates.set( "status", "ACTIVE" ) );
    }

    @RollbackExecution
    public void rollback( MongoDatabase db ) {
        db.getCollection( "users" )
          .updateMany( new Document(), Updates.unset( "status" ) );
    }
}
```

### `Version`

Lightweight version record for application-level schema tracking:

```java
Version current = new Version( 2, 1 );   // main=2, ext=1
boolean needsMigration = current.before( new Version( 3 ) );

// Persisted to the `version` collection
client.updateVersion( current );
```

---

## `MongoPersistence<I, T>`

Connects a `MemoryStorage` to a MongoDB collection. Loads all documents on `preStart()`, then periodically flushes changes back.

```java
MemoryStorage<String, Order> storage = new MemoryStorage<>( orderId, Lock.SERIALIZED, 10_000 );

MongoPersistence<String, Order> persistence = new MongoPersistence<>(
    mongoClient,
    "orders",       // collection name
    5_000L,         // flush interval in ms
    storage
);
persistence.preStart();
// storage is now populated and syncing
```

### Parameters

| Parameter | Default | Description |
|---|---|---|
| `mongoClient` | — | Connected `MongoClient` |
| `collectionName` | — | MongoDB collection (= "table") name |
| `delay` | — | Milliseconds between `fsync()` calls |
| `storage` | — | The `MemoryStorage` to back |
| `crashDumpPath` | `/tmp/mongo-persistance-crash-dump` | Directory for GZIP crash dumps on write failure |
| `watch` | `false` | Enable MongoDB change stream to apply remote changes in real time |
| `batchSize` | `100` | Max documents per bulk write call |

### Crash dumps

If a bulk write fails (network error, document too large, etc.), the pending batch is serialized to a GZIP JSON file under `crashDumpPath/<collection>/<timestamp>.json.gz` so no data is silently lost.

### Change stream (`watch = true`)

When enabled, `MongoPersistence` opens a `watch()` change stream and applies `INSERT`/`UPDATE`/`REPLACE`/`DELETE` operations from other writers (other pods, direct DB edits) into the local `MemoryStorage` in real time. Requires MongoDB 4.0+ replica set or sharded cluster.

### OAP module integration

```hocon
name = my-app
dependsOn = [oap-storage-mongo]

services {
  oap-storage-mongo.mongo-client.parameters {
    connectionString = "mongodb://mongo:27017/mydb"
    migrationPackage = ["com.example.migrations"]
  }

  order-storage {
    implementation = oap.storage.MemoryStorage
    parameters {
      identifier = <modules.this.order-identifier>
      lock       = SERIALIZED
      transactionLogSize = 10000
    }
  }

  order-persistence {
    implementation = oap.storage.MongoPersistence
    parameters {
      mongoClient    = <modules.oap-storage-mongo.mongo-client>
      collectionName = orders
      delay          = 5000
      storage        = <modules.this.order-storage>
      watch          = true
    }
    supervision.supervise = true
  }
}
```

---

## `MongoIndex`

Manages collection indexes declaratively: creates missing ones, drops obsolete ones.

```java
MongoIndex index = new MongoIndex( collection );

SequencedMap<String, MongoIndex.IndexConfiguration> desired = new LinkedHashMap<>();
desired.put( "status_created", new MongoIndex.IndexConfiguration(
    new LinkedHashMap<>( Map.of( "status", 1, "created", -1 ) ),
    false       // not unique
) );
// With TTL (value must be whole seconds expressed in ms):
desired.put( "expiry_idx", new MongoIndex.IndexConfiguration(
    new LinkedHashMap<>( Map.of( "expiresAt", 1 ) ),
    false,
    3_600_000L  // 1 hour in ms
) );

index.update( desired );   // idempotent — skips existing identical indexes
```

`IndexConfiguration` fields:

| Field | Type | Description |
|---|---|---|
| `keys` | `LinkedHashMap<String, Direction>` | Ordered field → `ASC`/`DESC` |
| `unique` | `boolean` | Enforce uniqueness |
| `expireAfter` | `Long` (ms) | TTL in milliseconds (whole seconds only); `null` = no expiry |
