# oap-storage-cloud

Provider-agnostic cloud object storage API for the OAP platform. A single `FileSystem` facade dispatches to pluggable backends (AWS S3, Google Cloud Storage, Azure Blob, local filesystem) selected by URI scheme.

Depends on: `oap-stdlib`

## `CloudURI`

Every path is represented as a `CloudURI`, addressed by **configurationId**, not by backend/container directly:

```
fs://configurationId/path/to/object
  │         │              │
  │         │              └─ object key (no leading slash)
  │         └─ named target: resolves to a backend scheme + connection (container) via config
  └─ fixed literal scheme
```

The configurationId is the only thing the URI carries — which backend scheme it maps to (`s3`, `ftp`, `smb`, ...) and which concrete connection (bucket, host[:port], host[:port]/share, ...) it uses are both resolved from `FileSystemConfiguration` at call time (see below).

| Scheme | Backend |
|---|---|
| `s3` | AWS S3 (requires `oap-storage-cloud-aws-s3` on classpath) |
| `gcs` | Google Cloud Storage |
| `ab` | Azure Blob Storage |
| `file` | Local filesystem |
| `ftp` | FTP (requires `oap-storage-cloud-ftp` on classpath) |
| `ftps` | FTP over TLS (requires `oap-storage-cloud-ftp` on classpath) |
| `smb` | SMB/CIFS (requires `oap-storage-cloud-smb` on classpath) |

```java
CloudURI uri = new CloudURI( "fs://my-configuration-id/data/report-2024-06-01.json" );
// uri.configurationId = "my-configuration-id"
// uri.path            = "data/report-2024-06-01.json"

// equivalent, canonical constructor
CloudURI uri2 = new CloudURI( "my-configuration-id", "data/report-2024-06-01.json" );

// Builder-style copies
CloudURI other = uri.withPath( "data/report-2024-06-02.json" );
CloudURI otherConfigurationId = uri.withConfigurationId( "other-configuration-id" );
```

### Migrating a legacy `scheme://container/path` string

`FileSystem.resolve(String)` accepts the old `scheme://container/path` shape (as used before configurationIds existed) and maps it onto whichever configurationId is configured for that scheme+container, falling back to the scheme's own name (the "bare configurationId == scheme name" convention) when the container matches the scheme-wide one:

```java
CloudURI uri = fileSystem.resolve( "s3://my-bucket/data/report-2024-06-01.json" );
```

Throws `CloudException` if no configurationId can be resolved for the given scheme+container, and always throws for `file://...` (local paths have no container to match against — use `fs://file/<path>` or `new CloudURI("file", path)` directly). `fs://...` input passes straight through to `new CloudURI(uri)`.

---

## `FileSystemConfiguration`

Holds per-scheme, per-configurationId, and global-default credentials and settings. Keys follow the pattern:

```
fs.<scheme>.<property>[.<configurationId>]
fs.default.<property>
```

Looking up a property for a given `(scheme, configurationId)` tries, in order:
1. `fs.<scheme>.<property>.<configurationId>` — configurationId-specific override
2. `fs.<scheme>.<property>` — scheme-wide default
3. `fs.default.<property>` — global fallback, for any property, across every scheme

`fs.default.*` is entirely optional — there's no required key under it, and a config with no `fs.default.*` at all is perfectly valid.

### Overriding via system properties and environment variables

Any `fs.*` key can also be supplied as a JVM system property or an OS environment variable, without touching the map/HOCON config — useful for ops-level overrides. Priority, highest first:

1. Environment variable
2. JVM system property
3. The `Map`/HOCON passed to the constructor

```bash
java -Dfs.s3.container=override-bucket -jar app.jar
# or
export fs.s3.container=override-bucket
```

`_` is not a valid character in this key namespace — any `_` in a system-property or environment-variable key is normalized to `-` before matching, so `fs.file.filesystem.remove_empty_folders` behaves identically to `fs.file.filesystem.remove-empty-folders`. This normalization applies only to system properties/env vars, not to keys in the `Map`/HOCON argument.

### ConfigurationIds

A configurationId is a named target (a backend scheme + connection). It's **detected from configuration** — no separate declaration list, and every caller states the configurationId it means explicitly (`FileSystem` has no notion of "the default one"):

- Any configurationId is registered the moment it appears in a `fs.<scheme>.container.<configurationId>` key — `container` is the anchor property every configurationId needs to actually connect to something, so declaring it is what makes the configurationId exist.
- A bare configurationId equal to an installed backend's scheme name (`fs://ftp/...`, `fs://file/...`) resolves implicitly with **zero** configurationId-related config, so single-target setups need nothing beyond the scheme-wide properties.
- `FileSystemConfiguration.required(String configurationId)` validates up front that a configurationId is registered to some scheme, throwing `CloudException` immediately if not.

```java
FileSystemConfiguration config = new FileSystemConfiguration( Map.of(
    // S3 credentials (apply to every configurationId on this scheme unless overridden per-configurationId)
    "fs.s3.identity",   "AKIAIOSFODNN7EXAMPLE",
    "fs.s3.credential", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
    "fs.s3.region",     "us-east-1",
    "fs.s3.container",  "my-bucket"
    // "s3" is both the scheme and the configurationId here (self-name convention) — no further registration needed
) );
```

Values support `${env.VAR_NAME}` and `${system.property}` substitution.

### Multi-configurationId example

```
fs.ftp.container   = ftp.example.com:21
fs.ftp.identity    = shared-user
fs.ftp.credential  = shared-pass

# the primary account: container is repeated so this configurationId gets registered
fs.ftp.container.primary    = ftp.example.com:21

# a second account on the same server: only identity/credential differ,
# container is repeated so this configurationId gets registered
fs.ftp.container.secondary  = ftp.example.com:21
fs.ftp.identity.secondary   = other-user
fs.ftp.credential.secondary = other-pass
```

`fs://primary/...` connects as `shared-user`; `fs://secondary/...` connects to the same host as `other-user`.

### OAP module configuration

```hocon
name = my-app
dependsOn = [oap-storage-cloud]

services {
  oap-storage-cloud.oap-cloud-configuration.parameters {
    configuration {
      fs.s3.identity   = ${?AWS_ACCESS_KEY_ID}
      fs.s3.credential = ${?AWS_SECRET_ACCESS_KEY}
      fs.s3.region     = us-east-1
      fs.s3.container  = my-bucket
    }
  }
}
```

---

## `FileSystem`

Stateless facade that routes calls to the right backend by resolving the URI's configurationId to a scheme (via `FileSystemConfiguration`, falling back to an installed backend's scheme name for a bare self-named configurationId). Backend instances are cached and closed with `FileSystem.close()`, keyed by `scheme://configurationId` — every backend is configurationId-scoped, since configurationId is the stable per-connection identity (this also means two configurationIds on the same S3 bucket with different credentials get independent cached clients, not a shared one).

```java
FileSystem fs = new FileSystem( config );

// Upload
CloudURI dest = new CloudURI( "fs://my-configuration-id/reports/2024-06-01.json" );
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
    new CloudURI( "my-configuration-id", "reports/" ),
    ListOptions.builder().maxResults( 100 ).build()
);

// Metadata only (no download)
FileSystem.StorageItem meta = fs.getMetadata( dest );
// meta.getName(), meta.getSize(), meta.getLastModified(), meta.getETag(), meta.getContentType()

// Build a CloudURI for a given configurationId
CloudURI defaultUri = fs.getDefaultURL( "my-configuration-id", "reports/today.json" );

// Migrate a legacy scheme://container/path string to a configurationId-based CloudURI
CloudURI legacyResolved = fs.resolve( "s3://my-bucket/reports/today.json" );
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
| `getDefaultURL(configurationId, path)` | Build a `CloudURI` for the given configurationId, normalizing path separators |
| `resolve(legacyUri)` | Map a legacy `scheme://container/path` string onto the configurationId configured for that scheme+container |
| `toLocalFilePath(configurationId, path)` | Convert a `java.nio.Path` to a `fs://<configurationId>/...` `CloudURI` for the given `file` configurationId |

---

## `FileSystemCloudApi`

Interface implemented by each backend. Register a new implementation by placing a `cloud-service.properties` file on the classpath:

```properties
# cloud-service.properties
s3=com.example.MyS3CloudApi
```

The class must have a constructor `(FileSystemConfiguration, String configurationId)` — each backend resolves its own connection details (bucket, host[:port], ...) from config via `fileSystemConfiguration.getOrThrow(scheme, configurationId, "container")`, rather than receiving them pre-parsed.

Every method is a required synchronous, blocking method.

---

## AWS S3

Add the `oap-storage-cloud-aws-s3` artifact to your dependencies. The `s3://` scheme is registered automatically via `cloud-service.properties` — no additional wiring is needed.

Required configuration keys for S3 (each supports the configurationId-override / scheme-wide / `fs.default.*` fallback chain):

| Key | Description |
|---|---|
| `fs.s3.container` | Bucket name |
| `fs.s3.identity` | AWS access key ID |
| `fs.s3.credential` | AWS secret access key |
| `fs.s3.region` | AWS region (e.g. `us-east-1`) |
| `fs.s3.endpoint` | Override endpoint URL (e.g. for LocalStack); when set, path-style access is forced automatically |
| `fs.s3.filesystem.basedir` | Optional key prefix within the bucket; every object key is resolved as `<basedir>/<path>` and `list()` results are returned relative to it, same as `file`'s `filesystem.basedir` |

---

## FTP

Add the `oap-storage-cloud-ftp` artifact to your dependencies. The `ftp://` and `ftps://` schemes are registered automatically via `cloud-service.properties`.

Unlike `file`, FTP/FTPS **require** a container: `fs.ftp.container[.<configurationId>]` (`host[:port]`) identifies the FTP server a configurationId connects to. `getOrThrow` throws `CloudException` if no container can be resolved for the configurationId.

Each distinct configurationId gets its own pooled connection set — two configurationIds pointing at different hosts (or even the same host with different credentials) never share a connection pool.

FTP control connections (TCP connect + login) are pooled per backend instance using [Apache Commons Pool 2](https://commons.apache.org/proper/commons-pool/) — operations borrow a connection from the pool and return it when done instead of reconnecting/logging in on every call. Pooled connections are validated with an FTP `NOOP` before reuse, so idle connections dropped by the server/firewall are transparently replaced.

Required/optional configuration keys (each supports the configurationId-override / scheme-wide / `fs.default.*` fallback chain):

| Key | Description |
|---|---|
| `fs.ftp.container` | `host[:port]` of the FTP server (default port `21`) |
| `fs.ftp.identity` | FTP username (default `anonymous`) |
| `fs.ftp.credential` | FTP password |
| `fs.ftp.passive-mode` | `true`/`false` (default `true`) |
| `fs.ftp.remove-empty-folders` | `true` to delete now-empty parent directories after a blob delete (default `false`) |
| `fs.ftp.pool-max-size` | Max pooled FTP connections per backend instance (default `8`) |
| `fs.ftp.pool-max-wait-millis` | Max time to wait for a pooled connection before failing, in milliseconds (default `30000`) |
| `fs.ftp.connect-timeout-millis` | TCP connect timeout, in milliseconds (default `30000`) |
| `fs.ftp.default-timeout-millis` | Timeout applied to the socket immediately after connecting, before login, in milliseconds (default `30000`) |
| `fs.ftp.so-timeout-millis` | Timeout while waiting for control-connection responses, in milliseconds (default `30000`) |
| `fs.ftps.tls-mode` | `explicit` (default) or `implicit` |
| `fs.ftps.trust-all` | `true` to skip server certificate validation (e.g. self-signed certs in tests) |
| `fs.ftp.filesystem.basedir` | Optional remote path prefix; every path is resolved as `<basedir>/<path>` and `list()` results are returned relative to it, same as `file`'s `filesystem.basedir` |

```java
CloudURI dest = new CloudURI( "fs://my-ftp-configuration-id/reports/2024-06-01.json" );
fs.upload( dest, BlobData.builder().content( jsonBytes ).build() );
```

### Per-configurationId FTP configuration overrides

Declaring `fs.ftp.container.<configurationId>` registers `<configurationId>` and gives it its own connection target; pairing it with `fs.ftp.identity.<configurationId>`/`fs.ftp.credential.<configurationId>` gives that configurationId its own credentials too (see the [multi-configurationId example](#multi-configurationid-example) above). Since the lookup mechanism probes exact key strings rather than positionally splitting stored keys, configurationId names need no dot-escaping, unlike the old per-container scheme:

```
fs.ftp.container.reporting-server = ftp.server1.example.com:21
fs.ftp.identity.reporting-server  = as
```

A configurationId-specific entry overrides `fs.ftp.<property>` only for that exact configurationId; other configurationIds on the same scheme keep falling back to the scheme-wide default, and ultimately to `fs.default.<property>`.

`createContainer`/`deleteContainerIfEmpty` always return `false`, and `deleteContainer` throws `CloudException` — there's no container to create or delete. FTP also has no object-tagging concept, so tags passed to `upload`/`getOutputStream` are ignored.

---

## SMB

Add the `oap-storage-cloud-smb` artifact to your dependencies. The `smb://` scheme (backed by [jcifs-ng](https://github.com/codelibs/jcifs)) is registered automatically via `cloud-service.properties`.

Like FTP, SMB **requires** a container, but `fs.smb.container[.<configurationId>]` is `host[:port]/share` (default port `445`) — the share is part of the container, not the path. `fs.smb.container = fileserver:445/reports` addresses share `reports` on `fileserver:445`; the object path is relative to that share. `getOrThrow` throws `CloudException` if no container (or no share segment within it) can be resolved for the configurationId.

Each configurationId gets its own backend instance holding one `CIFSContext` — jcifs-ng manages the underlying SMB session/connection reuse internally, so (unlike FTP) there's no separate connection-pool configuration. Two configurationIds never share a session, even if they point at the same share.

Required/optional configuration keys (each supports the configurationId-override / scheme-wide / `fs.default.*` fallback chain):

| Key | Description |
|---|---|
| `fs.smb.container` | `host[:port]/share` |
| `fs.smb.identity` | SMB username (default `guest`) |
| `fs.smb.credential` | SMB password |
| `fs.smb.domain` | NTLM domain/workgroup (default empty) |
| `fs.smb.filesystem.basedir` | Optional path prefix within the share; every path is resolved as `<basedir>/<path>` and `list()` results are returned relative to it, same as `file`'s `filesystem.basedir` |

```java
CloudURI dest = new CloudURI( "fs://my-smb-configuration-id/reports/2024-06-01.json" );
fs.upload( dest, BlobData.builder().content( jsonBytes ).build() );
```

`createContainer`/`deleteContainerIfEmpty` always return `false`, and `deleteContainer` throws `CloudException` — SMB shares aren't created/deleted through this client. SMB has no object-tagging concept, so tags passed to `upload`/`getOutputStream` are ignored.
