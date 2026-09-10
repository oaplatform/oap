# oap-logstream

High-throughput transactional log streaming for the OAP platform. Writes typed data rows — rendered from Java objects via the template engine — into time-bucketed, gzip-compressed, RowBinary-framed files on disk. Supports both local disk writes and remote delivery over TCP via `oap-message`.

## Contents

- [Architecture](#architecture)
- [Sub-modules](#sub-modules)
- [`Timestamp`](#timestamp)
- [`DiskLoggerBackend`](#diskloggerbackend)
  - [On-disk write model](#on-disk-write-model)
  - [File pattern tokens](#file-pattern-tokens)
- [`SocketLoggerBackend` (net-client)](#socketloggerbackend-net-client)
- [`SocketLoggerServer` (net-server)](#socketloggerserver-net-server)
- [`TemplateLogger`](#templatelogger)
- [`RowBinaryObjectLogger`](#rowbinaryobjectlogger)
- [See also](#see-also)

## Architecture

```
TemplateLogger ──────────────────────────────► DiskLoggerBackend ──► gzip .rb.gz files
                                                        ▲
TemplateLogger ──► SocketLoggerBackend                  │
                       │ (oap-message TCP)              │
                       ▼                                │
               SocketLoggerServer ──────────────────────┘

TemplateLogger ──► MemoryLoggerBackend   (tests)
```

`TemplateLogger` renders one row per `log()` call and hands the bytes to whichever `AbstractLoggerBackend` is wired in. `DiskLoggerBackend` fans writes across per-`LogId` writers (currently always `RowBinaryWriter`); each writer tracks its own buffer and rotates output files on a time-bucket boundary. Every writer opens a file with a RowBinary column header, then appends whatever bytes the upstream renderer produced for each row — TSV-text bytes from `TemplateLogger`/`TemplateAccumulatorTsv`, or true RowBinary bytes from `RowBinaryObjectLogger`/`TemplateAccumulatorRowBinary` — and the writer always appends `.rb.gz` to the configured file pattern, regardless of what extension the pattern itself ends with.

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

Writes log rows to gzip-compressed, RowBinary-framed files on local disk. Each unique `LogId` (log type + file prefix + properties + headers) gets its own writer; writers are cached for the duration of a time bucket and evicted when the bucket changes.

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

An overload also takes a `WriterConfiguration`, whose only current knob is the date/time format used when rendering `DATETIME32`-typed TSV columns:

```java
WriterConfiguration writerConfiguration = new WriterConfiguration();
// writerConfiguration.tsv.dateTime32Format defaults to Dates.PATTERN_FORMAT_SIMPLE_CLEAN

DiskLoggerBackend backend = new DiskLoggerBackend(
    templateEngine, Path.of( "/data/logs" ), writerConfiguration, Timestamp.BPH_12, 100 * 1024, Inet.hostName() );
```

### On-disk write model

Each `buffer` passed to `AbstractLoggerBackend.log(...)` (and therefore to `backend.log()` / `Logger.log()`) must already be a **complete, standalone gzip member** — e.g. built with `oap.compression.Compression.gzip(...)`, as every test that exercises `DiskLoggerBackend`/`RowBinaryWriter` end-to-end does (`RowBinaryWriterTest.java`, `DiskLoggerBackendTest.java`). The writer never compresses `buffer` itself.

- **Gzip concatenation.** On first write for a file, `RowBinaryWriter` writes one gzip member containing just the RowBinary column header (`RowBinaryOutputStream` wrapped in a `GZIPOutputStream`, closed immediately with zero rows — `RowBinaryWriter.java:43-48`). Every subsequent `write()` call then appends the caller-supplied `buffer` — itself a complete gzip member — directly onto the file as raw bytes (`RowBinaryWriter.java:59` → `LogFile.beginTransactionWriteAndCommitTransaction`). The result is a sequence of independently-compressed gzip members concatenated back-to-back in one file. This is valid per [RFC 1952](https://www.rfc-editor.org/rfc/rfc1952): any standard gzip decoder (Java's `GZIPInputStream`, the `gzip`/`zcat` CLI, ClickHouse, `oap.compression.Compression.ungzip(...)`) reads a multi-member file transparently as one continuous decompressed stream — confirmed by `RowBinaryWriterTest.testWrite`, which writes two separately-gzipped row batches and reads the whole file back with a single `Compression.ungzip(...)` call.
- **Transactional write.** `LogFile` tracks the byte offset already durably committed in a sidecar `<file>.metadata.transaction` file. Each write: reads the last committed offset (`LogFile.beginTransaction()`), seeks the `FileChannel` to that exact offset, writes `buffer`, `force(true)`-fsyncs it, then atomically rewrites the transaction file with the new offset (`commitTransaction()`, via `Files.move(..., ATOMIC_MOVE)`) — `LogFile.java:77-118`, `:148-177`. Because every write re-seeks to the last *committed* offset rather than blindly appending, a crash between the data write and the transaction-offset update is safely retried/overwritten at the same position on the next write — combined with each write being a self-contained gzip member, every prefix of the file up to the last committed offset stays valid, decodable gzip at all times. A `.metadata.yaml` sidecar carries the `LogId`/schema (`LogFile.syncLogMetadata`), and a `.metadata.completed` marker (written by `AbstractWriter.closeOutput()` → `LogFile.readyForUpload()`) signals the file is done rotating and ready for pickup.

### Key parameters

| Parameter | Default                                                                                                                                                  | Description |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| `logDirectory` | (required)                                                                                                                                                | Root directory; hostname is appended as a subdirectory |
| `timestamp` | (required)                                                                                                                                                | Bucket cadence (`BPH_1` … `BPH_12`) |
| `bufferSize` | `102400` (100 KB)                                                                                                                                         | Per-writer in-memory write buffer |
| `writerConfiguration` | `new WriterConfiguration()`                                                                                                                              | Currently just `tsv.dateTime32Format` — date format used for TSV `DATETIME32` columns |
| `filePattern` | `{{ YEAR }}-{{ MONTH }}/{{ DAY }}/{{ LOG_TYPE }}_v{{ LOG_VERSION }}_{{ CLIENT_HOST }}-{{ YEAR }}-{{ MONTH }}-{{ DAY }}-{{ HOUR }}-{{ INTERVAL }}.tsv.gz` | Output path template — `.rb.gz` is always appended on top of this, so the actual default output extension is `.tsv.gz.rb.gz` |
| `requiredFreeSpace` | 2 GB                                                                                                                                                      | Minimum free space; backend reports FAILED below this threshold |
| `maxVersions` | 20                                                                                                                                                        | Maximum concurrent file versions per log ID |
| `refreshInitDelay` | 10 s                                                                                                                                                      | Delay before first writer flush |
| `refreshPeriod` | 10 s                                                                                                                                                      | How often writers are flushed and evicted |

### File pattern tokens

`filePattern` is not simple string substitution — it's rendered by the [`oap-template`](../oap-template/README.md) engine (`LogIdTemplate.render()`, via the same `TemplateEngine` passed into `DiskLoggerBackend`), so the full template syntax is available: `{{ VAR }}` expressions, `{{% if COND %}} ... {{% else %}} ... {{% end %}}` conditional blocks, `and`/`or`, etc. — see `oap-template`'s README for the complete syntax. The render context is a flat `Map<String, String>` of predefined variables (below) merged with every entry of the `properties` map passed to `TemplateLogger.log()` / `backend.log()`, so custom properties can be referenced (and branched on) directly in the pattern, e.g. `{{% if ORGANIZATION and ACCOUNT }}{{ ORGANIZATION }}/{{ ACCOUNT }}/{{% end }}...`.

`filePattern` must contain a `{{ LOG_VERSION }}` token and an `{{ INTERVAL }}` (or `{{ MINUTE }}`) token — the backend refuses to start otherwise (the former is required unconditionally; the latter is required so bucket rotation can be detected).

Predefined variables:

| Token                                                  | Value |
|--------------------------------------------------------|---|
| `{{ LOG_TYPE }}`                                       | Log type string from `log()` call |
| `{{ LOG_VERSION }}`                                    | Content hash + replica id + file version (required) |
| `{{ CLIENT_HOST }}`                                    | Source hostname |
| `{{ SERVER_HOST }}`                                    | Hostname of the process running the backend |
| `{{ YEAR }}`, `{{ MONTH }}`, `{{ DAY }}`, `{{ HOUR }}` | UTC date components |
| `{{ MINUTE }}`, `{{ SECOND }}`                         | Current UTC minute-of-hour / second-of-minute (zero-padded) — **not** related to `{{ INTERVAL }}` |
| `{{ INTERVAL }}`                                       | Zero-padded bucket index within the hour (required — must be present to detect bucket rotation) |
| `{{ LOG_TIME_INTERVAL }}`                              | Bucket length in minutes (`60 / bucketsPerHour`) |
| `{{ REGION }}`                                         | `REGION` environment variable |
| `{{ LOG_FORMAT }}`, `{{ LOG_FORMAT_<NAME> }}`          | Extension text for the writer's `LogFormat` (`TSV_GZ`, `TSV_ZSTD`, `ROW_BINARY_GZ`, `PARQUET`) — informational only; it does not change which writer implementation is used |
| any key from the `properties` map                     | Custom metadata passed to `TemplateLogger.log()` / `backend.log()` is also available as a token |

Per-type patterns override the default:

```java
backend.filePatternByType.put( "CLICK",
    new DiskLoggerBackend.FilePatternConfiguration(
        "{{ YEAR }}-{{ MONTH }}/{{ DAY }}/clicks-{{ HOUR }}-{{ INTERVAL }}-{{ LOG_VERSION }}.tsv.gz" ) );
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

---

## `RowBinaryObjectLogger`

Schema-driven logger that serializes Java objects to ClickHouse RowBinary format. The column schema is declared in an OAP dictionary model; a typed renderer is compiled once and reused per log call.

```java
RowBinaryObjectLogger logger = new RowBinaryObjectLogger(
    model,                           // DictionaryRoot — column schema
    backend,                         // any AbstractLoggerBackend
    Path.of( "/tmp/template-cache" ),
    Dates.d( 10 )                    // template cache TTL
);

TypedRowBinaryLogger<MyEvent> typed = logger.typed( new TypeRef<>() {}, "MODEL_ID" );
typed.log( event, "file-prefix", Map.of( "region", "eu" ), "EVENT_TYPE" );
```

See [docs/RowBinaryObjectLogger.md](docs/RowBinaryObjectLogger.md) for the full API reference, supported types, schema format, and a complete example.

---

## See also

- [`oap-template`](../oap-template/README.md) — the template engine that renders `filePattern` (see [File pattern tokens](#file-pattern-tokens)) and every `DictionaryTemplate`/row renderer (`TemplateLogger`, `RowBinaryObjectLogger`) built on top of it; full syntax reference (`{{ }}` expressions, `{{% if %}}` blocks, functions, accumulators).
- [docs/RowBinaryObjectLogger.md](docs/RowBinaryObjectLogger.md) — full `RowBinaryObjectLogger` API reference.
