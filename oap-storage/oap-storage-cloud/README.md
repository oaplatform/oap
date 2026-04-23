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

Stateless facade that routes calls to the right backend by URI scheme. Backends are cached per scheme and closed with `FileSystem.close()`.

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

Every synchronous method has an `...Async` counterpart returning `CompletableFuture`.

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

Every method has a required `...Async(CloudURI, ...)` variant and a default synchronous wrapper that calls `.join()`.

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
