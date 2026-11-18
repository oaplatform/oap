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

Writes log rows to gzip-compressed, RowBinary-framed files on local disk. Each unique `LogId` (log type + properties + headers) gets its own writer; writers are cached for the duration of a time bucket and evicted when the bucket changes.

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

Each `buffer` passed to `AbstractLoggerBackend.log(...)` (and therefore to `backend.log()` / `Logger.log()`) must already be a **complete, standalone compressed frame** of the writer's codec — a gzip member, an LZ4 frame or a zstd frame (`LogStreamCompression`; `log(...)` without a codec means gzip). E.g. built with `oap.compression.Compression.gzip(...)` / `lz4(...)` / `zstd(...)`, or by the client's `BufferCompressor`. The writer never compresses `buffer` itself. The codec defines the file: `.rb.gz`, `.rb.lz4`, `.rb.zst`, one writer per `(LogId, codec)`.

- **Frame concatenation.** A new file starts with one frame holding just the RowBinary column header (compressed once per writer); it is written together with the first `buffer` in a single gathering write. Every later `write()` appends the caller-supplied frame as raw bytes. The file is a sequence of independent frames, which every standard decoder reads as one stream ([RFC 1952](https://www.rfc-editor.org/rfc/rfc1952) multi-member gzip, concatenated LZ4 / zstd frames): `GZIPInputStream`, `zcat`, `lz4 -d`, `zstd -d`, ClickHouse, `oap.compression.Compression.ungzip/unlz4/unzstd(...)`.
- **Commit = sync of the data, grouped.** `LogFile.write(...)` appends at the end of the last complete frame under the writer lock (page cache only). With `SyncMode.ALWAYS` (default) the buffer is then committed **outside** the lock (`LogFile.commit(end)`): the first committer `fdatasync`s everything written so far, the buffers written meanwhile wait for that sync and are covered by it or share the next one. So N concurrent senders of one `(LogId, codec)` cost about one sync per sync latency, not N. `write()` returns — and the client is acknowledged — only when its buffer is synced. There is no separate commit record.
  - A failed **write** truncates the file back to the previous end, closes it (`.metadata.completed`) and the next buffer goes to the next file version; the buffers written before it are still committed and acknowledged.
  - A failed **sync** fails the file: every buffer not synced yet gets an error (the client resends it), the file is truncated to the synced end (nothing acknowledged is dropped), closed (`.metadata.completed`) and the next buffer goes to the next file version. If even the truncation fails, the file is closed without `.metadata.completed` and recovery repairs it on the next start.
- **Shards** (`maxShards`, default 1 — off). A `(LogId, codec)` key can write up to `maxShards` files at the same time, each with its own lock. Shard `k > 0` adds `-s<k>` before the codec extension (`log_v…-1-2015-10-10-01-00.tsv.gz-s2.rb.gz`; shard 0 keeps the plain name) and the metadata property `SHARD=k` (`VERSION` is the same in all shards). A write takes a free shard of the active ones (round-robin) and waits only when all of them are busy. Once per `refreshPeriod` window the active shard count **doubles** (up to `maxShards`) if the mean wait for a busy shard exceeded `shardGrowWait`, and **drops by one** after `shardShrinkWindows` windows in a row in which fewer shards than active were ever held at once. A shard taken out of use gets no new writes; its file is closed on the next rotation. Versions are chosen per shard (the names differ). Metrics: `logstream_logging_disk_shards` (active shards, all keys), `logstream_logging_disk_shard_changes{direction=grow|shrink}`, `logstream_logging_disk_lock_wait`.
- **Sidecars.** `<file>.metadata.yaml` carries the `LogId`/schema and properties (`VERSION`), written once when the file is created. `<file>.metadata.completed` (written by `AbstractWriter.closeOutput()` → `LogFile.readyForUpload()`) means the file is closed and the **whole** file is valid — ready for pickup.
- **Crash recovery.** `DiskLoggerBackend.start()` (`recoverOnStart`, default `true`; also `recover()`) walks the log directory and repairs every file left without `.metadata.completed`: `FrameScanner` walks the frames from the start, verifies them by decompression and by the content checksum (gzip CRC32; LZ4 and zstd frames are written with the content checksum), the file is truncated to the end of the last valid frame, synced and marked completed (the `FileWriterNotification` listeners registered at that moment are notified). A file with no data frames (only the header) is deleted. Files open by a writer are not touched.
- **Files of the previous versions.** They have a `<file>.metadata.transaction` sidecar with the committed offset. A completed one is left as is and is still moved / deleted with all its sidecars (`LogFile.moveTo`, `delete`). A non-completed one is recovered to its committed offset if the scan confirms it is a frame boundary (the data written after it was never acknowledged, the client resends it — no duplicates), otherwise to the scan result; `.metadata.transaction` is kept. LZ4 / zstd frames without the content checksum (old files, not yet updated clients) are accepted on structure and decompression. A version that has any of the files (data, metadata, completed, transaction) is never reused.
- **Sync modes** (`syncMode`): `ALWAYS` — every buffer is synced before the ACK, nothing acknowledged is lost; `PERIODIC` — synced every `syncInterval` and on close, an OS crash / power loss may lose up to `syncInterval` of acknowledged data; `ON_CLOSE` — synced on rotation / shutdown only. A process crash loses nothing in any mode (page cache + recovery). Delivery is at-least-once: a buffer written but not acknowledged is resent by the client. A failed sync in `PERIODIC` / `ON_CLOSE` fails the file (a retried `fsync` after an error guarantees nothing; earlier versions just retried it on the next period): the file is closed at once without `.metadata.completed` (recovered on the next start), the next buffer goes to the next file version, `FileWriterNotification.fileFailed(file, cause)` is called (`DiskLoggerBackend` forwards it to its listeners) and `logstream_logging_disk_sync_failures` is incremented. The data written since the last successful sync of that file may be lost by an OS crash.

### Key parameters

| Parameter | Default                                                                                                                                                  | Description |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| `logDirectory` | (required)                                                                                                                                                | Root directory; hostname is appended as a subdirectory |
| `timestamp` | (required)                                                                                                                                                | Bucket cadence (`BPH_1` … `BPH_12`) |
| `bufferSize` | `102400` (100 KB)                                                                                                                                         | Per-writer in-memory write buffer |
| `writerConfiguration` | `new WriterConfiguration()`                                                                                                                              | Currently just `tsv.dateTime32Format` — date format used for TSV `DATETIME32` columns |
| `filePattern` | `{{ YEAR }}-{{ MONTH }}/{{ DAY }}/{{ LOG_TYPE }}_v{{ LOG_VERSION }}-{{ YEAR }}-{{ MONTH }}-{{ DAY }}-{{ HOUR }}-{{ INTERVAL }}.tsv.gz` | Output path template — the codec extension (`.rb.gz` / `.rb.lz4` / `.rb.zst`) is always appended on top of this, so the actual default gzip output extension is `.tsv.gz.rb.gz` |
| `requiredFreeSpace` | 2 GB                                                                                                                                                      | Minimum free space; backend reports FAILED below this threshold |
| `maxVersions` | 20                                                                                                                                                        | Maximum concurrent file versions per log ID |
| `refreshInitDelay` | 10 s                                                                                                                                                      | Delay before first writer flush |
| `refreshPeriod` | 10 s                                                                                                                                                      | How often writers are flushed and evicted |
| `syncMode` | `ALWAYS`                                                                                                                                                  | When the written data is synced to the device: `ALWAYS`, `PERIODIC`, `ON_CLOSE` (see above) |
| `syncInterval` | 1 s                                                                                                                                                       | `PERIODIC` sync period |
| `recoverOnStart` | `true`                                                                                                                                                    | Repair the files left without `.metadata.completed` by a crash on `start()` |
| `maxShards` | 1                                                                                                                                                         | The most files written at the same time per `(LogId, codec)`, 1..999 (see Shards); read when a key's writers are created |
| `shardGrowWait` | 5 ms                                                                                                                                                      | Mean wait for a busy shard per window above which the active shards double |
| `shardShrinkWindows` | 30                                                                                                                                                        | Quiet windows (`refreshPeriod` each) in a row before the active shards drop by one |

### File pattern tokens

`filePattern` is not simple string substitution — it's rendered by the [`oap-template`](../oap-template/README.md) engine (`LogIdTemplate.render()`, via the same `TemplateEngine` passed into `DiskLoggerBackend`), so the full template syntax is available: `{{ VAR }}` expressions, `{{% if COND %}} ... {{% else %}} ... {{% end %}}` conditional blocks, `and`/`or`, etc. — see `oap-template`'s README for the complete syntax. The render context is a flat `Map<String, String>` of predefined variables (below) merged with every entry of the `properties` map passed to `TemplateLogger.log()` / `backend.log()`, so custom properties can be referenced (and branched on) directly in the pattern, e.g. `{{% if ORGANIZATION and ACCOUNT }}{{ ORGANIZATION }}/{{ ACCOUNT }}/{{% end }}...`.

`filePattern` must contain a `{{ LOG_VERSION }}` token and an `{{ INTERVAL }}` (or `{{ MINUTE }}`) token — the backend refuses to start otherwise (the former is required unconditionally; the latter is required so bucket rotation can be detected).

Predefined variables:

| Token                                                  | Value |
|--------------------------------------------------------|---|
| `{{ LOG_TYPE }}`                                       | Log type string from `log()` call |
| `{{ LOG_VERSION }}`                                    | Content hash + replica id + file version (required) |
| `{{ YEAR }}`, `{{ MONTH }}`, `{{ DAY }}`, `{{ HOUR }}` | UTC date components |
| `{{ MINUTE }}`                                         | Current UTC minute-of-hour (zero-padded) — **not** related to `{{ INTERVAL }}` |
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

| Parameter | Description | Runtime change |
|---|---|---|
| `sender` | `MessageSender` from `oap-message-client` — handles TCP connection and retry | — |
| `bufferSize` | Per-log-ID in-memory buffer; rows are accumulated until the buffer is full or the flush interval fires. A `BufferConfigurationMap` gives per-log-type settings (`bufferSize`, `priority`) by `logType` pattern, the first matching configuration wins (`DEFAULT` = one size for all) | `setBufferSize( name, size )` / `setBufferSize( size )` (all configurations), `getBufferSizes()` — the next buffer of every log ID has the new size; the current non-empty one is sent as is |
| `priority` | Per configuration (`new BufferConfiguration( bufferSize, pattern, priority )`, default `0`): `oap-message` priority of the buffers — higher is sent earlier, see [`MessageSender` priority](../../oap-message/oap-message-client/README.md#priority) | `setPriority( name, p )` / `setPriority( p )` (all configurations), `getPriorities()` — from the next `sendAsync()`, for all not yet sent buffers (current and ready); messages already passed to the `MessageSender` keep their priority |
| `flushInterval` | How often buffered rows are sent to the server (milliseconds); `<= 0` — no automatic sending, the application calls `sendAsync()` | `setFlushInterval( ms )`, `getFlushInterval()` — rescheduled immediately |
| `maxBuffers` | Maximum number of ready (not yet sent) buffers before the backend reports FAILED (default 5000) | `setMaxBuffers( n )`, `getMaxBuffers()` — from the next `availabilityReport()` |
| `compression` | Payload codec: `ZSTD` (default, level 1), `LZ4`, `GZIP` | `setCompression( codec )`, `setGzipLevel` / `setLz4Level` / `setZstdLevel` — from the next `sendAsync()` |
| `sendThreads` | Number of threads compressing the ready buffers and passing them to the sender (default 2; the thread of `sendAsync()` is one of them) | `setSendThreads( n )`, `getSendThreads()` — from the next `sendAsync()` |
| `stripes` | Constructor parameter: max number of buffers filled concurrently per log ID, relative to the number of cores: `0` (default) — cores, `N` — N, `-N` — cores - N, `1.5` — cores * 1.5, `-1.5` — cores / 1.5; rounded up to a power of 2. A log ID starts with one buffer; the number doubles only when the writing threads contend for it | — |

All setters validate the value (`IllegalArgumentException`) and log the change; `setFlushInterval` after `close()` throws `IllegalStateException`. Buffers that are already ready are sent unchanged.

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
typed.log( event, Map.of( "region", "eu" ), "EVENT_TYPE" );
```

See [docs/RowBinaryObjectLogger.md](docs/RowBinaryObjectLogger.md) for the full API reference, supported types, schema format, and a complete example.

---

## See also

- [`oap-template`](../oap-template/README.md) — the template engine that renders `filePattern` (see [File pattern tokens](#file-pattern-tokens)) and every `DictionaryTemplate`/row renderer (`TemplateLogger`, `RowBinaryObjectLogger`) built on top of it; full syntax reference (`{{ }}` expressions, `{{% if %}}` blocks, functions, accumulators).
- [docs/RowBinaryObjectLogger.md](docs/RowBinaryObjectLogger.md) — full `RowBinaryObjectLogger` API reference.
