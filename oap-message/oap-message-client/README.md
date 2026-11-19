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

### Priority

```java
// higher priority - sent earlier; default MessageSender.DEFAULT_PRIORITY = 0, negative values are allowed
sender.send( MY_MESSAGE_TYPE, (short) 1, 10, payload, ContentWriter.ofJson() );
sender.send( MY_MESSAGE_TYPE, (short) 1, 10, bytes, 0, bytes.length );
```

The ready queue is drained strictly by priority: a message is taken only when no message of a higher priority is ready.
Messages of the same priority are taken in the order of `send`. A retried message keeps its priority and goes to the tail of
its priority. The priority survives the spill to disk (`close()`) and the reload (`syncDisk()`).

Strict priority means a steady stream of high-priority messages delays the lower ones until it stops.

### Spool (store and forward)

With `spool = true` (runtime switch, `setSpool( true )`) a message that cannot be delivered now goes to a disk spool instead
of the memory queue, and the sender stays `OPERATIONAL` until a spool limit is reached. "Cannot be delivered now":

- the server is unavailable: more than `networkAvailableMaxErrors` consecutive network errors, or the last delivery of the
  message type returned a retryable error;
- the memory queue (ready + retry + inprogress) reached `maxMessages`.

`send()` writes such a message to the spool; `syncMemory()` moves the memory messages of an unavailable server to the spool.
When the server is available again, `syncMemory()` replays the spool: up to `spoolReplayMessages` spooled messages are kept in
memory at once, the highest priority first. While the server is unavailable exactly one spooled message is kept in memory and
retried (the probe), so the reconnection is detected without loading the spool into memory. A spooled segment is deleted when
all its messages are delivered (`OK`, `ALREADY_WRITTEN`) or dropped (no-retry statuses). The spool is replayed whether `spool`
is enabled or not, so switching it off at runtime still drains the backlog.

Optional limits, any combination (each `<= 0` - off):

| Parameter | Limit |
|---|---|
| `spoolMaxAge` | ms, age of the oldest not delivered spooled message |
| `spoolMaxSize` | bytes of the spool |
| `spoolMinFreeSpace` | bytes, usable space of the file store of the spool (checked at most once a second) |

When a limit is reached nothing is dropped: new messages go to the memory queue (the old behaviour) and `availabilityReport`
is `FAILED` with `SPOOL` (`IO` / `MESSAGES` by the old rules). The same happens when a spool write fails.

Delivery is at-least-once, as without the spool: a message replayed after a crash may be sent again, the server deduplicates
it by md5. Within a priority the order across an outage is not kept (new messages go to memory once the server is available,
the spooled ones are replayed in parallel).

Durability (`spoolSync`): `PERIODIC` (default) - the active segments are synced every `spoolSyncInterval`, when a segment is
closed and on close; `ALWAYS` - before `send()` returns; `ON_CLOSE` - when a segment is closed. A process crash loses nothing
that reached the spool in any mode; the modes differ only for an OS crash / power loss.

### Flushing

```java
// Flush in-memory queue to server (called automatically on schedule)
sender.syncMemory();

// Reload persisted messages from disk into the ready queue
sender.syncDisk();
```

Both methods are called automatically when `start()` is used with the default scheduler configuration.

`syncMemory()` drains the ready queue until it is empty. With `concurrency = 1` (default) messages are delivered one by one; with `concurrency = N` the run starts `N` workers on virtual threads that poll the shared ready queue in parallel. `syncMemory()` returns only after all workers finish, so scheduled runs never overlap. Messages are taken from the ready queue by [priority](#priority); with `concurrency = 1` they are delivered in that order, with `concurrency = N` the delivery of concurrently taken messages may interleave. Retried messages are re-queued at the tail of their priority.

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

When everything is fine the report is `OPERATIONAL` with empty `subsystemStates`. Otherwise it is `FAILED` and `subsystemStates` contains all the subsystems (`SPOOL` only when `spool` is enabled):

| Subsystem | Constant | `FAILED` when |
|---|---|---|
| `IO` | `MessageSender.FAILURE_IO_STATE` | more than `networkAvailableMaxErrors` consecutive network errors, or the last delivery of this message type returned a retryable error; not checked while the spool takes the messages |
| `MESSAGES` | `MessageSender.FAILURE_MESSAGES_STATE` | messages held in memory (ready + retry + inprogress) exceed `maxMessages`; not checked while the spool takes the messages |
| `SHUTDOWN` | `MessageSender.FAILURE_SHUTDOWN_STATE` | the sender is closed |
| `SPOOL` | `MessageSender.FAILURE_SPOOL_STATE` | `spool` is enabled and a spool limit is reached (`getSpoolLimit()`) or the last spool write failed |

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
| `spool` | `false` | [Spool](#spool-store-and-forward) the messages that cannot be delivered now (runtime, `setSpool`) |
| `spoolMaxAge` | `0` | ms, spool limit, `<= 0` - off (runtime) |
| `spoolMaxSize` | `0` | bytes, spool limit, `<= 0` - off (runtime) |
| `spoolMinFreeSpace` | `0` | bytes, spool limit, `<= 0` - off (runtime) |
| `spoolSegmentSize` | `64 MB` | the active segment is closed at this size (runtime) |
| `spoolSync` | `PERIODIC` | `ALWAYS` / `PERIODIC` / `ON_CLOSE` (runtime) |
| `spoolSyncInterval` | `1s` | period of `PERIODIC` (runtime, rescheduled immediately) |
| `spoolReplayMessages` | `64` | max spooled messages in memory at once while the server is available (runtime) |

## Disk persistence layout

```
<directory>/
  <clientId-hex>/
    <messageType>/
      <md5Hex>-<version>.bin              ← persisted message payload, default priority
      <md5Hex>-<version>-<priority>.bin   ← persisted message payload, priority != 0 (e.g. `…-1-10.bin`, `…-1--5.bin`)
      <md5Hex>-<version>[-<priority>].lock ← per-file lock created during reload
```

A default-priority message keeps the old file name, so the files are readable by the older versions.

Messages are written atomically (`.bin.tmp` → `.bin`) and deleted once delivered or reloaded into the queue.
With `spool` enabled `close()` writes the unsent messages to the spool instead.

### Spool layout

```
<directory>/spool/
  <clientId-hex>/
    .lock                         ← FileChannel lock, held while the sender lives
    p<priority>/                  ← one append-only stream per priority (p0, p10, p-3 …)
      <seq>.seg                   ← segment, closed at spoolSegmentSize
```

Segment: `int magic "OMSP" | byte format version`, then records
`int length | byte messageType | short version | long enqueued time | byte[16] md5 | data | int crc32c`.
A record is appended with one gathering write (the payload is not copied); a record with an invalid length or crc
(a torn tail of a crash) ends the segment on replay and is counted as `corrupted`.

The directory of a dead sender (its `.lock` can be taken, a restarted process has a new client id) is adopted on `start()`
and on every `syncDisk()`: its segments are moved into the own directory and replayed.

## Metrics

`MessageSender` publishes Micrometer gauges and counters automatically:

| Metric | Tags | Description |
|---|---|---|
| `message_count` | `host`, `port`, `type=ready\|retry\|inprogress` | Queue depth by state |
| `oap.messages` | `type`, `status` | Per-message-type delivery outcome counter |
| `message_spool_bytes`, `message_spool_messages`, `message_spool_segments` | `host`, `port` | Spool size: bytes, not delivered messages, segments |
| `message_spool_oldest_age_ms` | `host`, `port` | Age of the oldest spool segment |
| `message_spool_free_space_bytes` | `host`, `port` | Usable space of the file store of the spool |
| `message_spool_offline` | `host`, `port` | `1` - the server is unavailable (network errors) |
| `message_spool_limit` | `host`, `port`, `limit=size\|age\|free_space` | `1` - the limit is reached |
| `oap.messages.spool` | `type`, `event=written\|replayed\|delivered\|dropped\|write_error\|corrupted\|adopted` | Spool events (messages; `adopted` - segments) |
| `oap.messages.spool.bytes` | `type`, `event=written\|replayed` | Spool bytes |
| `message_spool_sync` | `host`, `port` | Timer of the `PERIODIC` sync |

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
    spool = true
    spoolMaxSize = 10737418240   // 10 GB
    spoolMinFreeSpace = 1073741824
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
