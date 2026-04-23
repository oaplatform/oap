# oap-logstream

High-throughput transactional log streaming for the OAP platform. Writes typed data rows — rendered from Java objects via the template engine — into time-bucketed, gzip-compressed TSV files on disk. Supports both local disk writes and remote delivery over TCP via `oap-message`.

## Architecture

```
TemplateLogger ──────────────────────────────► DiskLoggerBackend ──► gzip TSV files
                                                        ▲
TemplateLogger ──► SocketLoggerBackend                  │
                       │ (oap-message TCP)               │
                       ▼                                 │
               SocketLoggerServer ──────────────────────┘

TemplateLogger ──► MemoryLoggerBackend   (tests)
```

`TemplateLogger` renders one row per `log()` call and hands the bytes to whichever `AbstractLoggerBackend` is wired in. `DiskLoggerBackend` fans writes across per-`LogId` writers; each writer tracks its own buffer and rotates output files on a time-bucket boundary.

## Sub-modules

| Module | Description |
|---|---|
| `oap-logstream` | Core: `TemplateLogger`, `DiskLoggerBackend`, `MemoryLoggerBackend`, `Timestamp`, `LogId` |
| `oap-logstream-net-client` | `SocketLoggerBackend` — buffers rows and flushes via `oap-message` TCP |
| `oap-logstream-net-server` | `SocketLoggerServer` — receives TCP messages and forwards to any backend |
| `oap-logstream-data` | `DataModel`, `LogRenderer` — schema-driven data model abstraction |
| `oap-logstream-data-object` | `ObjectLogRenderer` — renders arbitrary Java objects using the data model |
| `oap-logstream-test` | Test backends and helpers |

---

## `Timestamp`

Controls the time-bucketing cadence for output files. Each bucket maps to one output file.

| Constant | Buckets/hour | File rotates every |
|---|---|---|
| `Timestamp.BPH_1` | 1 | 60 min |
| `Timestamp.BPH_2` | 2 | 30 min |
| `Timestamp.BPH_3` | 3 | 20 min |
| `Timestamp.BPH_4` | 4 | 15 min |
| `Timestamp.BPH_6` | 6 | 10 min |
| `Timestamp.BPH_12` | 12 | 5 min |

```java
Timestamp ts = Timestamp.BPH_12;

// Format a DateTime to a file timestamp string: "2024-06-01-14-00"
String stamp = ts.format( DateTime.now() );

// Build the full file path under a base directory
String path = Timestamp.path( "/data/logs", stamp, "impressions", "tsv.gz" );
// → /data/logs/2024-06/01/impressions-2024-06-01-14-00.tsv.gz

// Iterate timestamps backward from now
ts.timestampsBeforeNow( 12 )   // last 12 buckets (1 hour at BPH_12)
  .forEach( System.out::println );
```

File names follow the pattern `<name>-yyyy-MM-dd-HH-mm.<ext>` where `mm` is `00` padded to the nearest bucket boundary.

---

## `DiskLoggerBackend`

Writes log rows to gzip-compressed TSV files on local disk. Each unique `LogId` (log type + file prefix + properties + headers) gets its own writer; writers are cached for the duration of a time bucket and evicted when the bucket changes.

```java
DiskLoggerBackend backend = new DiskLoggerBackend(
    templateEngine,
    Path.of( "/data/logs" ),
    Timestamp.BPH_12,
    100 * 1024,   // bufferSize per writer (bytes)
    Inet.hostName()
);
backend.start();
```

### Key parameters

| Parameter | Default | Description |
|---|---|---|
| `logDirectory` | (required) | Root directory; hostname is appended as a subdirectory |
| `timestamp` | (required) | Bucket cadence (`BPH_1` … `BPH_12`) |
| `bufferSize` | `102400` (100 KB) | Per-writer in-memory write buffer |
| `filePattern` | `/${YEAR}-${MONTH}/${DAY}/${LOG_TYPE}_v${LOG_VERSION}_${CLIENT_HOST}-${YEAR}-${MONTH}-${DAY}-${HOUR}-${INTERVAL}.tsv.gz` | Output path template |
| `requiredFreeSpace` | 2 GB | Minimum free space; backend reports FAILED below this threshold |
| `maxVersions` | 20 | Maximum concurrent file versions per log ID |
| `refreshInitDelay` | 10 s | Delay before first writer flush |
| `refreshPeriod` | 10 s | How often writers are flushed and evicted |

### File pattern tokens

| Token | Value |
|---|---|
| `${LOG_TYPE}` | Log type string from `log()` call |
| `${LOG_VERSION}` | Protocol version |
| `${CLIENT_HOST}` | Source hostname |
| `${YEAR}`, `${MONTH}`, `${DAY}`, `${HOUR}` | UTC date components |
| `${INTERVAL}` | Zero-padded bucket index within the hour (required — must be present to detect bucket rotation) |
| `${MINUTE}` | Alias for `${INTERVAL}` |

Per-type patterns override the default:

```java
backend.filePatternByType.put( "CLICK",
    new DiskLoggerBackend.FilePatternConfiguration(
        "/${YEAR}-${MONTH}/${DAY}/clicks-${HOUR}-${INTERVAL}.tsv.gz" ) );
```

---

## `SocketLoggerBackend` (net-client)

Buffers rows in memory and flushes them over TCP to a `SocketLoggerServer` using the `oap-message` protocol. Suitable when the writing process does not have direct filesystem access to the log directory.

```java
SocketLoggerBackend backend = new SocketLoggerBackend(
    messageSender,    // oap-message MessageSender
    100 * 1024,       // per-logId buffer size (bytes)
    30_000L           // flush interval (ms)
);
```

| Parameter | Description |
|---|---|
| `sender` | `MessageSender` from `oap-message-client` — handles TCP connection and retry |
| `bufferSize` | Per-log-ID in-memory buffer; rows are accumulated until the buffer is full or the flush interval fires |
| `flushInterval` | How often buffered rows are sent to the server (milliseconds) |
| `maxBuffers` | Maximum number of in-flight buffers before the backend reports FAILED (default 5000) |

---

## `SocketLoggerServer` (net-server)

A `MessageListener` that plugs into the `oap-message` server. It deserialises incoming logstream messages and forwards them to any `AbstractLoggerBackend` — typically a `DiskLoggerBackend`.

```java
SocketLoggerServer server = new SocketLoggerServer( diskBackend );
// Register with the oap-message MessageServer via oap-module.oap
```

---

## `TemplateLogger`

The primary logging entry point. Renders an object to a row using a `DictionaryTemplate`, then calls `backend.log()`.

```java
TemplateLogger<MyEvent, String, StringBuilder, TemplateAccumulatorString> logger =
    new TemplateLogger<>( backend, dictionaryTemplate );

logger.log( "events", Map.of( "region", "eu" ), "IMPRESSION", event );

if( logger.isLoggingAvailable() ) {
    // backend is OPERATIONAL
}
```
