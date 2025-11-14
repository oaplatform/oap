# OAP Formats

The `oap-formats` module provides a comprehensive suite of data format handlers and transformation tools for the Open Application Platform (OAP). It includes JSON schema validation, template processing, TSV file handling, and log streaming capabilities.

## Overview

The formats module is composed of four main sub-modules:

### 1. [oap-json](./oap-json) - JSON Schema Validation
Extended JSON support with powerful schema validation, transformation, and diff capabilities.

**Key Features:**
- Schema-based JSON validation
- Type validators for primitives, arrays, objects, and dictionaries
- Template support in schemas
- JSON path operations
- JSON diff functionality

**Main Components:**
- `JsonSchema` - Core schema validator
- Type-specific validators (String, Number, Boolean, Array, Object, Dictionary)
- `JsonPath` - Navigate and query JSON structures
- `JsonDiff` - Compare JSON documents

### 2. [oap-template](./oap-template) - Template Engine
ANTLR-based dynamic template engine for generating Java objects and transforming data.

**Key Features:**
- Dynamic Java object generation from templates
- Expression evaluation
- Field access and method invocation
- Optional/nullable handling
- Built-in functions (urlencode, toUpperCase, etc.)
- Binary and string output formats

**Main Components:**
- `TemplateEngine` - Core template processor
- `Template` classes for different data models
- Template accumulators for different output types
- ANTLR-generated grammar classes

### 3. [oap-tsv](./oap-tsv) - TSV File Handling
Tab-separated values processing with strict parsing rules for high performance.

**Key Features:**
- Efficient TSV parsing and serialization
- Multiple delimiter support (tab, comma, semicolon)
- Streaming capability
- Type mapping configuration
- Array handling with proper escaping

**Main Components:**
- `Tsv` - Main API for TSV operations
- `TsvStream` - Stream-based processing
- `Tokenizer` - Fast tokenization
- `Printer` - TSV output generation
- `TsvInputStream` - Reading TSV files
- `Mapper` - Type mapping configuration

### 4. [oap-logstream](./oap-logstream) - Log Streaming
Distributed log streaming system with support for various data models and network protocols.

**Key Features:**
- Template-based logging
- Map-based and object-based data logging
- Network server/client for distributed logging
- TSV data transformation
- Dynamic logger configuration
- Parquet format support
- Memory, disk, and network backends

**Sub-modules:**
- `oap-logstream` - Core logging framework
- `oap-logstream-data` - Data model handling
- `oap-logstream-data-object` - Object-based logging
- `oap-logstream-net-server` - Network server implementation
- `oap-logstream-net-client` - Network client implementation
- `oap-logstream-test` - Testing utilities

## Module Dependencies

```
oap-formats (parent)
├── oap-json
│   └── oap-json-schema
├── oap-template
├── oap-tsv
│   ├── oap-tsv
│   └── oap-tsv-test
└── oap-logstream
    ├── oap-logstream
    ├── oap-logstream-data
    ├── oap-logstream-data-object
    ├── oap-logstream-net-server
    ├── oap-logstream-net-client
    └── oap-logstream-test
```

## Usage Examples

### JSON Schema Validation

```java
// Create a schema from JSON definition
JsonSchema schema = JsonSchema.schemaFromString(
    "{type: object, properties: {name: {type: string, required: true}}}"
);

// Validate data
NodeResponse response = schema.validate("{\"name\": \"test\"}");
if (response.hasErrors()) {
    response.errors().forEach(e -> System.out.println(e));
}
```

### Template Processing

```java
TemplateEngine engine = new TemplateEngine();
Template<Map<String, String>> template = engine.getTemplate(
    "mytemplate",
    new TypeRef<Map<String, String>>() {},
    "Hello {{name}}!",
    TemplateAccumulators.STRING,
    null
);

Map<String, String> data = Map.of("name", "World");
String result = template.render(data).get(); // "Hello World!"
```

### TSV Processing

```java
// Parse TSV data
String tsvData = "id\tname\tvalue\n1\titem1\t100\n2\titem2\t200";
List<List<String>> rows = new TsvStream(new StringReader(tsvData))
    .collect(Collectors.toList());

// Generate TSV
Printer printer = Printer.values(new String[]{"id", "name"});
printer.accept("1", "item1");
String output = printer.get(); // "id\tname\n1\titem1"
```

### Log Streaming

```java
// Create a logger with memory backend
AbstractLoggerBackend backend = new MemoryLoggerBackend();
Logger logger = new Logger(backend);

// Log data
Map<String, String> properties = Map.of();
String[] headers = new String[]{"TIMESTAMP", "MESSAGE"};
byte[][] types = new byte[][]{{Types.RAW.id}};
byte[] row = "2021-01-01 00:00:00\tHello".getBytes();

logger.log("mylog", properties, "LOG", headers, types, row);

// Check if logging is available
if (logger.isLoggingAvailable()) {
    System.out.println("Logging available");
}
```

## Building

All modules follow Maven-based build system:

```bash
mvn clean install
```

Build a specific module:

```bash
mvn -pl oap-formats/oap-json clean install
mvn -pl oap-formats/oap-template clean install
mvn -pl oap-formats/oap-tsv clean install
mvn -pl oap-formats/oap-logstream clean install
```

## Testing

Run all tests:

```bash
mvn test
```

Run module-specific tests:

```bash
mvn -pl oap-formats/oap-json test
mvn -pl oap-formats/oap-template test
mvn -pl oap-formats/oap-tsv test
mvn -pl oap-formats/oap-logstream test
```

## Architecture Patterns

### JSON Schema
- **Validator Pattern**: Type-specific validators implement `AbstractJsonSchemaValidator`
- **AST Pattern**: Schema definitions are parsed into Abstract Syntax Trees
- **Storage Pattern**: Schemas can be loaded from resources via `SchemaStorage`

### Template Engine
- **ANTLR Grammar**: Uses ANTLR4 for parsing template syntax
- **Visitor Pattern**: AST nodes implement rendering logic
- **Accumulator Pattern**: Different output types use specialized accumulators

### TSV Processing
- **Tokenizer Pattern**: Fast character-by-character parsing
- **Stream Pattern**: Lazy evaluation with streaming support
- **Configuration Pattern**: Flexible mapper configuration for type mapping

### Log Streaming
- **Backend Pattern**: Pluggable logging backends (memory, disk, network)
- **Data Model Pattern**: Dictionary-based configuration of log structures
- **Network Pattern**: Client-server architecture for distributed logging

## License

All modules are licensed under the MIT License. See individual modules for license details.

## Related Documentation

- [oap-json README](./oap-json/README.md)
- [oap-template README](./oap-template/README.md)
- [oap-tsv README](./oap-tsv/README.md)
- [oap-logstream README](./oap-logstream/README.md)
