# oap-stdlib-test

Test utilities for the OAP platform. Provides the fixture lifecycle framework, isolated test directories, free port allocation, assertion helpers, and a micro-benchmark harness.

Depends on: `oap-stdlib`

## Fixture lifecycle

Fixtures are lifecycle-managed objects that set up and tear down resources around TestNG tests. Three scopes are supported:

| Scope | Lifetime |
|---|---|
| `METHOD` (default) | Around each test method |
| `CLASS` | Around the test class (once per class) |
| `SUITE` | Around the entire test run (once per JVM) |

---

## `AbstractFixture<Self>`

Base class for all fixtures. Subclass it to build custom fixtures.

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
| `define(key, value)` | Store a named property (available as a template variable in `application.conf`) |
| `definePort(key)` | Allocate a free TCP port, store it, and return it |
| `getProperty(key)` | Retrieve a stored property |
| `withScope(Scope)` | Change the fixture scope |
| `addChild(fixture)` | Nest another fixture; its lifecycle follows the parent |
| `defineLocalClasspath(key, class, resource)` | Define a `classpath(...)` string pointing to a test resource |
| `definePath(key, path)` | Define a `path(...)` string |

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
// In any test class — shared across all test classes in the run
private static final MyFixture shared = Fixtures.suiteFixture( new MyFixture() );
```

---

## `TestDirectoryFixture`

Provides an isolated, temporary directory under `/tmp/test/`. The directory is created before the test and deleted after.

```java
TestDirectoryFixture dir = fixture( new TestDirectoryFixture() );

// Inside the test
Path directory = dir.testDirectory();                  // /tmp/test/test-<cuid>/
Path file      = dir.testPath( "output/result.json" ); // creates parent dirs
URL  fileUrl   = dir.testUrl( "output/result.json" );

// Copy test resources from classpath into the test directory
// Looks for src/test/resources/<ClassName>/ and copies its contents
dir.deployTestData( MyTest.class );
dir.deployTestData( MyTest.class, "subdir" );          // copy into a subdirectory
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

## `Ports`

Cross-process free port allocator. Uses a file lock (`/tmp/port.lock`) to coordinate allocation between concurrently running test JVMs.

```java
int port = Ports.getFreePort( MyTest.class );   // range 10000–30000
```

`AbstractFixture.definePort(key)` calls this internally and stores the result as a fixture property.

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

The fixture resets the clock to the real system time in `after()` (and optionally in `before()` when constructed with `new SystemTimerFixture(true)`, the default).

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
// Read as String (with optional variable substitution)
String json = Asserts.contentOfTestResource( MyTest.class, "sample.json" );
String json = Asserts.contentOfTestResource( MyTest.class, "sample.json",
    Map.of( "id", "o-1" ) );

// Read as bytes
byte[] bytes = Asserts.bytesOfTestResource( MyTest.class, "sample.bin" );

// Get as Path or URL
Path path = Asserts.pathOfTestResource( MyTest.class, "sample.json" );
URL  url  = Asserts.urlOfTestResource( MyTest.class, "sample.json" );

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
```

---

## `Benchmark`

Micro-benchmark harness with TeamCity integration.

```java
import static oap.benchmark.Benchmark.benchmark;

@Test
public void hashMapPutPerformance() {
    Map<String, String> map = new ConcurrentHashMap<>();

    benchmark( "put", 100_000, i -> map.put( Integer.toString( i ), "v" ) )
        .warming( 1_000 )           // warm-up iterations before timing (default 1000)
        .experiments( 5 )           // number of timed runs (default 5)
        .threads( 4 )               // run on 4 threads simultaneously
        .period( Period.seconds(1) ) // rate denominator (default 1 second)
        .run();
}
```

The `code` lambda receives `(experiment, sample)` indices. Use the single-argument form `(sample) -> ...` for simple cases.

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
