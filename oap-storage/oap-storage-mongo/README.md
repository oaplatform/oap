# oap-storage-mongo

MongoDB persistence for `MemoryStorage`. Changes are flushed on a schedule via bulk write, and optionally streamed back in real time via MongoDB change streams.

Depends on: `oap-storage`

## `MongoClient`

OAP's thin wrapper around the MongoDB Java driver. Manages the connection and runs schema migrations via [Flamingock](https://docs.flamingock.io) on startup.

```java
// Basic — no migrations
MongoClient client = new MongoClient( "mongodb://localhost:27017/mydb" );

// With Flamingock migrations
MongoClient client = new MongoClient( "mongodb://localhost:27017/mydb", true );
client.preStart();  // runs migrations
```

| Parameter | Default | Description |
|---|---|---|
| `connectionString` | — | Standard MongoDB URI including database name |
| `migrations` | `false` | Run all Flamingock changes found on the classpath on `preStart()` |
| `throwIfMigrationFailed` | `true` | Rethrow migration exceptions; set `false` to log-and-continue |

`preStart()` is called automatically by the OAP Kernel if registered as a supervised service.

### Migrations with Flamingock

Flamingock discovers changes **at compile time**. Every module that contains changes needs:

1. the annotation processor (keep the parent's processors with `combine.children="append"`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths combine.children="append">
            <path>
                <groupId>io.flamingock</groupId>
                <artifactId>flamingock-processor</artifactId>
                <version>${oap.deps.flamingock.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

2. one class declaring the stages (stage names must be unique across modules):

```java
@EnableFlamingock( stages = { @Stage( name = "my-app", location = "com.example.migrations" ) } )
public final class MyAppMigrations {}
```

3. change classes bound to the `MongoClient` target system. The order is taken from the class name (`_<order>__<Name>`, order of at least 4 characters):

```java
@SuppressWarnings( "checkstyle:TypeName" )
@TargetSystem( id = MongoClient.TARGET_SYSTEM )
@Change( id = "add-status-field", author = "team", transactional = false )
public class _0001__AddStatusField {
    @Apply
    public void apply( MongoDatabase db ) {
        db.getCollection( "users" )
          .updateMany( new Document(), Updates.set( "status", "ACTIVE" ) );
    }

    @Rollback
    public void rollback( MongoDatabase db ) {
        db.getCollection( "users" )
          .updateMany( new Document(), Updates.unset( "status" ) );
    }
}
```

Notes:

- `transactional` defaults to `true`; use `transactional = false` on a standalone MongoDB (no replica set).
- Every `MongoClient` with `migrations = true` runs **all** changes on the classpath — there is no per-client package filter.
- Audit and lock are stored in `flamingockAuditLog` / `flamingockLock`.
- Run one clean build after adding the processor, so `META-INF/flamingock/metadata_*` is generated.

### Migrating from Mongock

Mongock has been replaced by Flamingock; the `migrationPackage` parameter is gone.

- `migrationPackage = [...]` → `migrations = true`; `new MongoClient( uri, List.of( ... ) )` → `new MongoClient( uri, true )`.
- `@ChangeUnit`/`@Execution`/`@RollbackExecution` → `@Change`/`@Apply`/`@Rollback` + `@TargetSystem( id = MongoClient.TARGET_SYSTEM )`; `order` moves into the class name.
- Mongock history (`mongockChangeLog`) is **not** imported: on an existing database, rewritten changes run again, so make them idempotent (or drop changes that are already applied everywhere).

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
    migrations = true
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
