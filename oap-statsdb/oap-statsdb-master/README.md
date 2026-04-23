# oap-statsdb-master

Master-side components for `oap-statsdb`. Holds the authoritative in-memory statistics tree, merges updates from remote nodes, and persists state to storage.

Depends on: `oap-statsdb-common`

## `StatsDBMaster`

Extends `StatsDB`. Loads persisted state on startup and accepts sync batches from `StatsDBNode` instances.

```java
try( StatsDBMaster master = new StatsDBMaster( schema, StatsDBStorage.NULL ) ) {
    // direct local updates
    master.<Counters>update( "api", "2024-06-01", v -> v.requests += 10 );

    // query
    Counters c = master.get( "api", "2024-06-01" );

    // periodic persist (called by scheduler or manually)
    master.run();

    // permanently remove a key path from memory and storage
    master.permanentlyDelete( "api", "2024-06-01" );

    // reset all data
    master.reset();
} // close() persists state
```

### Key methods

| Method | Description |
|---|---|
| `update( Sync sync, String host )` | Merge a sync batch from a remote node |
| `run()` | Persist current in-memory state to `StatsDBStorage` (implements `Runnable`) |
| `close()` | Persist and release resources (implements `Closeable`) |
| `reset()` | Clear in-memory state and wipe storage |
| `permanentlyDelete( String... keys )` | Remove a key path from memory and storage, then recompute aggregates |

### OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-statsdb-master]

services {
  my-statsdb-master {
    implementation = oap.statsdb.StatsDBMaster
    parameters {
      schema  = ???          # inject NodeSchema bean
      storage = <modules.this.my-statsdb-storage>
    }
    supervision {
      supervise = true
      schedule  = true
      cron      = "0 * * * * ?"   # persist every minute
    }
  }

  my-statsdb-storage {
    implementation = oap.statsdb.StatsDBStorageMongo
    parameters {
      mongoClient = <modules.oap-storage-mongo.mongo-client>
      table       = "stats"
    }
  }
}
```

---

## `StatsDBStorage`

Interface for persisting and loading the tree. The master calls `store` on every scheduled `run()` and `close()`, and `load` once on startup.

```java
public interface StatsDBStorage {
    Map<String, Node> load( NodeSchema schema );
    void store( NodeSchema schema, Map<String, Node> db );
    void removeAll();
    void permanentlyDelete( NodeSchema schema, String... keys );
}
```

### Implementations

| Implementation | Description |
|---|---|
| `StatsDBStorage.NULL` | No-op — data is lost on restart; useful for tests and ephemeral deployments |
| `StatsDBStorageMongo` | Persists each leaf node as a MongoDB document; upserts in bulk |

#### `StatsDBStorageMongo`

```java
StatsDBStorageMongo storage = new StatsDBStorageMongo( mongoClient, "stats_collection" );
```

| Field | Default | Description |
|---|---|---|
| `bulkSize` | `1000` | Maximum documents per MongoDB bulk write |

---

## `StatsDBMessageListener`

Receives sync batches from `StatsDBNode` instances over the `oap-message` HTTP protocol and feeds them into a `StatsDBMaster`.

```java
// Wired automatically when configured in oap-module.oap
StatsDBMessageListener listener = new StatsDBMessageListener( master );
```

Register it with a `MessageHttpHandler` to enable the HTTP message endpoint:

```hocon
services {
  my-message-handler {
    implementation = oap.message.server.MessageHttpHandler
    parameters {
      server    = <modules.oap-http.oap-http-server>
      path      = /messages
      listeners = [<modules.this.my-statsdb-listener>]
    }
  }

  my-statsdb-listener {
    implementation = oap.statsdb.StatsDBMessageListener
    parameters {
      master = <modules.this.my-statsdb-master>
    }
  }
}
```

The listener handles message type `MessageType.MESSAGE_TYPE` and deserializes `RemoteStatsDB.Sync` payloads from JSON.
