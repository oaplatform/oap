# oap-storage-cloud

Provider-agnostic cloud object storage API for the OAP platform. A single `FileSystem` facade dispatches to pluggable backends (AWS S3, Google Cloud Storage, Azure Blob, local filesystem) selected by URI scheme.

Depends on: `oap-stdlib`

## `CloudURI`

Every path is represented as a `CloudURI`:

```
scheme://container/path/to/object
  │          │          │
  │          │          └─ object key (no leading slash)
  │          └─ bucket / container name
  └─ backend scheme
```

| Scheme | Backend |
|---|---|
| `s3` | AWS S3 (requires `oap-storage-cloud-aws-s3` on classpath) |
| `gcs` | Google Cloud Storage |
| `ab` | Azure Blob Storage |
| `file` | Local filesystem |
| `ftp` | FTP (requires `oap-storage-cloud-ftp` on classpath) |
| `ftps` | FTP over TLS (requires `oap-storage-cloud-ftp` on classpath) |

```java
CloudURI uri = new CloudURI( "s3://my-bucket/data/report-2024-06-01.json" );
// uri.scheme    = "s3"
// uri.container = "my-bucket"
// uri.path      = "data/report-2024-06-01.json"

// Builder-style copies
CloudURI other = uri.withPath( "data/report-2024-06-02.json" );
```

---

## `FileSystemConfiguration`

Holds per-scheme (and optionally per-container) credentials and settings. Keys follow the pattern:

```
fs.<scheme>[.<container>].clouds.<property>
```

The `fs.default.clouds.scheme` and `fs.default.clouds.container` entries define the default used by `FileSystem.getDefaultURL(path)`.

```java
FileSystemConfiguration config = new FileSystemConfiguration( Map.of(
    // S3 credentials (apply to all buckets unless overridden per-container)
    "fs.s3.clouds.identity",   "AKIAIOSFODNN7EXAMPLE",
    "fs.s3.clouds.credential", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
    "fs.s3.clouds.region",     "us-east-1",

    // Default target
    "fs.default.clouds.scheme",    "s3",
    "fs.default.clouds.container", "my-bucket"
) );
```

Values support `${env.VAR_NAME}` and `${system.property}` substitution.

### OAP module configuration

```hocon
name = my-app
dependsOn = [oap-storage-cloud]

services {
  oap-storage-cloud.oap-cloud-configuration.parameters {
    configuration {
      fs.s3.clouds.identity   = ${?AWS_ACCESS_KEY_ID}
      fs.s3.clouds.credential = ${?AWS_SECRET_ACCESS_KEY}
      fs.s3.clouds.region     = us-east-1

      fs.default.clouds.scheme    = s3
      fs.default.clouds.container = my-bucket
    }
  }
}
```

---

## `FileSystem`

Stateless facade that routes calls to the right backend by URI scheme. Backend instances are cached and closed with `FileSystem.close()`; the cache key granularity depends on the backend — most (S3, `file`) are cached per scheme, while backends implementing `ContainerScopedCloudApi` (FTP/FTPS) are cached per scheme **and** container, since `container` identifies a distinct server connection for them rather than a request-scoped parameter.

```java
FileSystem fs = new FileSystem( config );

// Upload
CloudURI dest = new CloudURI( "s3://my-bucket/reports/2024-06-01.json" );
fs.upload( dest, BlobData.builder()
    .content( jsonBytes )
    .tags( Map.of( "env", "prod" ) )
    .build() );

// Download to local path
fs.downloadFile( dest, Path.of( "/tmp/report.json" ) );

// Stream read
try( InputStream in = fs.getInputStream( dest ) ) { ... }

// Stream write
try( OutputStream out = fs.getOutputStream( dest, Map.of() ) ) { ... }

// Copy between URIs (may cross backends)
fs.copy( src, dest, Map.of( "copied", "true" ) );

// List objects
PageSet<? extends FileSystem.StorageItem> page = fs.list(
    new CloudURI( "s3://my-bucket/reports/" ),
    ListOptions.builder().maxResults( 100 ).build()
);

// Metadata only (no download)
FileSystem.StorageItem meta = fs.getMetadata( dest );
// meta.getName(), meta.getSize(), meta.getLastModified(), meta.getETag(), meta.getContentType()

// Default URL from configured scheme + container
CloudURI defaultUri = fs.getDefaultURL( "reports/today.json" );
```

### Operations reference

All methods are synchronous/blocking.

| Method | Description |
|---|---|
| `getInputStream(uri)` | Open object for reading |
| `getOutputStream(uri, tags)` | Open object for writing |
| `upload(uri, blobData)` | Write bytes / stream with optional tags |
| `downloadFile(uri, localPath)` | Save object to a local file |
| `copy(src, dest, tags)` | Cross-backend copy via stream |
| `list(uri, options)` | List objects under a prefix; returns `PageSet` |
| `getMetadata(uri)` | Fetch object metadata without body |
| `blobExists(uri)` | Check whether an object exists |
| `deleteBlob(uri)` | Delete a single object |
| `containerExists(uri)` | Check whether a bucket/container exists |
| `createContainer(uri)` | Create a bucket/container |
| `deleteContainer(uri)` | Delete an empty bucket/container |
| `deleteContainerIfEmpty(uri)` | Delete only if empty; returns `boolean` |
| `getDefaultURL(path)` | Build a `CloudURI` using the configured default scheme + container |
| `toLocalFilePath(path)` | Convert a `java.nio.Path` to a `file://` `CloudURI` |

---

## `FileSystemCloudApi`

Interface implemented by each backend. Register a new implementation by placing a `cloud-service.properties` file on the classpath:

```properties
# cloud-service.properties
s3=com.example.MyS3CloudApi
```

The class must have a constructor `(FileSystemConfiguration, String container)`.

Every method is a required synchronous, blocking method.

---

## AWS S3

Add the `oap-storage-cloud-aws-s3` artifact to your dependencies. The `s3://` scheme is registered automatically via `cloud-service.properties` — no additional wiring is needed.

Required configuration keys for S3:

| Key | Description |
|---|---|
| `fs.s3.clouds.identity` | AWS access key ID |
| `fs.s3.clouds.credential` | AWS secret access key |
| `fs.s3.clouds.region` | AWS region (e.g. `us-east-1`) |
| `fs.s3.clouds.endpoint` | Override endpoint URL (e.g. for LocalStack) |
| `fs.s3.clouds.s3.virtual-host-buckets` | `false` for path-style access (LocalStack, MinIO) |

---

## FTP

Add the `oap-storage-cloud-ftp` artifact to your dependencies. The `ftp://` and `ftps://` schemes are registered automatically via `cloud-service.properties`.

Unlike `file`, FTP/FTPS **require** a container: the URI's host (optionally `:port`) identifies the FTP server to connect to. `ftp://ftp.example.com:2121/reports/2024-06-01.json` connects to `ftp.example.com:2121` and addresses the remote path `reports/2024-06-01.json`. A URI with no host (e.g. `ftp:///reports/file.txt`, `ftp://`) throws `CloudException`.

Each distinct `host[:port]` gets its own pooled connection set — using two different FTP hosts through the same `FileSystem` instance connects to both independently, they don't share a connection pool.

FTP control connections (TCP connect + login) are pooled per backend instance using [Apache Commons Pool 2](https://commons.apache.org/proper/commons-pool/) — operations borrow a connection from the pool and return it when done instead of reconnecting/logging in on every call. Pooled connections are validated with an FTP `NOOP` before reuse, so idle connections dropped by the server/firewall are transparently replaced.

Required/optional configuration keys:

| Key | Description |
|---|---|
| `fs.ftp.clouds.identity` | FTP username (default `anonymous`) |
| `fs.ftp.clouds.credential` | FTP password |
| `fs.ftp.clouds.passive-mode` | `true`/`false` (default `true`) |
| `fs.ftp.clouds.remove-empty-folders` | `true` to delete now-empty parent directories after a blob delete (default `false`) |
| `fs.ftp.clouds.pool-max-size` | Max pooled FTP connections per backend instance (default `8`) |
| `fs.ftp.clouds.pool-max-wait-millis` | Max time to wait for a pooled connection before failing, in milliseconds (default `30000`) |
| `fs.ftp.clouds.connect-timeout-millis` | TCP connect timeout, in milliseconds (default `30000`) |
| `fs.ftp.clouds.default-timeout-millis` | Timeout applied to the socket immediately after connecting, before login, in milliseconds (default `30000`) |
| `fs.ftp.clouds.so-timeout-millis` | Timeout while waiting for control-connection responses, in milliseconds (default `30000`) |
| `fs.ftps.clouds.tls-mode` | `explicit` (default) or `implicit` |
| `fs.ftps.clouds.trust-all` | `true` to skip server certificate validation (e.g. self-signed certs in tests) |

```java
CloudURI dest = new CloudURI( "ftp://ftp.example.com/reports/2024-06-01.json" );
fs.upload( dest, BlobData.builder().content( jsonBytes ).build() );
```

### Per-host FTP configuration overrides

Config keys follow `fs.<scheme>.<container>.clouds.<property>`, where `<container>` must exactly match the runtime `host[:port]` value derived from the URI. Since config keys are dot-delimited, a literal dot inside the host must be escaped as `\.` so it isn't parsed as a key-path separator — a host with no dots (just `localhost` or `localhost:12345`, port digits included) needs no escaping:

```
fs.ftp.localhost:12345.clouds.identity = as
fs.ftp.ftp\.server1\.com.clouds.identity = as
```

A container-specific entry overrides `fs.ftp.clouds.<property>` only for that exact host; other hosts keep falling back to the scheme-wide default.

`createContainer`/`deleteContainerIfEmpty` always return `false`, and `deleteContainer` throws `CloudException` — there's no container to create or delete. FTP also has no object-tagging concept, so tags passed to `upload`/`getOutputStream` are ignored.
