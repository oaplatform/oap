# Testing

OAP provides a fixture lifecycle framework, assertion helpers, and integration utilities spread across several test-support modules.

| Module | Artifact | What it provides |
|---|---|---|
| `oap-stdlib-test` | `oap-stdlib-test` | Core fixtures, `Asserts`, `JsonAsserts`, `MetricsFixture`, `Benchmark` |
| `oap-application-test` | `oap-application:oap-application-test` | `KernelFixture` — full Kernel boot |
| `oap-storage-mongo-test` | `oap-storage:oap-storage-mongo-test` | `MongoFixture` — embedded MongoDB |
| `oap-storage-cloud-test` | `oap-storage:oap-storage-cloud-test` | `S3MockFixture` — LocalStack S3 |
| `oap-template-test` | `oap-formats:oap-template-test` | `TemplateEngineFixture` |

---

## Fixture lifecycle

Fixtures are lifecycle-managed objects that set up and tear down resources around TestNG tests. Three scopes are supported:

| Scope | Lifetime |
|---|---|
| `METHOD` (default) | Around each test method |
| `CLASS` | Around the test class (once per class) |
| `SUITE` | Around the entire test run (once per JVM) |

---

## `AbstractFixture<Self>`

Base class for all fixtures. Override `before()` and `after()` to set up and tear down resources.

```java
public class MyServiceFixture extends AbstractFixture<MyServiceFixture> {
    public MyService service;

    @Override
    protected void before() {
        service = new MyService();
        service.start();
    }

    @Override
    protected void after() {
        service.stop();
    }
}
```

### Key methods

| Method | Description |
|---|---|
| `withScope(Scope)` | Change the fixture scope (METHOD or CLASS; use `Fixtures.suiteFixture()` for SUITE) |
| `define(key, value)` | Store a named property (available as a template variable in `application.conf`) |
| `definePort(key)` | Allocate a free TCP port, store it under `key`, and return it |
| `getProperty(key)` | Retrieve a stored property |
| `addChild(fixture)` | Nest another fixture; its lifecycle follows the parent |
| `defineLocalClasspath(key, class, resource)` | Define a `classpath(...)` string pointing to a test resource |
| `defineClasspath(key, resource)` | Define a `classpath(...)` string from a resource location |
| `definePath(key, path)` | Define a `path(...)` string |
| `defineURL(key, url)` | Define a `url(...)` string |

---

## `Fixtures`

TestNG listener that drives fixture lifecycle. Add `@Listeners(Fixtures.class)` to your test class (or extend `Fixtures`), then register fixtures with `fixture(f)` in the constructor.

```java
@Listeners( Fixtures.class )
public class OrderServiceTest extends Fixtures {
    private final TestDirectoryFixture dir = fixture( new TestDirectoryFixture() );
    private final MyServiceFixture svc    = fixture( new MyServiceFixture() );

    @Test
    public void createsOrder() {
        svc.service.create( new Order( "o-1" ) );
        assertThat( svc.service.find( "o-1" ) ).isPresent();
    }
}
```

Teardown runs in **reverse registration order**. All teardown errors are collected and re-thrown after all fixtures complete.

### Suite-scoped fixtures

```java
// Shared across all test classes in the run
private static final MyFixture shared = Fixtures.suiteFixture( new MyFixture() );
```

---

## `TestDirectoryFixture`

Provides an isolated, temporary directory under `/tmp/test/`. The directory is created before the test and deleted after.

```java
TestDirectoryFixture dir = fixture( new TestDirectoryFixture() );

// Inside the test
Path directory = dir.testDirectory();                   // /tmp/test/test-<cuid>/
Path file      = dir.testPath( "output/result.json" );  // creates parent dirs automatically
URL  fileUrl   = dir.testUrl( "output/result.json" );

// Copy test resources from classpath into the test directory
// Looks for src/test/resources/<ClassName>/ and copies its contents
dir.deployTestData( MyTest.class );
dir.deployTestData( MyTest.class, "subdir" );           // copy into a subdirectory
```

### Fixture variable

| Variable | Value |
|---|---|
| `TEST_DIRECTORY` | Absolute path of the test directory |

Use in `application.conf`:
```hocon
services.my-service.parameters.directory = ${TEST_DIRECTORY}
```

### Cleanup

```java
// Wipe and recreate without ending the fixture (e.g. between test iterations)
dir.cleanTestDirectory();
```

Directories older than 2 hours are automatically removed on each teardown pass.

---

## `KernelFixture`

Boots a full OAP `Kernel` from an `application.conf` and the module descriptors on the classpath. Use it for integration tests that need real services wired by the IoC container.

```java
@Listeners( Fixtures.class )
public class OrderServiceTest extends Fixtures {
    private final TestDirectoryFixture dir    = fixture( new TestDirectoryFixture() );
    private final KernelFixture        kernel = fixture( new KernelFixture(
        dir,
        Resources.url( OrderServiceTest.class, "application.conf" ).orElseThrow()
    ) );

    @Test
    public void test() {
        OrderService svc = kernel.service( "my-module", OrderService.class );
        assertThat( svc ).isNotNull();
    }
}
```

### Fixture variables

| Variable | Value |
|---|---|
| `TEST_HTTP_PORT` | Allocated HTTP port (usable in `application.conf`) |
| `TEST_DIRECTORY` | Absolute path of the test directory |
| `TEST_RESOURCE_PATH` | Classpath root for the test class |
| `TEST_HTTP_PREFIX` | `http://localhost:<TEST_HTTP_PORT>` |

### Key methods

| Method | Description |
|---|---|
| `service(moduleName, Class)` | Retrieve a service by module and type |
| `service(moduleName, serviceName)` | Retrieve a service by module and name |
| `service(reference)` | Retrieve a service by `<modules.mod.svc>` reference |
| `ofClass(Class)` | Get all services of a type |
| `defaultHttpPort()` | Return the allocated `TEST_HTTP_PORT` value |
| `httpUrl(path)` | Build `http://localhost:<port><path>` |
| `withConfdResources(class, resource)` | Copy a classpath directory into the confd path |
| `withLocalConfdResources(class, resource)` | Same, with `<ClassName>/` prefix |
| `withConfResource(class, resource)` | Copy a classpath file into the confd path |
| `withLocalConfResource(class, resource)` | Same, with `<ClassName>/` prefix |
| `withProperties(map)` | Add extra kernel properties |
| `withFileProperties(url)` | Load extra properties from a HOCON/JSON file |
| `addDependency(name, fixture)` | Expose another fixture's properties to the kernel |

### Constructor variants

```java
new KernelFixture( testDirectoryFixture, conf )
new KernelFixture( testDirectoryFixture, conf, confdPath )
new KernelFixture( testDirectoryFixture, conf, additionalModules )
new KernelFixture( testDirectoryFixture, conf, confdPath, additionalModules )
```

---

## `SystemTimerFixture`

Controls the Joda-Time clock for tests that depend on `DateTimeUtils.currentTimeMillis()`.

```java
SystemTimerFixture timer = fixture( new SystemTimerFixture() );

// Inside the test
Dates.setTimeFixed( 2024, 6, 1, 14, 30, 0 );  // freeze at 14:30:00 UTC
// ... run code that reads DateTimeUtils.currentTimeMillis() ...
Dates.incFixed( Dates.m( 5 ) );                // advance 5 minutes
```

The fixture resets the clock to the real system time in `after()`. Pass `false` to the constructor to skip the reset in `before()`.

---

## `JavaWebServerFixture`

Embeds a JDK `HttpServer` for testing HTTP client code. A free port is allocated automatically and exposed as the `HTTP_PORT` fixture property.

```java
JavaWebServerFixture server = fixture(
    new JavaWebServerFixture()
        .withHandler( "/api/data", exchange -> {
            byte[] body = """{"ok":true}""".getBytes();
            exchange.getResponseHeaders().set( "Content-Type", "application/json" );
            exchange.sendResponseHeaders( 200, body.length );
            exchange.getResponseBody().write( body );
            exchange.close();
        } )
);

// Inside the test
int port = server.getProperty( "HTTP_PORT" );
```

| Method | Description |
|---|---|
| `withHandler(path, HttpHandler)` | Register a handler on a context path |
| `withThreads(n)` | Thread-pool size (default 10) |
| `withBacklog(n)` | Socket backlog (default 0) |
| `withWaitUntilExchangesHaveFinishedSeconds(n)` | Shutdown delay in seconds (default 1) |

---

## `MetricsFixture`

Creates an isolated Micrometer `SimpleMeterRegistry`, adds it to the global registry before the test, and removes it after.

```java
public class ServiceTest extends Fixtures {
    private final MetricsFixture metrics = fixture( new MetricsFixture() );

    @Test
    public void countsRequests() {
        Metrics.counter( "requests" ).increment( 3 );

        metrics.assertMetric( "requests", Tags.empty() )
               .isCounter()
               .isEqualTo( 3.0 );
    }
}
```

`assertMetric(name, tags)` returns a `MetricsAssertion`. Call `.isCounter()` or `.isGauge()` to narrow to the appropriate type, then `.isEqualTo(double)` to assert the value.

---

## `MongoFixture`

Spins up an in-process MongoDB server (via `de.bwaldvogel:mongo-java-server`) on a free port. No Docker or external process required.

```java
public class StorageTest extends Fixtures {
    private final MongoFixture mongo = fixture( new MongoFixture( "mydb" ) );

    @Test
    public void insertsDocument() {
        mongo.insertDocument( StorageTest.class, "items", "item.json" );
        assertThat( mongo.client().getCollection( "items" ).countDocuments() ).isEqualTo( 1 );
    }
}
```

### Fixture variables

| Variable | Value |
|---|---|
| `PORT` | Allocated MongoDB port |
| `HOST` | `localhost` |
| `DATABASE` | Database name passed to the constructor |

Use in `application.conf`:
```hocon
services.mongo.parameters.uri = "mongodb://${HOST}:${PORT}/${DATABASE}"
```

### Key methods

| Method | Description |
|---|---|
| `createMongoClient()` | Create a `MongoClient` connected to the fixture server |
| `createMongoClient(migrationPackage, ...)` | Create a `MongoClient` with Mongock migration packages |
| `getConnectionString()` | Return the MongoDB URI for the default database |
| `getConnectionString(database)` | Return the MongoDB URI for a specific database |
| `insertDocument(contextClass, collection, resource)` | Insert one document from a classpath JSON resource |
| `initializeVersion(version)` | Set the stored schema version |
| `client()` | Return the fixture-managed `MongoClient` |
| `dropDatabase(database)` | Drop a database |

---

## `S3MockFixture`

Starts a LocalStack container with S3 enabled via Testcontainers. Requires Docker.

```java
public class CloudStorageTest extends Fixtures {
    private final S3MockFixture s3 = fixture(
        new S3MockFixture().withInitialBuckets( "my-bucket" )
    );

    @Test
    public void uploadsFile() {
        s3.uploadFile( "my-bucket", "key/data.json", "{}" , Map.of() );
        String content = s3.readFile( "my-bucket", "key/data.json",
            ContentReader.ofString(), Encoding.PLAIN );
        assertThat( content ).isEqualTo( "{}" );
    }
}
```

### Fixture variables

| Variable | Value |
|---|---|
| `HTTP_PORT` | Allocated LocalStack HTTP port |

### Key methods

| Method | Description |
|---|---|
| `withInitialBuckets(names...)` | Create these buckets on startup |
| `uploadFile(bucket, key, path)` | Upload a local file |
| `uploadFile(bucket, key, path, tags)` | Upload a local file with S3 tags |
| `uploadFile(bucket, key, content, tags)` | Upload a string body with S3 tags |
| `readFile(bucket, key, reader, encoding)` | Read an object, decode with `reader` and `encoding` |
| `readTags(bucket, key)` | Return S3 tags as `Map<String, String>` |
| `headObject(bucket, key)` | Return S3 object metadata |
| `createFolder(bucket, path)` | Create a zero-byte `application/x-directory` object |
| `deleteAll()` | Delete all objects in all buckets |
| `getFileSystemConfiguration(bucket)` | Return OAP cloud-FS config pointing at the fixture endpoint |
| `getHttpPort()` | Return the allocated HTTP port |

---

## `TemplateEngineFixture`

Instantiates an OAP `TemplateEngine` with a `/tmp/file-cache` disk cache and a 5-day TTL.

```java
public class TemplateTest extends Fixtures {
    private final TemplateEngineFixture tmpl = fixture( new TemplateEngineFixture() );

    @Test
    public void rendersTemplate() {
        var t = tmpl.templateEngine.getTemplate( ... );
        assertThat( t.render( data ).get() ).isEqualTo( "expected" );
    }
}
```

Access the engine via the public field `templateEngine`.

---

## `Ports`

Cross-process free port allocator. Uses a file lock at `/tmp/port.lock` to coordinate allocation between concurrently running test JVMs.

```java
int port = Ports.getFreePort( MyTest.class );   // range 10 000–30 000
```

`AbstractFixture.definePort(key)` calls this internally and stores the result as a fixture property.

---

## `Asserts`

Static assertion helpers for TestNG tests.

### Async assertions

```java
// Retry the assertion block until it passes or the timeout expires
Asserts.assertEventually(
    Duration.ofSeconds( 5 ),
    Duration.ofMillis( 100 ),
    () -> assertThat( queue.size() ).isGreaterThan( 0 )
);
```

### Test resource helpers

Resources are looked up as `<ClassName>/<name>` relative to the test class's package.

```java
String json = Asserts.contentOfTestResource( MyTest.class, "sample.json", Map.of( "id", "o-1" ) );
byte[] bytes = Asserts.bytesOfTestResource( MyTest.class, "sample.bin" );
Path   path  = Asserts.pathOfTestResource( MyTest.class, "sample.json" );
URL    url   = Asserts.urlOfTestResource( MyTest.class, "sample.json" );

// Unmarshal a HOCON file directly into an object
MyConfig cfg = Asserts.objectOfTestResource( MyConfig.class, MyTest.class, "config.conf" );
```

### Fluent assertions

```java
// String assertion (triggers IntelliJ diff dialog on failure)
Asserts.assertString( actual )
    .isEqualTo( expected )
    .isEqualToIgnoringCase( expected )
    .isEqualToLineSorting( expected );   // sort both before comparing

// File assertion
Asserts.assertFile( path )
    .exists()
    .hasContent( "expected text" )
    .hasContent( "expected text", IoStreams.Encoding.GZIP )
    .hasContentLineSorting( "sorted expected", IoStreams.Encoding.PLAIN )
    .hasSize( 1024L )
    .hasSameContentAs( otherPath );

// oap.util.Result assertion
Asserts.assertResult( Result.success( "value" ) ).isSuccess();
Asserts.assertResult( Result.failure( "err" ) ).isFailure();
```

---

## `JsonAsserts`

Structural JSON assertions that compare objects independently of key order.

```java
import static oap.json.testng.JsonAsserts.assertJson;

// Key order is ignored; both sides are deeply sorted before comparison
assertJson( actual )
    .isEqualTo( """{"name":"Alice","id":"1"}""" );

// Compare with a test resource file
assertJson( actual )
    .isEqualTo( MyTest.class, "expected.json" );

// Map substitutions applied to the expected string before comparison
assertJson( actual )
    .isEqualTo( expected, Map.of( "id", "42" ) );

// Canonical form comparison (round-trips through the Java type)
assertJson( actual )
    .isEqualCanonically( MyBean.class, expected );
```

`assertJson` accepts a JSON string or a `Map<String, Object>`.

---

## `CaseSuite`

Reflection-based parameterized test framework. Define an abstract master class with `@CaseProvider` methods; concrete subclasses supply case data; `CaseSuite.casesOf()` discovers all subclasses and assembles a TestNG data provider.

```java
// Abstract master class
public abstract class ConversionTest {

    @CaseProvider
    public abstract Object[][] cases();

    @Test( dataProvider = "cases" )
    public void converts( CaseContext ctx, String input, String expected ) {
        ctx.assertion( () -> assertThat( convert( input ) ).isEqualTo( expected ) );
    }

    @DataProvider
    public Object[][] cases() {
        return CaseSuite.casesOf( this, ConversionTest.class );
    }
}

// Concrete case class
public class UpperCaseConversionTest extends ConversionTest {

    @CaseProvider
    public Object[][] cases() {
        return new Object[][] {
            CaseSuite.thecase( "hello", "HELLO" ),
            CaseSuite.thecase( "world", "WORLD" )
        };
    }
}
```

`CaseContext.assertion(Runnable)` runs the block and prepends the case class name to any assertion error message, making failures easy to locate.

---

## `TestUtils`

Generates test-context-aware names by inspecting the call stack to find the current TestNG method.

```java
// Inside a @Test, @BeforeMethod, @AfterMethod, @BeforeClass, etc.
String name = TestUtils.randomName( "{test_class_name}-{test_method_name}-{rand}" );
```

| Placeholder | Replaced with |
|---|---|
| `{test_class_name}` | Simple name of the test class |
| `{test_method_name}` | Name of the `@Test` method (not allowed in `@Before`/`@After`) |
| `{rand}` | Stable 6-character random alphabetic string (same value per test execution) |

---

## `Benchmark`

Micro-benchmark harness with TeamCity integration.

```java
import static oap.benchmark.Benchmark.benchmark;

@Test
public void hashMapPutPerformance() {
    Map<String, String> map = new ConcurrentHashMap<>();

    benchmark( "put", 100_000, i -> map.put( Integer.toString( i ), "v" ) )
        .warming( 1_000 )            // warm-up iterations before timing (default 1000)
        .experiments( 5 )            // number of timed runs (default 5)
        .threads( 4 )                // run on 4 threads simultaneously
        .period( Period.seconds( 1 ) ) // rate denominator (default 1 second)
        .run();
}
```

The `code` lambda receives `(experiment, sample)` indices. Use the single-argument form `i -> ...` for simple cases.

### Output

```
benchmarking HashMapPutPerformance#put: experiment 0(4thds), 100000 samples, 45321 usec, avg time 0 usec, rate 2205022.0000 action/s
benchmarking HashMapPutPerformance#put: avg time 0 usec, avg rate 2189341.2000 action/s
```

Results are also reported to TeamCity as performance metrics when running in CI.

### `Result`

`Benchmark.run()` returns a `Result` with fields:
- `rate` — operations per `period`
- `time` — average time per operation in µs
- `experiment` — label of the last (or only) experiment
