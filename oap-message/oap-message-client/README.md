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
    // last delivery attempt failed or too many consecutive network errors
}
```

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `host` | — | Target server hostname |
| `port` | `8081` | Target server port |
| `httpPrefix` | `/messages` | URL path for the message endpoint |
| `directory` | — | Directory for persisting unsent messages to disk |
| `memorySyncPeriod` | `100` ms | How often to flush the in-memory queue (`-1` disables) |
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
