# oap-message-client

Durable HTTP message sender for the OAP platform. Messages are queued in memory and flushed to the server on a schedule. When the server is unreachable, unsent messages are spilled to disk and reloaded on the next `syncDisk` pass or on the next process start.

Depends on: `oap-http`

## `MessageSender`

### Sending messages

```java
// Send with default version 1
sender.send( MY_MESSAGE_TYPE, payload, ContentWriter.ofJson() );

// Send with explicit version
sender.send( MY_MESSAGE_TYPE, (short) 2, payload, ContentWriter.ofJson() );

// Raw bytes
sender.send( MY_MESSAGE_TYPE, bytes, 0, bytes.length );
```

`send` enqueues the message in the in-memory ready queue and returns immediately — it does not block on network I/O.

### Flushing

```java
// Flush in-memory queue to server (called automatically on schedule)
sender.syncMemory();

// Reload persisted messages from disk into the ready queue
sender.syncDisk();
```

Both methods are called automatically when `start()` is used with the default scheduler configuration.

`syncMemory()` drains the ready queue until it is empty. With `concurrency = 1` (default) messages are delivered one by one; with `concurrency = N` the run starts `N` workers on virtual threads that poll the shared ready queue in parallel. `syncMemory()` returns only after all workers finish, so scheduled runs never overlap. Delivery order is not guaranteed in either mode (retried messages are re-queued at the tail).

### Lifecycle

```java
sender.start();   // starts disk and memory sync schedulers
// … application runs …
sender.close();   // stops schedulers; spills any unsent in-memory messages to disk
```

### Availability

```java
MessageAvailabilityReport report = sender.availabilityReport( MY_MESSAGE_TYPE );
if( report.state == State.FAILED ) {
    // report.subsystemStates tells which subsystem failed
}
```

When everything is fine the report is `OPERATIONAL` with empty `subsystemStates`. Otherwise it is `FAILED` and `subsystemStates` contains all three subsystems:

| Subsystem | Constant | `FAILED` when |
|---|---|---|
| `IO` | `MessageSender.FAILURE_IO_STATE` | more than `networkAvailableMaxErrors` consecutive network errors, or the last delivery of this message type returned a retryable error |
| `MESSAGES` | `MessageSender.FAILURE_MESSAGES_STATE` | messages held in memory (ready + retry + inprogress) exceed `maxMessages` |
| `SHUTDOWN` | `MessageSender.FAILURE_SHUTDOWN_STATE` | the sender is closed |

`send` is never blocked or rejected by `maxMessages` — the limit is a signal for the caller (e.g. to stop producing or to fail a health check).

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `host` | — | Target server hostname |
| `port` | `8081` | Target server port |
| `httpPrefix` | `/messages` | URL path for the message endpoint |
| `directory` | — | Directory for persisting unsent messages to disk |
| `memorySyncPeriod` | `100` ms | How often to flush the in-memory queue (`-1` disables) |
| `concurrency` | `1` | Number of parallel delivery workers in one `syncMemory()` run |
| `maxMessages` | `5000` | Messages in memory (ready + retry + inprogress) above which `availabilityReport` returns `FAILED` / `MESSAGES` (`<= 0` disables) |
| `diskSyncPeriod` | `1m` | How often to scan `directory` for persisted messages (`-1` disables) |
| `retryTimeout` | `1s` | Delay before retrying a failed message |
| `globalIoRetryTimeout` | `1s` | Global backoff after a host-unreachable error |
| `timeout` | `5s` | HTTP request read/write timeout |
| `connectionTimeout` | `30s` | TCP connection timeout |
| `keepAliveDuration` | `30d` | Maximum age of on-disk messages before they are discarded |
| `storageLockExpiration` | `1h` | Expiry of per-file lock files created during disk sync |
| `networkAvailableMaxErrors` | `2` | Consecutive errors before `availabilityReport` returns `FAILED` |
| `messageNoRetryStrategy` | `DROP` | What to do when the server returns `STATUS_UNKNOWN_ERROR_NO_RETRY` |

## Disk persistence layout

```
<directory>/
  <clientId-hex>/
    <messageType>/
      <md5Hex>-<version>.bin        ← persisted message payload
      <md5Hex>-<version>.bin.lock   ← per-file lock created during reload
```

Messages are written atomically (`.bin.tmp` → `.bin`) and deleted once delivered or reloaded into the queue.

## Metrics

`MessageSender` publishes Micrometer gauges and counters automatically:

| Metric | Tags | Description |
|---|---|---|
| `message_count` | `host`, `port`, `type=ready\|retry\|inprogress` | Queue depth by state |
| `oap.messages` | `type`, `status` | Per-message-type delivery outcome counter |

## Message type registration

Declare human-readable names for message type bytes in `META-INF/oap-messages.properties` so they appear in logs and metrics:

```properties
type.my-data = 0x42
```

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-message-client]

services {
  oap-message-client.oap-http-message-sender.parameters {
    host      = stats-server.internal
    port      = 8081
    httpPrefix = /messages
    directory = /opt/oap/messages/sender
    memorySyncPeriod = 100
    concurrency = 4
    maxMessages = 5000
  }
}
```

Access the sender in your service:

```hocon
services {
  my-service {
    implementation = com.example.MyService
    parameters {
      sender = <modules.oap-message-client.oap-http-message-sender>
    }
  }
}
```
