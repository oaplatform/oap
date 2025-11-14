# OAP Standard Library (oap-stdlib)

## Overview

The OAP Standard Library provides comprehensive utility classes and core functionality for building high-performance Java applications. It serves as the foundation for all OAP modules, offering utilities for I/O, JSON processing, concurrency, reflection, networking, and more.

This module is designed for:
- **Performance**: Optimized implementations for common operations
- **Reliability**: Extensive error handling and retry logic
- **Flexibility**: Support for multiple formats, protocols, and patterns
- **Developer productivity**: Rich API for common tasks

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-stdlib</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Dependencies

**Core libraries:**
- Apache Commons (Lang3, Text, IO, Compress, Codec)
- Jackson (JSON processing)
- Joda-Time (date/time operations)
- SLF4J & Logback (logging)
- Micrometer (metrics)

**Additional:**
- Guava (collections and utilities)
- FastUtil (high-performance collections)
- LZ4, Zstd, BZip2 (compression)

## Package Overview

### Core Utilities

#### `oap.util` - General Utilities
Collection operations, data structures, and common utilities.

**Key classes:**
- `Collections` - Collection utility methods (count, find, predicates)
- `Lists`, `Maps`, `Sets` - Typed utility classes for collections
- `Strings` - String manipulation and utilities
- `BloomFilter` - Probabilistic set membership testing
- `Hash` - Cryptographic and general hashing
- `Result<T, E>` - Functional error handling (similar to Either/Result types)

**Example:**
```java
import oap.util.Lists;
import oap.util.Result;

// List utilities
var evens = Lists.filter(numbers, n -> n % 2 == 0);
var names = Lists.map(users, User::getName);

// Result type for error handling
Result<User, String> result = findUser(id);
result.ifSuccess(user -> log.info("Found: {}", user.getName()));
result.ifFailure(error -> log.error("Error: {}", error));
```

#### `oap.json` - JSON Processing
Jackson-based JSON serialization with custom modules and extensions.

**Key classes:**
- `Binder` - Central JSON marshaling utility
- `JsonPatch` - JSON patch operations (RFC 6902)
- `TypeIdFactory` - Custom type ID handling for polymorphic serialization

**Example:**
```java
import oap.json.Binder;

// Serialize to JSON
String json = Binder.json.marshal(myObject);

// Deserialize from JSON
MyClass obj = Binder.json.unmarshal(MyClass.class, json);

// Pretty print
String pretty = Binder.json.marshalWithDefaultPrettyPrinter(myObject);

// Custom binder with HOCON support
Binder hoconBinder = Binder.hocon;
Config config = hoconBinder.unmarshal(Config.class, hoconContent);
```

#### `oap.io` - Input/Output Operations
File operations, streams, and I/O utilities with compression support.

**Key classes:**
- `Files` - File manipulation with retry logic and hashing
- `IoStreams` - Stream handling with auto-detection of compression formats
- `FileWalker` - Efficient directory traversal with wildcards

**Example:**
```java
import oap.io.Files;
import oap.io.IoStreams;
import java.nio.file.Path;

// Read file with automatic decompression
String content = IoStreams.asString(path, IoStreams.Encoding.from(path));

// Write with automatic compression
IoStreams.write(path, IoStreams.Encoding.GZIP, outputStream -> {
    // write to stream
});

// File operations with retry
Files.writeString(path, content);
String hash = Files.md5(path);
```

**Supported compression formats:**
- GZIP (`.gz`)
- LZ4 (`.lz4`)
- BZip2 (`.bz2`)
- Zstd (`.zstd`)
- Plain (uncompressed)

#### `oap.concurrent` - Concurrency & Threading
Thread management, executors, and asynchronous operations.

**Key classes:**
- `Executors` - Thread pool factory methods
- `Stopwatch` - Performance timing
- `CircularBuffer` - Thread-safe circular buffer
- `Scheduler` - Task scheduling utilities

**Example:**
```java
import oap.concurrent.Stopwatch;
import oap.concurrent.Executors;
import java.util.concurrent.ExecutorService;

// Performance measurement
try (Stopwatch sw = new Stopwatch("operation")) {
    // perform operation
}

// Thread pools
ExecutorService executor = Executors.newFixedThreadPool(10, "worker");
executor.submit(() -> doWork());
```

### Type System & Reflection

#### `oap.reflect` - Reflection Utilities
Type introspection, coercion, and reflection helpers.

**Key classes:**
- `Reflect` - High-level reflection API
- `Reflection` - Class metadata and introspection
- `TypeRef<T>` - Generic type reference holder
- `Coercions` - Type conversion utilities

**Example:**
```java
import oap.reflect.Reflect;
import oap.reflect.Reflection;

// Get reflection information
Reflection reflection = Reflect.reflect(MyClass.class);

// Call methods
reflection.method("methodName")
    .ifPresent(m -> m.invoke(instance, arg1, arg2));

// Access fields
reflection.field("fieldName")
    .ifPresent(f -> f.set(instance, value));

// Type coercion
Object result = Coercions.basic().cast(value, targetType);
```

### Data Handling

#### `oap.compression` - Data Compression
Compression and decompression with format auto-detection.

**Key classes:**
- `Compression` - Main compression facade

**Example:**
```java
import oap.compression.Compression;

// Compress data
byte[] compressed = Compression.compress(data, Compression.GZIP);

// Decompress with auto-detection
byte[] decompressed = Compression.decompress(compressed);
```

#### `oap.dictionary` - Dictionary Management
Key-value mappings with external ID support.

**Key classes:**
- `Dictionary` - Bidirectional ID-to-string mappings
- `DictionaryParser` - Parse dictionary files

**Example:**
```java
import oap.dictionary.Dictionary;

// Use dictionary for ID mapping
int id = dictionary.getOrDefault("key", -1);
Optional<String> value = dictionary.getValue(id);
```

#### `oap.id` - Identifier Management
Type-safe identifier abstractions.

**Key classes:**
- `Identifier` - Base interface for identifiers
- `IntIdentifier`, `StringIdentifier` - Concrete implementations

### Infrastructure

#### `oap.configuration` - Configuration Management
Load and parse configuration files.

**Key classes:**
- `ConfigurationLoader` - Configuration file loader with caching

**Example:**
```java
import oap.configuration.ConfigurationLoader;

MyConfig config = ConfigurationLoader.load(
    MyConfig.class,
    "classpath:config.json"
);
```

#### `oap.alert` - Alerting & Messaging
Alert and message transport system.

**Key classes:**
- `Alert` - Alert data model
- `Messenger` - Alert delivery interface

#### `oap.trace` - Tracing & Diagnostics
Request tracing and diagnostic information.

**Key classes:**
- `Trace` - Context propagation and tracing

**Example:**
```java
import oap.trace.Trace;

// Add trace context
Trace.trace("userId", userId);
Trace.trace("operation", "processOrder");

// Access trace info
Map<String, Object> context = Trace.get();
```

### Networking

#### `oap.net` - Network Utilities
IP address handling and network operations.

**Key classes:**
- `Inet` - Internet connectivity utilities
- `IpUtils` - IP address parsing and validation
- `IpRangeTree` - Efficient IP range lookups

**Example:**
```java
import oap.net.Inet;

// Get external IP
String externalIp = Inet.getExternalIP();

// Check connectivity
boolean connected = Inet.isConnected();
```

### Time & Date

#### `oap.time` - Time Utilities
Time handling and clock abstractions using Joda-Time.

**Key classes:**
- `Time` - Time utility interface

**Example:**
```java
import oap.time.Time;
import org.joda.time.DateTime;

// Current time
DateTime now = Time.now();

// Time arithmetic
DateTime tomorrow = now.plusDays(1);
```

### Advanced Features

#### `oap.pool` - Object Pooling
High-performance resource pooling using Disruptor.

**Key classes:**
- `Pool<T>` - Generic object pool

**Example:**
```java
import oap.pool.Pool;

// Create pool
Pool<StringBuilder> pool = new Pool<>(
    StringBuilder::new,
    builder -> builder.setLength(0)
);

// Use pooled object
try (Pool.PooledObject<StringBuilder> pooled = pool.get()) {
    StringBuilder sb = pooled.get();
    sb.append("Hello");
    return sb.toString();
}
```

#### `oap.tools` - Development Tools
Runtime compilation and code generation.

**Key classes:**
- `TemplateClassCompiler` - In-memory Java compilation

**Example:**
```java
import oap.tools.TemplateClassCompiler;

// Compile Java code at runtime
String javaCode = "public class Dynamic { public String hello() { return \"Hello\"; } }";
Class<?> clazz = TemplateClassCompiler.compile("Dynamic", javaCode);
Object instance = clazz.getDeclaredConstructor().newInstance();
```

## Common Patterns

### Error Handling with Result

The `Result<T, E>` type provides functional error handling:

```java
import oap.util.Result;

public Result<User, String> findUser(String id) {
    try {
        User user = database.find(id);
        return user != null
            ? Result.success(user)
            : Result.failure("User not found");
    } catch (Exception e) {
        return Result.failure(e.getMessage());
    }
}

// Usage
findUser(id)
    .ifSuccess(user -> log.info("Found: {}", user))
    .ifFailure(error -> log.error("Error: {}", error));

// Map and transform
Result<String, String> name = findUser(id).map(User::getName);

// Get or default
User user = findUser(id).successOrElse(User.guest());
```

### Stream Processing

```java
import oap.io.IoStreams;
import java.nio.file.Path;

// Read compressed file line by line
IoStreams.lines(path, IoStreams.Encoding.GZIP)
    .filter(line -> !line.isEmpty())
    .map(String::trim)
    .forEach(System.out::println);
```

### Safe File Operations

```java
import oap.io.Files;

// Atomic write with backup
Files.writeString(path, content);

// Read with fallback
String content = Files.exists(path)
    ? Files.readString(path)
    : "default content";

// Directory operations
Files.ensureDirectory(dirPath);
Files.delete(path);
```

## Testing Support

For testing utilities, see the companion module **oap-stdlib-test**.

## Performance Considerations

1. **BloomFilter**: Use for large-scale membership testing with acceptable false positive rate
2. **CircularBuffer**: Lock-free for high-throughput scenarios
3. **Pool**: Reuse expensive objects (StringBuilder, buffers, connections)
4. **IoStreams**: Auto-selects optimal buffer sizes based on file size
5. **FastUtil collections**: Use for primitive types to avoid boxing

## Best Practices

1. **Use Result for error handling** instead of exceptions in performance-critical code
2. **Prefer IoStreams** over raw Java I/O for automatic compression handling
3. **Use Binder.json** for all JSON operations (configured optimally)
4. **Leverage Reflect API** instead of raw reflection for type safety
5. **Use Stopwatch** for performance measurement in production
6. **Pool expensive objects** like StringBuilder in tight loops

## Common Use Cases

### Reading Configuration Files

```java
import oap.json.Binder;
import oap.io.IoStreams;

// JSON config
MyConfig config = Binder.json.unmarshal(
    MyConfig.class,
    IoStreams.asString(configPath)
);

// HOCON config
MyConfig config = Binder.hocon.unmarshal(
    MyConfig.class,
    IoStreams.asString(configPath)
);
```

### Processing Large Files

```java
import oap.io.IoStreams;
import java.util.concurrent.atomic.AtomicLong;

AtomicLong count = new AtomicLong();

// Auto-detects compression from extension
IoStreams.lines(largeFile, IoStreams.Encoding.from(largeFile))
    .parallel()
    .filter(line -> line.contains("ERROR"))
    .forEach(line -> {
        count.incrementAndGet();
        log.error(line);
    });

log.info("Found {} errors", count.get());
```

### Hash Computing

```java
import oap.util.Hash;
import oap.io.Files;

// String hashing
String hash = Hash.md5(data);
String sha = Hash.sha256(data);

// File hashing
String fileHash = Files.md5(path);
```

### Collection Operations

```java
import oap.util.Lists;
import oap.util.Maps;

// Transform collections
List<String> names = Lists.map(users, User::getName);
List<User> adults = Lists.filter(users, u -> u.getAge() >= 18);

// Group by
Map<String, List<User>> byCity = Lists.groupBy(users, User::getCity);

// Map operations
Map<String, Integer> map = Maps.of(
    "one", 1,
    "two", 2,
    "three", 3
);
```

## Module Structure

```
oap-stdlib/
├── src/main/java/oap/
│   ├── util/              # Core utilities
│   ├── json/              # JSON processing
│   ├── io/                # I/O operations
│   ├── concurrent/        # Threading & concurrency
│   ├── reflect/           # Reflection utilities
│   ├── time/              # Time & date
│   ├── net/               # Networking
│   ├── compression/       # Compression
│   ├── dictionary/        # Dictionary management
│   ├── id/                # Identifier types
│   ├── configuration/     # Configuration loading
│   ├── alert/             # Alerting
│   ├── trace/             # Tracing
│   ├── pool/              # Object pooling
│   ├── tools/             # Dev tools
│   ├── lang/              # Language extensions
│   ├── system/            # System utilities
│   └── archive/           # Archive handling
└── src/main/resources/
    └── META-INF/
        └── oap-module.conf  # Module configuration
```

## See Also

- [oap-stdlib-test](../oap-stdlib-test/README.md) - Testing utilities
- [oap-application](../oap-application/README.md) - Application framework
- [oap-json](../oap-formats/oap-json/README.md) - Extended JSON support
