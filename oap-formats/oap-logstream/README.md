# OAP Logstream

Distributed log streaming system for the Open Application Platform (OAP) with support for various data models, network protocols, and storage backends.

## Overview

The `oap-logstream` module provides a flexible, extensible framework for collecting, transforming, and streaming log data across distributed systems. It supports template-based logging, map-based and object-based data models, network streaming, and multiple backend storage options (memory, disk, Parquet).

## Key Features

1. **Multiple Data Models**
   - Template-based logging with dynamic field mapping
   - Map-based data model for flexible schemas
   - Object-based model for type-safe logging
   - Binary object serialization

2. **Data Transformation**
   - TSV format conversion
   - Dynamic field rendering
   - Type conversion and mapping
   - Timestamp handling

3. **Network Protocol**
   - Distributed logging via socket connections
   - Configurable network buffers
   - Server and client implementations
   - Binary protocol support

4. **Storage Backends**
   - Memory backend for testing
   - Disk-based persistence
   - Parquet format support
   - Network distribution

5. **Logger Configuration**
   - Template-based log IDs and file paths
   - Dynamic property substitution
   - Sharding support
   - Availability reporting

## Module Structure

```
oap-logstream/
├── pom.xml                           # Parent POM with dependencies
├── oap-logstream/                    # Core logging framework
│   ├── pom.xml
│   ├── src/main/java/oap/logstream/
│   │   ├── Logger.java               # Main logger interface
│   │   ├── LogId.java                # Log identifier and metadata
│   │   ├── LogIdTemplate.java        # Template-based log ID generation
│   │   ├── Timestamp.java            # Timestamp handling
│   │   ├── TemplateLogger.java       # Template-based logger
│   │   ├── AbstractLoggerBackend.java # Backend interface
│   │   ├── MemoryLoggerBackend.java  # In-memory backend
│   │   ├── LogStreamProtocol.java    # Network protocol definition
│   │   ├── disk/                     # Disk backend implementation
│   │   ├── formats/                  # Format handlers
│   │   ├── net/                      # Network utilities
│   │   ├── sharding/                 # Sharding support
│   │   └── tsv/                      # TSV input stream
│   └── src/test/java/oap/logstream/
│
├── oap-logstream-data/               # Data model handling
│   ├── pom.xml
│   ├── src/main/java/oap/logstream/data/
│   │   ├── DataModel.java            # Dictionary-based data model
│   │   ├── AbstractLogModel.java     # Base log model
│   │   ├── LogRenderer.java          # Log rendering
│   │   ├── TsvDataTransformer.java   # TSV transformation
│   │   ├── map/                      # Map-based logging
│   │   │   ├── AbstractMapLogger.java
│   │   │   ├── MapLogModel.java
│   │   │   ├── MapLogRenderer.java
│   │   │   └── MapLoggerTest.java
│   │   └── dynamic/                  # Dynamic map logger
│   │       └── DynamicMapLogger.java
│   └── src/test/java/oap/logstream/data/
│
├── oap-logstream-data-object/        # Object-based logging
│   ├── pom.xml
│   ├── src/main/java/oap/logstream/data/object/
│   │   ├── BinaryObjectLogger.java
│   │   └── ... (object logging implementation)
│   └── src/test/java/
│
├── oap-logstream-net-server/         # Network server
│   ├── pom.xml
│   ├── src/main/java/oap/logstream/net/server/
│   │   └── SocketLoggerServer.java
│   └── src/test/java/
│
├── oap-logstream-net-client/         # Network client
│   ├── pom.xml
│   ├── src/main/java/oap/logstream/net/client/
│   │   └── ... (client implementation)
│   └── src/test/java/
│
└── oap-logstream-test/               # Testing utilities
    ├── pom.xml
    ├── src/main/java/oap/testng/
    │   └── TsvAssertionTest.java
    └── src/test/java/oap/logstream/
        ├── LogIdTest.java
        ├── TimestampTest.java
        ├── LoggerJsonTest.java
        └── tsv/TsvTest.java
```

## Quick Start

### Basic Logging with Memory Backend

```java
import oap.logstream.Logger;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.MemoryLoggerBackend;
import oap.template.Types;

// Create a memory backend (useful for testing)
AbstractLoggerBackend backend = new MemoryLoggerBackend();
Logger logger = new Logger(backend);

// Log data
String filePrefix = "mylog";
Map<String, String> properties = Map.of("environment", "production");
String logType = "APPLICATION";
String[] headers = new String[]{"TIMESTAMP", "MESSAGE", "LEVEL"};
byte[][] types = new byte[][]{
    {Types.RAW.id},
    {Types.RAW.id},
    {Types.RAW.id}
};
byte[] row = "2024-01-01 12:00:00\tApplication started\tINFO".getBytes();

logger.log(filePrefix, properties, logType, headers, types, row);

// Check availability
if (logger.isLoggingAvailable()) {
    System.out.println("Logging is available");
}
```

### Map-Based Logging

```java
import oap.logstream.data.map.AbstractMapLogger;
import oap.logstream.data.DataModel;
import oap.logstream.AbstractLoggerBackend;
import oap.dictionary.DictionaryRoot;

// Define data model
DictionaryRoot modelDefinition = loadModel("datamodel.conf");
DataModel dataModel = new DataModel(modelDefinition);

// Create a custom map logger
class EventLogger extends AbstractMapLogger {
    public EventLogger(AbstractLoggerBackend backend, DictionaryRoot model) {
        super(backend, model, "EVENT", "LOG", "EVENT");
    }

    @Override
    public String prefix(Map<String, Object> data) {
        return "/event/${eventType}";
    }

    @Override
    public Map<String, String> substitutions(Map<String, Object> data) {
        return Map.of("eventType", String.valueOf(data.get("type")));
    }
}

// Use the logger
AbstractLoggerBackend backend = new MemoryLoggerBackend();
EventLogger eventLogger = new EventLogger(backend, modelDefinition);

Map<String, Object> event = Map.of(
    "type", "user_login",
    "userId", "user123",
    "timestamp", System.currentTimeMillis()
);
eventLogger.log(event);
```

### Template-Based Logging

```java
import oap.logstream.TemplateLogger;
import oap.logstream.LogIdTemplate;

// Create template-based log ID
LogIdTemplate logIdTemplate = new LogIdTemplate(
    "/logs/${service}/${date}/app.log",
    "SERVICE",
    "LOG_TYPE",
    "service"
);

// Create template logger
TemplateLogger templateLogger = new TemplateLogger(backend, logIdTemplate);

// Log structured data
Map<String, String> properties = Map.of(
    "service", "auth-service",
    "date", "2024-01-01"
);
templateLogger.log(prefix, properties, "APPLICATION", headers, types, row);
```

### Timestamp Management

```java
import oap.logstream.Timestamp;

// Create timestamp with specific resolution
Timestamp ts = new Timestamp(1704067200000L);  // milliseconds

// Format as string
String formatted = ts.toString();

// Access components
long seconds = ts.seconds();
long nanos = ts.nanos();
```

### LogId and Identification

```java
import oap.logstream.LogId;

// Create a log identifier
LogId logId = new LogId(
    "/logs/app/data.log",    // file path
    "APPLICATION",           // log type
    "hostname-001",          // hostname
    Map.of(                  // properties
        "service", "auth",
        "region", "us-west"
    ),
    new String[]{"TIMESTAMP\tMESSAGE"},  // headers
    new byte[][]{{Types.RAW.id}}         // field types
);

// Use for grouping and identification
String filePrefix = logId.prefix();
Map<String, String> props = logId.properties();
```

## Data Models

### Dictionary-Based Data Model

```java
// Load from configuration file
DataModel model = new DataModel(
    Paths.get("src/test/resources/datamodel.conf")
);

// Or from classpath resource
DataModel model = new DataModel(
    Resources.getResource(MyClass.class, "datamodel.json")
);

// Or from URL
DataModel model = new DataModel(
    new URL("file:///path/to/model.conf")
);
```

### Example datamodel.conf Structure

```
EVENTS {
  id = 1
  fields {
    TIMESTAMP { type = "string" }
    USER_ID { type = "integer" }
    EVENT_TYPE { type = "string" }
    EVENT_DATA { type = "string" }
  }
}
```

## Testing

### Using Memory Backend

```java
import oap.logstream.MemoryLoggerBackend;

@Test
public void testLogging() {
    MemoryLoggerBackend backend = new MemoryLoggerBackend();
    Logger logger = new Logger(backend);

    // Perform logging
    logger.log(prefix, props, type, headers, types, row);

    // Verify logs
    Map<LogId, String> logs = backend.logs();
    assertThat(logs).hasSize(1);
    assertThat(logs.values()).contains("2024-01-01 12:00:00\tData\n");
}
```

### TSV Assertion

```java
import oap.testng.TsvAssertionTest;
import oap.logstream.data.TsvDataTransformer;

@Test
public void testTsvOutput() {
    TsvDataTransformer transformer = new TsvDataTransformer(model);
    String tsvOutput = transformer.transform(data);

    TsvAssertionTest assertion = new TsvAssertionTest(tsvOutput);
    assertion.assertColumnCount(5);
    assertion.assertRowCount(10);
}
```

## Advanced Features

### Sharding Support

```java
import oap.logstream.sharding.Sharding;

// Configure sharding for distributed logging
Sharding sharding = new Sharding()
    .addShard("shard-1", "host1:9000")
    .addShard("shard-2", "host2:9000")
    .addShard("shard-3", "host3:9000");

// Route logs based on shard key
String shardKey = "userId:" + userId;
String shard = sharding.getShard(shardKey);
```

### Network Streaming

```java
import oap.logstream.net.server.SocketLoggerServer;
import oap.logstream.net.Buffer;

// Server configuration
SocketLoggerServer server = new SocketLoggerServer(
    9000,                    // port
    backend,                 // backend
    new BufferConfigurationMap()
);

// Client sends logs to server
// Logs are aggregated and stored via backend
```

### Format Handlers

```java
import oap.logstream.formats.Format;

// Support for different formats
Format tsvFormat = new TsvFormat();
Format parquetFormat = new ParquetFormat();

// Format conversion and serialization
byte[] serialized = tsvFormat.serialize(data);
Object deserialized = tsvFormat.deserialize(serialized);
```

### Availability Reporting

```java
import oap.logstream.AvailabilityReport;

// Get logging availability status
AvailabilityReport report = logger.availabilityReport();

if (!report.isAvailable()) {
    System.out.println("Status: " + report.status());
    System.out.println("Reason: " + report.reason());
}
```

## Testing Examples

### From MapLoggerTest.java

```java
@Test
public void mapLog() {
    Dates.setTimeFixed(2021, 1, 1, 1);
    MemoryLoggerBackend backend = new MemoryLoggerBackend();
    
    AbstractMapLogger logger = new EventMapLogger(
        backend,
        loadModel("datamodel.conf")
    );
    
    logger.log(Map.of(
        "name", "event",
        "value1", "value1",
        "value2", 222,
        "value3", 333
    ));
    
    assertThat(backend.logs()).containsEntry(
        new LogId("/EVENT/event", "EVENT", HOSTNAME,
            Map.of("NAME", "event"),
            new String[]{"TIMESTAMP\tNAME\tVALUE1\tVALUE2\tVALUE3"},
            new byte[][]{{Types.RAW.id}}
        ),
        "2021-01-01 01:00:00\tevent\tvalue1\t222\t333\n"
    );
}
```

### From LogIdTest.java

```java
@Test
public void logIdParsing() {
    LogId id = new LogId("/logs/app.log", "APP", "host", 
        Map.of("service", "auth"), headers, types);
    
    assertThat(id.prefix()).isEqualTo("/logs/app.log");
    assertThat(id.logType()).isEqualTo("APP");
    assertThat(id.hostname()).isEqualTo("host");
    assertThat(id.properties()).contains(entry("service", "auth"));
}
```

## Configuration

### LogStream Protocol

```java
// Protocol version configuration
LogStreamProtocol.ProtocolVersion version = 
    LogStreamProtocol.CURRENT_PROTOCOL_VERSION;

// Create logger with specific version
Logger logger = new Logger(backend, version);
```

### Buffer Configuration

```java
import oap.logstream.net.BufferConfigurationMap;

BufferConfigurationMap bufferConfig = new BufferConfigurationMap()
    .put("default", new Buffer(8192))
    .put("large", new Buffer(65536));
```

## Performance Considerations

1. **Batch Operations**: Collect multiple log entries and flush in batches
2. **Backend Selection**: Use memory backend for testing, disk for production
3. **Compression**: Parquet format provides compression for large logs
4. **Sharding**: Distribute logs across multiple shards for scalability
5. **Timestamps**: Timestamp creation is optimized for high throughput

## Error Handling

```java
import oap.logstream.LoggerException;
import oap.logstream.BackendLoggerNotAvailableException;

try {
    logger.log(prefix, props, type, headers, types, row);
} catch (BackendLoggerNotAvailableException e) {
    System.err.println("Logger backend unavailable: " + e.getMessage());
} catch (BufferOverflowException e) {
    System.err.println("Log buffer overflow");
} catch (LoggerException e) {
    System.err.println("Logging error: " + e.getMessage());
}
```

## Integration with Other Modules

### With oap-template

Template expressions in log IDs:

```java
LogIdTemplate template = new LogIdTemplate(
    "/logs/${service}/${year}-${month}-${day}/app.log",
    "SERVICE",
    "LOG"
);
```

### With oap-tsv

TSV data transformation:

```java
TsvDataTransformer transformer = new TsvDataTransformer(model);
String tsvOutput = transformer.transform(logData);
```

### With oap-json

JSON schema validation for log data:

```java
JsonSchema schema = JsonSchema.schemaFromString(logSchema);
NodeResponse validation = schema.validate(jsonLogData);
```

## Building

```bash
mvn clean install
```

Build only oap-logstream:

```bash
mvn -pl oap-formats/oap-logstream clean install
```

Build specific sub-module:

```bash
mvn -pl oap-formats/oap-logstream/oap-logstream clean install
mvn -pl oap-formats/oap-logstream/oap-logstream-data clean install
```

## Testing

```bash
mvn test
```

Run specific test class:

```bash
mvn -pl oap-formats/oap-logstream test -Dtest=MapLoggerTest
```

## Related Classes

- `Logger` - Main logging interface
- `AbstractLoggerBackend` - Backend abstraction
- `MemoryLoggerBackend` - In-memory implementation
- `LogId` - Log identification
- `Timestamp` - Time management
- `DataModel` - Data structure definition
- `AbstractMapLogger` - Map-based logging
- `SocketLoggerServer` - Network server

## Troubleshooting

### Logger Not Available

```java
AvailabilityReport report = logger.availabilityReport();
if (!report.isAvailable()) {
    // Check backend status
    System.out.println(report.reason());
}
```

### Buffer Overflow

Increase buffer size or process logs more frequently:

```java
Buffer largeBuffer = new Buffer(1024 * 1024);  // 1MB
```

### Missing Properties

Ensure all required properties are provided:

```java
// Properties must match template placeholders
Map<String, String> properties = Map.of(
    "service", "auth-service",
    "date", "2024-01-01"
);
```

## License

MIT License - See LICENSE file for details

## Further Reading

- [OAP Framework Documentation](https://github.com/oaplatform/oap)
- [Related oap-template Module](../oap-template/README.md)
- [Related oap-tsv Module](../oap-tsv/README.md)
- [Related oap-json Module](../oap-json/README.md)
