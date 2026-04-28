# RowBinaryObjectLogger

Logs Java objects in ClickHouse [RowBinary](https://clickhouse.com/docs/interfaces/formats/RowBinary) format. It reads a column schema from an OAP dictionary model, compiles a template renderer for a given Java type, and hands the serialized binary rows to any `AbstractLoggerBackend`.

---

## Constructors

```java
// Provide a pre-built TemplateEngine (share one engine across multiple loggers)
new RowBinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend, TemplateEngine engine )

// Convenience — creates a TemplateEngine with a disk template cache
new RowBinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend,
                           Path diskCache, long ttl )
```

| Parameter | Description |
|---|---|
| `model` | OAP dictionary that contains the column schema |
| `backend` | Destination backend (`DiskLoggerBackend`, `SocketLoggerBackend`, `MemoryLoggerBackend`, …) |
| `engine` | Pre-configured `TemplateEngine` |
| `diskCache` | Directory where compiled templates are cached on disk |
| `ttl` | Cache TTL (milliseconds; use `Dates.d(n)` for days) |

---

## Supported column types

| Schema `type` | Java class | ClickHouse type |
|---|---|---|
| `STRING` | `java.lang.String` | `String` |
| `INTEGER` | `java.lang.Integer` | `Int32` |
| `LONG` | `java.lang.Long` | `Int64` |
| `SHORT` | `java.lang.Short` | `Int16` |
| `FLOAT` | `java.lang.Float` | `Float32` |
| `DOUBLE` | `java.lang.Double` | `Float64` |
| `BOOLEAN` | `java.lang.Boolean` | `Bool` |
| `DATETIME` | `org.joda.time.DateTime` | `DateTime` |
| `ENUM` | any `java.lang.Enum` subclass | `String` |
| `<TYPE>_ARRAY` | `java.util.Collection<T>` | `Array(<type>)` |

Array types are formed by appending `_ARRAY` to any base type, e.g. `STRING_ARRAY`, `INTEGER_ARRAY`.

---

## Schema definition (HOCON dictionary format)

```hocon
name = model
values {
  MODEL_ID {                           # ID string passed to typed()
    values {
      column_name {
        path    = object.field.path    # template path expression (required to log the column)
        type    = STRING               # type name from the table above
        default = ""                   # value used when path resolves to null (required)
        format  = "yyyy-MM-dd"         # optional format function
      }
      # Columns without a "path" property are silently excluded from logging.
    }
  }
}
```

### Schema properties

| Property | Required | Description |
|---|---|---|
| `path` | Yes* | Template path to extract the value from the object. Supports fallback: `a \| default b`. Columns without `path` are excluded. |
| `type` | Yes | Column type from the supported types table |
| `default` | Yes | Default value when `path` resolves to null or the field is absent |
| `format` | No | Format function applied after value extraction (e.g. date pattern) |

---

## Creating a typed logger

```java
TypedRowBinaryLogger<MyEvent> typed = logger.typed( new TypeRef<>() {}, "MODEL_ID" );
```

`typed()` compiles a binary template for the given Java type `D` and the column schema identified by `MODEL_ID`. The returned `TypedRowBinaryLogger<D>` is thread-safe and should be reused.

---

## Logging rows

```java
typed.log( event, "file-prefix", Map.of( "region", "eu" ), "EVENT_TYPE" );
```

| Parameter | Type | Description |
|---|---|---|
| `data` | `D` | Domain object to serialize |
| `filePreffix` | `String` | Log file prefix passed to the backend |
| `properties` | `Map<String, String>` | Custom metadata forwarded to the backend |
| `logType` | `String` | Log type string; used by `DiskLoggerBackend` when building file names |

Each call renders one RowBinary row and calls `backend.log()` with protocol version `ROW_BINARY_V3`.

---

## Availability

```java
logger.isLoggingAvailable()     // true when backend state == OPERATIONAL
logger.availabilityReport()     // full AvailabilityReport (state + subsystem states)
```

---

## Full example

```java
String datamodel = """
    name = model
    values {
      MODEL1 {
        values {
          a    { path = a;                              type = STRING;       default = ""  }
          b    { path = b;                              type = INTEGER;      default = 0   }
          aaa  { path = a | default aa;                 type = STRING;       default = ""  }
          list { path = data1.list | default data2.list; type = STRING_ARRAY; default = [] }
        }
      }
    }
    """;

DictionaryRoot model = DictionaryParser.parseFromString( datamodel );
MemoryLoggerBackend backend = new MemoryLoggerBackend();

RowBinaryObjectLogger logger = new RowBinaryObjectLogger(
    model, backend, Path.of( "/tmp/template-cache" ), Dates.d( 10 ) );

TypedRowBinaryLogger<MyData> typed = logger.typed( new TypeRef<>() {}, "MODEL1" );

typed.log( new MyData( "hello", "fallback", 42, List.of( "x" ) ),
           "prefix", Map.of(), "my-log" );

// In tests — inspect logged rows as deserialized objects:
List<List<Object>> rows = backend.asRowBinary( _ -> true );
// rows.get(0) → ["hello", 42, "hello", ["x"]]
```

---

## Related classes

| Class | Description |
|---|---|
| `RowBinaryOutputStream` | Low-level writer for the ClickHouse RowBinary wire format |
| `RowBinaryInputStream` | Low-level reader for the ClickHouse RowBinary wire format |
| `RowBinaryUtils` | Static helpers: `read(byte[])`, `lines(rows)`, `line(cols)` |
| `TemplateAccumulatorRowBinary` | Template engine accumulator that serializes values to RowBinary bytes |
| `AbstractLoggerBackend` | Base class for all backends (`DiskLoggerBackend`, `SocketLoggerBackend`, `MemoryLoggerBackend`) |
