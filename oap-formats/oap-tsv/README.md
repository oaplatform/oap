# OAP TSV

Tab-separated values (TSV) processing with strict parsing rules and high performance for the Open Application Platform (OAP).

## Overview

The `oap-tsv` module provides efficient TSV (Tab-Separated Values) file handling with support for multiple delimiter types. Unlike CSV (Comma-Separated Values) which allows flexible delimiters and requires complex escaping rules, TSV uses only the tab character to distinguish columns, making it faster to parse and less ambiguous.

## Key Features

1. **Efficient TSV Parsing**
   - Tab character as only delimiter for strict parsing
   - Significantly faster than CSV parsing
   - Minimal escaping requirements
   - Proper handling of special characters

2. **Multiple Delimiter Support**
   - Tab (`\t`)
   - Comma (`,`)
   - Semicolon (`;`)
   - Configurable custom delimiters

3. **Streaming Support**
   - Process large files without loading entire content
   - Memory-efficient processing
   - Lazy evaluation

4. **Type Mapping**
   - Configure field types during mapping
   - Type conversion and validation
   - Handle complex data structures

5. **Array Handling**
   - Proper escaping of array elements
   - TSV wraps data with quotes when needed
   - Consistent serialization/deserialization

## Comparison: TSV vs CSV

| Aspect | TSV | CSV |
|--------|-----|-----|
| Delimiter | Tab only (`\t`) | Multiple (comma, tab, semicolon, pipe) |
| Escaping | Minimal | Complex (requires quotes, escape sequences) |
| Performance | Fast | Slower (more parsing rules) |
| Ambiguity | Low | Higher |
| Array Format | `1\t2\t3` | `"1","2","3"` |

## Module Structure

```
oap-tsv/
├── pom.xml
├── oap-tsv/                      # Main TSV library
│   ├── pom.xml
│   └── src/
│       ├── main/java/oap/tsv/
│       │   ├── Tsv.java           # Main API
│       │   ├── TsvStream.java     # Streaming processor
│       │   ├── TsvInputStream.java # Stream input
│       │   ├── TsvArray.java       # Array handling
│       │   ├── Tokenizer.java     # Fast tokenizer
│       │   ├── Printer.java       # TSV generation
│       │   └── mapper/             # Type mapping
│       │       ├── Mapper.java
│       │       └── Configuration.java
│       └── test/java/oap/tsv/
│           ├── TokenizerTest.java
│           ├── TsvStreamTest.java
│           ├── TsvArrayTest.java
│           ├── TsvInputStreamTest.java
│           └── PrinterTest.java
│
└── oap-tsv-test/                 # Testing utilities
    ├── pom.xml
    └── src/
        ├── main/java/oap/tsv/test/
        │   └── TsvAssertion.java
        └── test/java/oap/tsv/
            ├── TokenizerPerformance.java
            └── ... (other tests)
```

## Quick Start

### Basic TSV Parsing

```java
import oap.tsv.Tsv;
import oap.tsv.TsvStream;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

// TSV data
String tsvData = "id\tname\tvalue\n1\titem1\t100\n2\titem2\t200";

// Parse TSV
List<List<String>> rows = new TsvStream(new StringReader(tsvData))
    .map(Tsv.tsv::parse)
    .collect(Collectors.toList());

// Access data
for (List<String> row : rows) {
    String id = row.get(0);
    String name = row.get(1);
    String value = row.get(2);
    System.out.println(id + " -> " + name + " (" + value + ")");
}
```

### Writing TSV Data

```java
import oap.tsv.Printer;

// Create TSV printer with headers
Printer printer = Printer.values(new String[]{"id", "name", "value"});

// Add rows
printer.accept("1", "item1", "100");
printer.accept("2", "item2", "200");

// Get TSV output
String tsvOutput = printer.get();
System.out.println(tsvOutput);
// Output:
// id  name   value
// 1   item1  100
// 2   item2  200
```

### Reading TSV Files

```java
import oap.tsv.TsvInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// Read TSV file
Path tsvFile = Path.of("data.tsv");
try (TsvInputStream stream = new TsvInputStream(Files.newInputStream(tsvFile))) {
    stream.forEach(row -> {
        System.out.println("Row: " + row);
    });
}
```

### Using Tokenizer

```java
import oap.tsv.Tokenizer;

// Parse single TSV line
String line = "1\titem1\t100\ttext with spaces";
List<String> tokens = Tokenizer.parse(
    line,
    '\t',              // delimiter
    Integer.MAX_VALUE, // max tokens
    false              // include empty
);

tokens.forEach(System.out::println);
// Output:
// 1
// item1
// 100
// text with spaces
```

### Array Handling

```java
import oap.tsv.TsvArray;

// Handle arrays in TSV
TsvArray array = new TsvArray();

// Add elements
array.add("element1");
array.add("element2");
array.add("element3");

// Get TSV representation
String tsvValue = array.toString();
System.out.println(tsvValue);  // "element1\telement2\telement3"

// Parse array from TSV
TsvArray parsed = TsvArray.parse("value1\tvalue2\tvalue3");
List<String> elements = parsed.values();
System.out.println(elements);  // [value1, value2, value3]
```

### Type Mapping Configuration

```java
import oap.tsv.mapper.Configuration;
import oap.tsv.mapper.Mapper;

// Configure type mapping
Configuration config = Configuration.builder()
    .field("id", Integer.class)
    .field("name", String.class)
    .field("active", Boolean.class)
    .build();

// Create mapper
Mapper<MyData> mapper = new Mapper<>(MyData.class, config);

// Convert TSV row to object
List<String> row = List.of("1", "item", "true");
MyData data = mapper.map(row);
```

### Different Delimiters

```java
// CSV (comma-separated)
List<String> csvTokens = Tokenizer.parse(
    "value1,value2,value3",
    ',',
    Integer.MAX_VALUE,
    false
);

// Semicolon-separated
List<String> semiTokens = Tokenizer.parse(
    "value1;value2;value3",
    ';',
    Integer.MAX_VALUE,
    false
);

// Custom delimiter
List<String> customTokens = Tokenizer.parse(
    "value1|value2|value3",
    '|',
    Integer.MAX_VALUE,
    false
);
```

### Streaming Large Files

```java
import oap.tsv.TsvStream;
import java.nio.file.Files;
import java.nio.file.Path;

// Stream processing for memory efficiency
Path largeFile = Path.of("large_data.tsv");
try (var stream = new TsvStream(Files.newBufferedReader(largeFile))) {
    stream
        .filter(line -> !line.isEmpty())
        .forEach(line -> {
            List<String> values = Tsv.tsv.parse(line);
            processRow(values);
        });
}
```

## Advanced Features

### Custom Tokenizer Options

```java
import oap.tsv.Tokenizer;

// Parse with limited tokens
String line = "1\titem1\t100\textra\tdata";
List<String> tokens = Tokenizer.parse(
    line,
    '\t',
    3,     // limit to 3 tokens
    false
);
// Result: ["1", "item1", "100\textra\tdata"]

// Include empty tokens
String lineWithEmpty = "1\t\tvalue";
List<String> withEmpty = Tokenizer.parse(
    lineWithEmpty,
    '\t',
    Integer.MAX_VALUE,
    true   // include empty
);
// Result: ["1", "", "value"]
```

### Performance Optimization

```java
// For bulk operations, reuse Tokenizer instance
List<List<String>> allRows = new ArrayList<>();

try (TsvStream stream = new TsvStream(reader)) {
    stream
        .map(Tsv.tsv::parse)
        .forEach(allRows::add);
}
```

### Building TSV Data Programmatically

```java
import oap.tsv.Printer;

Printer printer = new Printer();

// Add header
printer.println("id\tname\tactive");

// Add rows using accept
printer.accept("1", "item1", "true");
printer.accept("2", "item2", "false");
printer.accept("3", "item3", "true");

String result = printer.get();
```

## Testing Examples

### From TokenizerTest.java

```java
@Test
public void testTokenization() {
    String line = "id\tname\tvalue";
    List<String> tokens = Tokenizer.parse(line, '\t', Integer.MAX_VALUE, false);
    
    assertThat(tokens)
        .containsExactly("id", "name", "value");
}

@Test
public void testWithSpecialCharacters() {
    String line = "1\titem with spaces\t100";
    List<String> tokens = Tokenizer.parse(line, '\t', Integer.MAX_VALUE, false);
    
    assertThat(tokens)
        .containsExactly("1", "item with spaces", "100");
}
```

### From TsvStreamTest.java

```java
@Test
public void testStreamProcessing() {
    String tsvData = "a\tb\tc\n1\t2\t3\n4\t5\t6";
    
    List<List<String>> rows = new TsvStream(new StringReader(tsvData))
        .map(Tsv.tsv::parse)
        .collect(Collectors.toList());
    
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).containsExactly("a", "b", "c");
    assertThat(rows.get(1)).containsExactly("1", "2", "3");
}
```

### From PrinterTest.java

```java
@Test
public void testPrinting() {
    Printer printer = Printer.values(new String[]{"a", "b", "c"});
    printer.accept("1", "2", "3");
    printer.accept("4", "5", "6");
    
    String output = printer.get();
    assertThat(output).contains("a\tb\tc");
    assertThat(output).contains("1\t2\t3");
}
```

## Integration with Other Modules

### With oap-logstream

```java
import oap.logstream.data.TsvDataTransformer;

// Transform log data to TSV
TsvDataTransformer transformer = new TsvDataTransformer(model);
String tsvOutput = transformer.transform(logData);
```

### With oap-template

```java
import oap.template.TemplateEngine;

// Use templates to generate TSV
TemplateEngine engine = new TemplateEngine();
var template = engine.getTemplate(
    "tsv",
    new TypeRef<Map<String, String>>() {},
    "{{id}}\t{{name}}\t{{value}}",
    STRING,
    null
);
```

## Performance Characteristics

- **Tokenizer**: O(n) where n is line length
- **Streaming**: Constant memory regardless of file size
- **Parsing**: Minimal overhead with simple delimiter detection
- **Best for**: Large files, simple delimited data

## Comparison with Other Formats

| Format | Speed | Compatibility | Compressibility | Use Case |
|--------|-------|---------------|-----------------|----------|
| TSV | Very Fast | High | Good | Log data, analytics |
| CSV | Fast | Very High | Good | Data interchange |
| JSON | Slow | High | Poor | Structured data |
| Parquet | Medium | Medium | Excellent | Big data analytics |

## Building

```bash
mvn clean install
```

Build only oap-tsv:

```bash
mvn -pl oap-formats/oap-tsv clean install
```

## Testing

```bash
mvn test
```

Run specific test class:

```bash
mvn -pl oap-formats/oap-tsv test -Dtest=TokenizerTest
```

Run performance tests:

```bash
mvn -pl oap-formats/oap-tsv test -Dtest=TokenizerPerformance
```

## Related Classes

- `Tsv` - Main API with preset parsers and printers
- `TsvStream` - Streaming TSV processor
- `TsvInputStream` - Input stream wrapper
- `Tokenizer` - Fast character-based tokenizer
- `Printer` - TSV output generator
- `TsvArray` - Array handling
- `Mapper` - Type mapping configuration
- `TsvAssertion` - Testing utilities

## Troubleshooting

### Parsing Issues

Check delimiter configuration:

```java
// Tab delimiter
List<String> tokens = Tokenizer.parse(line, '\t', Integer.MAX_VALUE, false);

// Wrong: Space delimiter would fail on "text with spaces"
List<String> wrongTokens = Tokenizer.parse(line, ' ', Integer.MAX_VALUE, false);
```

### Empty Fields

Enable empty token inclusion:

```java
// Include empty fields
List<String> withEmpty = Tokenizer.parse(
    "1\t\t3",
    '\t',
    Integer.MAX_VALUE,
    true  // Include empty tokens
);
// Result: ["1", "", "3"]
```

### Special Characters

TSV handles special characters naturally:

```java
// No escaping needed for these
String line = "1\tvalue with,commas\tvalue with\ttabs\tdone";
List<String> tokens = Tokenizer.parse(line, '\t', Integer.MAX_VALUE, false);
// Tab is only delimiter, other characters are literal
```

## Best Practices

1. **Always use tabs as delimiter** for true TSV format
2. **Stream large files** to avoid memory issues
3. **Validate data types** after parsing
4. **Precompile patterns** for repeated operations
5. **Use TsvArray** for handling multi-valued fields

## Further Reading

- [TSV vs CSV Comparison](https://github.com/eBay/tsv-utils/blob/master/docs/comparing-tsv-and-csv.md)
- [OAP Framework Documentation](https://github.com/oaplatform/oap)
- [Related oap-logstream Module](../oap-logstream/README.md)
- [Related oap-template Module](../oap-template/README.md)

## License

MIT License - See LICENSE file for details
