# oap-tsv

TSV and CSV parsing, streaming, and printing for the OAP platform. Provides a lazy `TsvStream` pipeline for reading, projecting, filtering, and serialising tabular data.

## Sub-modules

| Module | Description |
|---|---|
| `oap-tsv` | Core: `Tsv`, `TsvStream`, `Printer`, `Tokenizer` |
| `oap-tsv-test` | `TsvAssertion` — AssertJ-style fluent assertions for `TsvStream` |

---

## Parsing

`Tsv` exposes two pre-built parsers as constants:

```java
Tsv.tsv   // tab-delimited
Tsv.csv   // comma-delimited, handles quoted fields
```

Both expose the same `AbstractParser` API:

```java
// From a Reader (preferred)
TsvStream stream = Tsv.tsv.from( reader );

// From an InputStream
TsvStream stream = Tsv.tsv.from( inputStream, StandardCharsets.UTF_8 );

// From a line stream (when lines are already split)
TsvStream stream = Tsv.tsv.fromStream( Stream.of( lines ) );

// From a URL with encoding and progress callback
TsvStream stream = Tsv.tsv.fromUrl( url, IoStreams.Encoding.GZIP, bytesRead -> {} );

// As a ContentReader (for use with Files.read / IoStreams)
ContentReader<TsvStream> reader = Tsv.tsv.ofSeparatedValues();
```

---

## `TsvStream`

A lazy pipeline over rows represented as `List<String>`. All operations return a new `TsvStream`; nothing is evaluated until a terminal method is called.

### Header handling

```java
// Promote the first row to headers (consumed from the data stream)
TsvStream stream = Tsv.tsv.from( reader ).withHeaders();

// Access the header list
List<String> headers = stream.headers();

// Remove headers (yields a headerless stream)
TsvStream noHeaders = stream.stripHeaders();
```

### Column projection

```java
// By zero-based index
TsvStream projected = stream.select( 0, 2, 5 );

// By header name (requires withHeaders() first)
TsvStream projected = stream.select( "id", "price", "qty" );

// By Header object
TsvStream.Header h = new TsvStream.Header( "id", "price" );
TsvStream projected = stream.select( h );
```

### Filtering

```java
TsvStream filtered = stream.filter( row -> !row.get( 0 ).isEmpty() );
```

### Mapping

```java
// Map rows to domain objects
Stream<Order> orders = stream.mapToObj( row -> new Order( row.get(0), row.get(1) ) );
```

### Terminal operations

```java
// Collect to String
String tsv  = stream.toTsvString();    // tab-delimited, newline after each row
String csv  = stream.toCsvString();    // comma-delimited, quoted
String csv2 = stream.toCsvString( false );  // comma-delimited, unquoted

// Collect to List
List<List<String>> rows = stream.toList();

// Snapshot as Tsv (in-memory)
Tsv tsv = stream.toTsv();

// Stream to an OutputStream
stream.collect( TsvStream.Collectors.toTsvOutputStream( outputStream ) );

// Any custom Collector
stream.collect( myCollector );
```

---

## `Printer`

Low-level row serialiser.

```java
// Single row → String (tab-delimited, newline appended)
String line = Printer.print( List.of( "a", "b\tc" ), '\t' );          // "a\tb\tc\n"
String line = Printer.print( List.of( "a", "say \"hi\"" ), ',', true ); // quoted CSV

// Stream of rows → String
String out  = Printer.print( Stream.of( rows ), '\t' );
```

### Escaping rules (`Printer.escape`)

| Character | Escaped as |
|---|---|
| `\n` | `\n` |
| `\r` | `\r` |
| `\t` | `\t` |
| `\` | `\\` |
| `"` | `""` (in quoted mode) or `"` (unquoted) |
| Empty/null | `""` (empty string) |

In quoted mode (`quoted = true`) the value is wrapped in double-quote characters after escaping.

---

## `TsvAssertion` (oap-tsv-test)

AssertJ-style fluent assertions for `TsvStream` or `Tsv` in TestNG tests.

```java
import static oap.tsv.test.TsvAssertion.assertTsv;

assertTsv( Tsv.tsv.from( reader ).withHeaders() )
    .hasHeaders( "id", "name", "price" )
    .hasRowCount( 3 )
    .row( 0 ).hasValues( "o-1", "Widget", "9.99" );
```
