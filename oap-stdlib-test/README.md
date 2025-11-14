# OAP Stdlib Test

Testing utilities and frameworks for OAP applications, including fixtures, benchmarking, assertions, and TestNG integration.

## Overview

OAP Stdlib Test provides comprehensive testing infrastructure for OAP applications and modules. It features:
- TestNG integration and fixtures
- Benchmarking framework with multiple runners
- Custom assertions and matchers
- JSON testing utilities
- Test containers support
- Directory and file fixtures
- HTTP server testing
- Metrics testing fixtures
- Port management utilities

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-stdlib-test</artifactId>
    <version>${oap.version}</version>
    <scope>test</scope>
</dependency>
```

## Key Features

- **TestNG Framework** - Integration with TestNG test framework
- **Fixture System** - Setup/teardown with scope control (method, class, suite)
- **Benchmarking** - Single-threaded and multi-threaded benchmarking
- **Assertions** - JSON assertions, custom matchers
- **Test Containers** - Docker container support for integration tests
- **HTTP Testing** - Built-in HTTP server for testing
- **Port Management** - Automatic port assignment to avoid conflicts
- **File Operations** - Temporary directory management
- **System Testing** - System time control and monitoring

## Key Classes

### Fixtures Framework
- `Fixtures` - Base class for test fixtures
- `AbstractFixture` - Abstract fixture with lifecycle management
- `Fixture` - Scope-based fixture provider

### Specialized Fixtures
- `TestDirectoryFixture` - Temporary directory management
- `JavaWebServerFixture` - HTTP server for testing
- `MetricsFixture` - Metrics collection and monitoring
- `SystemTimerFixture` - System time control

### Testing Utilities
- `Asserts` - Custom assertions
- `JsonAsserts` - JSON-specific assertions
- `TestUtils` - Utility functions
- `Ports` - Port management

### Benchmarking
- `Benchmark` - Main benchmarking class
- `AbstractRunner` - Base benchmark runner
- `SingleThreadRunner` - Single-threaded benchmarks
- `MultiThreadRunner` - Multi-threaded benchmarks
- `Result` - Benchmark results

## Quick Example

### Using Fixtures

```java
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

public class MyTest extends Fixtures {
    
    private TestDirectoryFixture tempDir = fixture(new TestDirectoryFixture());
    
    @Test
    public void testWithTempDirectory() {
        // tempDir.get() returns Path to temporary directory
        Path testDir = tempDir.get();
        
        // Create test files
        Files.write(testDir.resolve("test.txt"), "content".getBytes());
        
        // Assert file exists
        assertTrue(Files.exists(testDir.resolve("test.txt")));
    }
}
```

### Using Assertions

```java
import oap.testng.Asserts;
import static org.assertj.core.api.Assertions.assertThat;

@Test
public void testWithCustomAssertions() {
    String json = "{\"name\": \"John\", \"age\": 30}";
    
    // Parse and assert
    var obj = Json.parse(json);
    assertThat(obj).hasFieldOrProperty("name");
    
    // Custom field assertion
    Asserts.assertEquals(obj.get("name"), "John", 
        "Name should be John");
}
```

### Using JSON Assertions

```java
import oap.json.testng.JsonAsserts;
import static oap.json.testng.JsonAsserts.assertJson;

@Test
public void testJsonResponse() {
    String response = api.getUser(123);
    
    assertJson(response)
        .has("$.id", 123)
        .has("$.name", "Alice")
        .has("$.roles[0]", "admin")
        .isNotEmpty();
}
```

### Benchmarking

```java
import oap.benchmark.Benchmark;
import oap.benchmark.MultiThreadRunner;

@Test
public void benchmarkStringConcat() {
    Benchmark benchmark = new Benchmark("String Concat")
        .withWarmup(1000)
        .withIterations(100000)
        .withRunner(new MultiThreadRunner(4)); // 4 threads
    
    Result result = benchmark.run(() -> {
        String s = "a" + "b" + "c";
        return s.length();
    });
    
    System.out.println(result.getAverage() + "ns");
    System.out.println(result.getMedian() + "ns");
    System.out.println(result.getPercentile(95) + "ns");
}
```

### HTTP Server Testing

```java
import oap.testng.http.JavaWebServerFixture;

public class ApiTest extends Fixtures {
    
    private JavaWebServerFixture server = fixture(
        new JavaWebServerFixture()
            .get("/api/users/:id", (ctx) -> {
                int id = Integer.parseInt(ctx.path.get("id"));
                ctx.response = Json.format(Map.of("id", id, "name", "User" + id));
                ctx.status = 200;
            })
    );
    
    @Test
    public void testApiEndpoint() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/api/users/123";
        String response = HttpClient.get(url);
        
        // Assert response
        var data = Json.parse(response);
        assertThat(data.get("id")).isEqualTo(123);
    }
}
```

## Fixture Lifecycle

Fixtures support three scopes:

### Method Scope (Default)
```java
public class MyTest extends Fixtures {
    private TestDirectoryFixture tempDir = fixture(new TestDirectoryFixture());
    
    @Test
    public void test1() {
        // Fresh directory for each test method
    }
    
    @Test
    public void test2() {
        // Fresh directory for each test method
    }
}
```

### Class Scope
```java
public class MyTest extends Fixtures {
    private TestDirectoryFixture tempDir = classFixture(new TestDirectoryFixture());
    
    @Test
    public void test1() {
        // Same directory for all methods in class
    }
    
    @Test
    public void test2() {
        // Same directory for all methods in class
    }
}
```

### Suite Scope
```java
public class MyTest extends Fixtures {
    private TestDirectoryFixture tempDir = suiteFixture(new TestDirectoryFixture());
    
    @Test
    public void test1() {
        // Same directory for entire test suite
    }
}
```

## Assertions

### Basic Assertions
```java
Asserts.assertEquals(actual, expected);
Asserts.assertNotNull(value);
Asserts.assertTrue(condition);
Asserts.fail("message");
```

### JSON Assertions
```java
JsonAsserts.assertJson(json)
    .has("$.field", value)
    .hasPath("$.nested.path")
    .isNotEmpty()
    .hasSize(5);
```

### Collection Assertions
```java
assertThat(list)
    .isNotEmpty()
    .hasSize(3)
    .contains("item1", "item2")
    .doesNotContain("item4");
```

## Benchmarking Framework

### Single-Threaded Benchmark
```java
Result result = new Benchmark("Operation")
    .withWarmup(1000)
    .withIterations(10000)
    .withRunner(new SingleThreadRunner())
    .run(() -> doWork());
```

### Multi-Threaded Benchmark
```java
Result result = new Benchmark("Operation")
    .withWarmup(1000)
    .withIterations(10000)
    .withThreads(8)
    .withRunner(new MultiThreadRunner(8))
    .run(() -> doWork());
```

### Benchmark Results
```java
System.out.println(result.getAverage());      // Average time
System.out.println(result.getMedian());       // Median time
System.out.println(result.getMin());          // Minimum time
System.out.println(result.getMax());          // Maximum time
System.out.println(result.getPercentile(95)); // 95th percentile
System.out.println(result.getStdDev());       // Standard deviation
```

## Configuration

Include in `pom.xml`:

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-stdlib-test</artifactId>
    <version>${oap.version}</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

Configure TestNG in `testng.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-current.dtd">
<suite name="Suite">
    <test name="Unit Tests">
        <classes>
            <class name="com.example.MyTest"/>
        </classes>
    </test>
</suite>
```

## Advanced Usage

### Custom Fixtures

```java
public class DatabaseFixture extends AbstractFixture<Database> {
    @Override
    protected void start() throws Exception {
        object = new Database("test");
        object.start();
    }
    
    @Override
    protected void stop() throws Exception {
        object.stop();
    }
}

// Usage
public class MyTest extends Fixtures {
    private DatabaseFixture db = fixture(new DatabaseFixture());
    
    @Test
    public void testWithDatabase() {
        db.get().query("SELECT * FROM users");
    }
}
```

### Nested Fixtures

```java
public class ServiceFixture extends AbstractFixture<Service> {
    private final DatabaseFixture database;
    
    public ServiceFixture(DatabaseFixture database) {
        this.database = database;
        addDependency(database);
    }
    
    @Override
    protected void start() throws Exception {
        object = new Service(database.get());
        object.start();
    }
}
```

### Port Management

```java
import oap.testng.Ports;

@Test
public void testWithMultiplePorts() {
    int port1 = Ports.getFreePort();
    int port2 = Ports.getFreePort();
    
    server1.start(port1);
    server2.start(port2);
}
```

## Test Containers Integration

```java
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PostgresTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("testdb")
        .withUsername("user")
        .withPassword("password");
    
    @Test
    public void testDatabase() {
        String url = postgres.getJdbcUrl();
        // Use database in test
    }
}
```

## Best Practices

1. **Use Fixtures** - Leverage fixtures for common setup/teardown
2. **Scope Appropriately** - Choose fixture scope (method/class/suite) carefully
3. **Clean Resources** - Always clean up temporary files and ports
4. **Bench Realistically** - Warm up before benchmarking
5. **Assert Meaningfully** - Use specific assertions, not just assertTrue
6. **Test Isolation** - Ensure tests don't depend on each other
7. **Performance** - Keep tests fast with appropriate iteration counts

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-json` - JSON processing
- `oap-io` - I/O utilities

## Performance Tips

### Reduce Benchmark Variance
```java
new Benchmark("op")
    .withWarmup(10000)      // More warmup
    .withIterations(100000) // More iterations
    .withRunner(new SingleThreadRunner()) // Single thread
    .run(() -> doWork());
```

### Avoid GC During Benchmarks
```java
System.gc();
new Benchmark("op")
    .withWarmup(1000)
    .run(() -> doWork());
```

## Troubleshooting

### Port Already in Use
```
[ERROR] Port 8080 already in use
```
Solution: Use `Ports.getFreePort()` instead of hardcoded ports

### Fixture Not Initialized
```
[ERROR] NullPointerException on fixture.get()
```
Solution: Ensure fixture is properly declared with `fixture()` method

### Test Takes Too Long
```
[INFO] Test completed in 5 minutes
```
Solution: Reduce benchmark iterations or use fewer threads

## Testing

See `oap-stdlib-test/src/test/` for comprehensive examples.
