# oap-statsdb-node

Client-side accumulator for `oap-statsdb`. Collects statistics locally and flushes them to a `StatsDBMaster` on a schedule, keeping the write path off the network on every counter increment.

Depends on: `oap-statsdb-common`

## `StatsDBNode`

Implements `IStatsDB`. Updates accumulate in a local `ConcurrentHashMap<NodeId, Node>`. On `sync()`, the current snapshot is cleared and sent to the master via `StatsDBTransport`.

```java
StatsDBNode node = new StatsDBNode( schema, transport );

// Accumulate locally — no network call
node.<Counters>update( "api", "2024-06-01", v -> v.requests++ );
node.<Counters>update( "api", "2024-06-01", v -> v.errors++ );

// Flush to master
node.sync();

// Check if last sync succeeded
if( !node.lastSyncSuccess ) {
    log.warn( "stats sync failed" );
}
```

After `sync()`, the node's local state is empty regardless of whether the sync succeeded. Failed syncs are logged; `lastSyncSuccess` is set to `false`.

### Key methods

| Method | Description |
|---|---|
| `sync()` | Snapshot local state, clear it, and send to master via transport |
| `run()` | Calls `sync()` — use as a scheduled `Runnable` |
| `close()` | Calls `sync()` on shutdown — ensures pending data is not lost |
| `removeAll()` | Discard all pending local updates without sending |
| `lastSyncSuccess` | `true` if the last `sync()` call completed without exception |

### OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-statsdb-node]

services {
  my-statsdb-node {
    implementation = oap.statsdb.node.StatsDBNode
    parameters {
      schema    = ???
      transport = <modules.this.my-statsdb-transport>
    }
    supervision {
      supervise = true
      schedule  = true
      delay     = 10s           # sync every 10 seconds
    }
  }

  my-statsdb-transport {
    implementation = oap.statsdb.node.StatsDBTransportMessage
    parameters {
      sender = <modules.oap-message.message-sender>
    }
  }
}
```

---

## `StatsDBTransport`

Functional interface for delivering a sync batch to the master.

```java
@FunctionalInterface
public interface StatsDBTransport {
    void sendAsync( RemoteStatsDB.Sync sync );
}
```

### `StatsDBTransportMessage`

Production transport. Serializes the `Sync` payload to JSON and sends it via `oap-message`'s `MessageSender`.

```java
StatsDBTransport transport = new StatsDBTransportMessage( messageSender );
```

`MessageSender` handles reliable delivery: messages are buffered on disk and retried until acknowledged by the master's `StatsDBMessageListener`.
