# oap-storage-cloud-test

Test utilities for `oap-storage-cloud`. Provides a LocalStack S3 container (via Testcontainers) so tests can exercise `FileSystem` against a real S3-compatible endpoint.

Depends on: `oap-storage-cloud-aws-s3`

**Requires Docker** to be running on the test host.

## `S3MockFixture`

Starts a `localstack/localstack` Docker container exposing the S3 service. Implements `AbstractFixture` — wire it with TestNG `@Listeners(Fixtures.class)`.

```java
@Listeners( Fixtures.class )
public class ReportUploaderTest {

    @Fixture
    S3MockFixture s3 = new S3MockFixture()
        .withInitialBuckets( "reports" );

    @Test
    public void uploadsReport() throws Exception {
        FileSystemConfiguration config = s3.getFileSystemConfiguration( "reports" );
        FileSystem fs = new FileSystem( config );

        CloudURI dest = new CloudURI( "s3://reports/2024-06-01.json" );
        fs.upload( dest, BlobData.builder().content( "{\"ok\":true}".getBytes() ).build() );

        assertThat( fs.blobExists( dest ) ).isTrue();
    }
}
```

### Fixture variables

| Variable | Value |
|---|---|
| `HTTP_PORT` | Randomly assigned port for LocalStack HTTP |

### API

| Method | Description |
|---|---|
| `withInitialBuckets(names...)` | Create these S3 buckets when the container starts |
| `getFileSystemConfiguration(container)` | Returns a pre-wired `FileSystemConfiguration` pointing at this LocalStack instance with the given default bucket |
| `getHttpPort()` | The port LocalStack is bound to |
| `uploadFile(container, name, Path, tags)` | Upload a local file to the mock S3 |
| `uploadFile(container, name, content, tags)` | Upload string content to the mock S3 |
| `readTags(container, path)` | Read the tag map for an object |
| `createFolder(container, path, tags)` | Create an S3 "folder" (zero-byte object with trailing `/`) |
| `deleteAll()` | Delete all objects in all buckets |

### Wiring `FileSystem`

`getFileSystemConfiguration(container)` returns a `FileSystemConfiguration` with:

```
fs.s3.clouds.identity   = access_key
fs.s3.clouds.credential = access_secret
fs.s3.clouds.region     = aws-global
fs.s3.clouds.endpoint   = http://localhost:<HTTP_PORT>
fs.s3.clouds.s3.virtual-host-buckets = false   (path-style, required for LocalStack)
fs.default.clouds.scheme    = s3
fs.default.clouds.container = <container>
```

Use it directly with `new FileSystem(config)` or pass it to a `KernelFixture`-based test via `application.conf` variable substitution.

### Seeding files from tests

```java
// Upload a file from the local filesystem
s3.uploadFile( "reports", "2024-06-01.json", Path.of( "src/test/resources/sample.json" ) );

// Upload string content with metadata tags
s3.uploadFile( "reports", "2024-06-01.json", "{\"status\":\"ok\"}", Map.of( "env", "test" ) );

// Verify tags (note: S3Mock URL-encodes tags; S3MockFixture URL-decodes them for you)
Map<String, String> tags = s3.readTags( "reports", "2024-06-01.json" );
assertThat( tags ).containsEntry( "env", "test" );
```
