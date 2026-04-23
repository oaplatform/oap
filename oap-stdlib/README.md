# oap-stdlib

Core utility library for the OAP platform. Provides serialization, reflection, file I/O, collections, concurrency primitives, and identifier abstractions used across all OAP modules.

## Packages

| Package | Contents |
|---|---|
| `oap.json` | `Binder` — JSON/HOCON/YAML/XML/BSON serializer |
| `oap.reflect` | `Reflect`, `Reflection`, `TypeRef`, `Coercions` |
| `oap.id` | `Identifier`, `StringIdentifier`, `IntIdentifier` |
| `oap.io` | `Files`, `IoStreams`, `Resources`, `ContentReader`, `ContentWriter` |
| `oap.util` | `Stream`, `Cuid`, `Dates`, `Result`, `Lists`, `Maps`, `Sets`, `Strings`, `Pair` |
| `oap.concurrent` | `Executors`, `Threads`, `Scheduler`, `Stopwatch`, `LimitedTimeExecutor` |
| `oap.net` | `Inet`, `IpRangeTree` |
| `oap.dictionary` | `Dictionary` |

---

## `oap.json.Binder`

Pre-configured Jackson `ObjectMapper` wrappers. All instances are thread-safe singletons.

### Static instances

| Instance | Format | Notes |
|---|---|---|
| `Binder.json` | JSON | Standard serializer; skips nulls |
| `Binder.jsonWithTyping` | JSON | Embeds `@class` type info for polymorphic deserialization |
| `Binder.hocon` | HOCON | Resolves `${?ENV_VAR}` and system properties |
| `Binder.hoconWithoutSystemProperties` | HOCON | No system property substitution |
| `Binder.yaml` | YAML | |
| `Binder.xml` | XML | |
| `Binder.xmlWithTyping` | XML | With type info |
| `Binder.bson` | BSON | For MongoDB codecs |

### Serialization

```java
// Object → String
String json = Binder.json.marshal( order );
String pretty = Binder.json.marshalWithDefaultPrettyPrinter( order );

// Object → Path (auto-detects encoding from extension)
Binder.json.marshal( Path.of( "/data/order.json.gz" ), order );

// Streaming JSON array to OutputStream
Binder.json.marshal( outputStream, List.of( order1, order2 ) );
```

### Deserialization

```java
Order order = Binder.json.unmarshal( Order.class, jsonString );
Order order = Binder.json.unmarshal( Order.class, path );
Order order = Binder.json.unmarshal( Order.class, url );
Order order = Binder.json.unmarshal( Order.class, inputStream );

// Generic types — use TypeRef
List<Order> orders = Binder.json.unmarshal( new TypeRef<List<Order>>() {}, jsonString );
```

### Partial update

```java
// Apply HOCON-format overrides to an existing object (preserves unmentioned fields)
Binder.update( order, Map.of( "status", "SHIPPED" ) );
Binder.update( order, "{ status: SHIPPED, total: 99.99 }" );
```

### Dynamic config binders

```java
// Parse HOCON with extra fallback properties merged in
Binder b = Binder.hoconWithConfig( Map.of( "host", "localhost", "port", 8080 ) );
MyConfig cfg = b.unmarshal( MyConfig.class, "classpath:config.conf" );
```

### Jackson customization

Extra Jackson modules are registered by listing them in `META-INF/jackson.modules` (one class name per line). The default configuration:
- Field visibility: `ANY` (no getters required)
- Accepts case-insensitive property names
- Accepts single-quoted strings
- Skips null input values on deserialization
- Omits null fields on serialization
- Joda-Time, JDK8, JavaTime modules registered

---

## `oap.reflect.TypeRef<T>`

Java generic type token. Use it anywhere a `Class<T>` cannot carry generic parameters.

```java
TypeRef<List<Order>> ref = new TypeRef<List<Order>>() {};
List<Order> orders = Binder.json.unmarshal( ref, json );

// Also accepted by Reflect
Reflection r = Reflect.reflect( ref );
```

---

## `oap.reflect.Reflect` / `Reflection`

OAP's cached reflection layer, built on Guava `TypeToken`.

```java
Reflection r = Reflect.reflect( Order.class );

// Construct
Order order = r.newInstance();
Order order = r.newInstance( Map.of( "id", "o-1", "total", 42 ) );

// Fields
Reflection.Field f = r.field( "status" ).orElseThrow();
f.set( order, "SHIPPED" );
Object v = f.get( order );

// Methods
r.method( "validate" ).ifPresent( m -> m.invoke( order ) );

// Iterate all declared fields
r.fields.forEach( field -> System.out.println( field.name() + " : " + field.type() ) );
```

`Coercions.basic()` is the default type-coercion registry (String→int, String→enum, etc.) used by the Kernel when wiring service parameters.

---

## `oap.id.Identifier<I, T>`

Strategy interface that extracts, generates, and converts the ID of a data object.

```java
// Explicit getter+setter (most common)
Identifier<String, Order> id = Identifier
    .forId( o -> o.id, ( o, newId ) -> o.id = newId )
    .suggestion( o -> o.customerName )   // derive initial id from this field
    .length( 10 )                        // max generated id length
    .build();

// Derive from a JPath expression
Identifier<String, Order> id = Identifier.<Order>forPath( "$.id" ).build();

// Use @Id annotation on the field
Identifier<String, Order> id = Identifier.<Order>forAnnotation().build();
```

`Identifier.generate(base, length, conflict, maxAttempts, options)` — slug generator with deconfliction:
- `Option.COMPACT` — removes vowels from the base (shorter slugs)
- `Option.FILL` — pads with `X` to reach `length`

---

## `oap.io.Files`

Static file system utilities.

```java
// Read
String text = Files.readString( path );
String text = Files.readString( path, encoding );
byte[] bytes = Files.read( path, Encoding.GZIP, ContentReader.ofBytes() );

// Write
Files.writeString( path, Encoding.PLAIN, "hello" );
Files.writeString( path, Encoding.GZIP, "hello", /* append */ false );

// Glob matching (Ant-style wildcards)
List<Path> found = Files.wildcard( basePath, "**/*.json" );
List<Path> found = Files.wildcard( basePath, "logs/*.log", "logs/*.log.gz" );

// Directory operations
Files.ensureFile( path );        // creates parent directories; does not create the file
Files.ensureDirectory( path );   // creates the directory and all parents
Files.delete( path );            // recursive delete
Files.copyDirectory( src, dest );
Files.move( src, dest );         // atomic rename where possible

// Metadata
long ts = Files.getLastModifiedTime( path );    // epoch ms
boolean exists = Files.exists( path );

// Hashed subdirectory (distributes many files across a 3-level tree)
Path deep = Files.deepPath( basePath, filename );
```

---

## `oap.io.IoStreams`

Stream I/O with transparent compression support.

### `Encoding`

```java
Encoding.PLAIN    // no compression
Encoding.GZIP     // gzip
Encoding.BZIP2    // bzip2
Encoding.LZ4      // LZ4
Encoding.ZSTD     // Zstandard
Encoding.ZIP      // ZIP

// Auto-detect from path or URL extension
Encoding enc = Encoding.from( path );
Encoding enc = Encoding.from( url );
```

### Reading

```java
InputStream in = IoStreams.in( path );
InputStream in = IoStreams.in( path, Encoding.GZIP );

Stream<String> lines = IoStreams.lines( path );
Stream<String> lines = IoStreams.lines( path, Encoding.GZIP );
Stream<String> lines = IoStreams.lines( url );
Stream<String> lines = IoStreams.lines( inputStream );
```

### Writing

```java
OutputStream out = IoStreams.out( path );
OutputStream out = IoStreams.out( path, Encoding.GZIP );
OutputStream out = IoStreams.out( path, Encoding.GZIP, /* append */ true );

IoStreams.write( path, Encoding.GZIP, "text content" );
IoStreams.write( path, Encoding.GZIP, inputStream );
IoStreams.write( path, Encoding.PLAIN, lineStream );  // Stream<String>, one line per element
```

---

## `oap.io.Resources`

Classpath resource loading relative to a context class.

```java
// Single resource
Optional<URL>  url  = Resources.url( MyClass.class, "config.conf" );
Optional<Path> path = Resources.filePath( MyClass.class, "config.conf" );

// All resources with this name across all jars (useful for META-INF aggregation)
List<URL>  urls  = Resources.urls( "META-INF/services/MyService" );
List<Path> paths = Resources.filePaths( MyClass.class, "schemas" );

// Read content
Optional<String> text = Resources.read( MyClass.class, "query.sql", ContentReader.ofString() );
Stream<String>   lines = Resources.lines( "META-INF/jackson.modules" );
```

---

## `oap.io.content.ContentReader` / `ContentWriter`

Typed I/O adapters passed to `Files.read()`, `IoStreams.write()`, and cloud storage APIs.

### `ContentReader` factories

| Factory | Returns |
|---|---|
| `ContentReader.ofString()` | `String` (UTF-8) |
| `ContentReader.ofBytes()` | `byte[]` |
| `ContentReader.ofLines()` | `List<String>` |
| `ContentReader.ofLinesStream()` | `Stream<String>` |
| `ContentReader.ofInputStream()` | `InputStream` (caller must close) |

Chain readers with `.andThen(fn)`:
```java
ContentReader<MyObj> r = ContentReader.ofString()
    .andThen( s -> Binder.json.unmarshal( MyObj.class, s ) );
```

### `ContentWriter` factories

| Factory | Writes |
|---|---|
| `ContentWriter.ofString()` | String → UTF-8 bytes |
| `ContentWriter.ofBytes()` | `byte[]` pass-through |
| `ContentWriter.ofJson()` | Any object → JSON bytes via `Binder.json` |
| `ContentWriter.ofObject()` | Java serialization (`ObjectOutputStream`) |

---

## `oap.util.Cuid`

Cluster-unique identifier — time-based, monotonic, embeds the local IP address.

```java
// Production: globally unique, embeds timestamp + local IP
String id   = Cuid.UNIQUE.next();       // e.g. "0000018F3A2B1C00C0A80101"
long   idL  = Cuid.UNIQUE.nextLong();
String last = Cuid.UNIQUE.last();       // last generated (no increment)

// Parse a Cuid back to components
Cuid.UniqueCuid.Info info = Cuid.UniqueCuid.parse( id );
// info.time  → DateTime (UTC)
// info.ip    → int[4]
// info.count → per-millisecond counter

// Tests: deterministic counter starting at seed
Cuid counter = Cuid.incremental( 1 );
counter.next();     // "1"
counter.next();     // "2"
```

---

## `oap.util.Dates`

Joda-Time utilities. All operations use UTC unless otherwise noted.

### Formatters

| Constant | Pattern | Example |
|---|---|---|
| `Dates.FORMAT_MILLIS` | `yyyy-MM-dd'T'HH:mm:ss.SSS` | `2024-06-01T14:30:00.000` |
| `Dates.FORMAT_SIMPLE` | `yyyy-MM-dd'T'HH:mm:ss` | `2024-06-01T14:30:00` |
| `Dates.FORMAT_DATE` | `yyyy-MM-dd` | `2024-06-01` |

```java
String s = Dates.formatDateWithMillis( DateTime.now() );
String s = Dates.FORMAT_DATE.print( dt );

Result<DateTime, Exception> r = Dates.parseDateWithMillis( "2024-06-01T14:30:00.000" );
Result<DateTime, Exception> r = Dates.parseDate( "2024-06-01T14:30:00" );
DateTime now   = Dates.nowUtc();
DateTime today = Dates.nowUtcDate();   // time zeroed to 00:00:00.000
```

### Duration constants (return milliseconds as `long`)

```java
Dates.s( 30 )   // 30 seconds in ms
Dates.m( 5 )    // 5 minutes in ms
Dates.h( 2 )    // 2 hours in ms
Dates.d( 7 )    // 7 days in ms
Dates.w( 2 )    // 2 weeks in ms

String human = Dates.durationToString( Dates.h(1) + Dates.m(30) ); // "1h 30m"
```

### Controllable clock (for tests)

```java
Dates.setTimeFixed( 2024, 6, 1, 14, 30, 0 );   // freeze at 14:30:00 UTC
Dates.incFixed( Dates.h( 1 ) );                  // advance by 1 hour
DateTimeUtils.setCurrentMillisSystem();           // restore real clock
```

---

## `oap.util.Stream<E>`

OAP's extended stream — wraps `java.util.stream.Stream` and adds extra operations.

```java
// Factory methods
Stream<T> s = Stream.of( collection );
Stream<T> s = Stream.of( iterator );
Stream<T> s = Stream.of( enumeration );
Stream<T> s = Stream.traverse( initialState, nextFn );  // iterator-style generator

// Extra intermediates
stream.takeWhile( predicate )          // stop at first non-matching element
stream.grouped( batchSize )           // → Stream<List<E>> in fixed-size batches
stream.grouped( classifier )         // → BiStream<K, List<E>> grouped by key
stream.zip( otherStream, zipper )    // pair-wise transform into a new type
stream.zip( otherStream )            // → BiStream<E, B>

// Extra terminals
List<E>  list = stream.toList();
Set<E>   set  = stream.toSet();
Map<K,V> map  = stream.toMap( keyFn, valueFn );
```

---

## `oap.util.Lists` / `Maps` / `Sets` / `Strings`

Static utility classes.

```java
// Lists
List<B>      mapped   = Lists.map( list, fn );
List<T>      filtered = Lists.filter( list, pred );
List<T>      concat   = Lists.concat( listA, listB );
List<T>      reversed = Lists.reverse( list );
Optional<T>  head     = Lists.head( list );

// Maps
Map<K,V>           filtered = Maps.filter( map, ( k, v ) -> pred );
List<R>            asList   = Maps.toList( map, ( k, v ) -> ... );
LinkedHashMap<K,V> linked   = Maps.toLinkedHashMap( list, keyFn, valueFn );

// Sets
Set<T> intersection = Sets.intersection( setA, setB );
Set<T> union        = Sets.union( setA, setB );
Set<T> difference   = Sets.difference( setA, setB );

// Strings
String result = Strings.substitute( "Hello ${name}!", Map.of( "name", "World" ) );
String sorted = Strings.sortLines( multilineString );
byte[] bytes  = Strings.toByteArray( str );
String hex    = Strings.toHexString( bytes );
```

---

## `oap.util.Result<S, F>`

Typed success/failure without exceptions.

```java
Result<Order, String> r = Result.success( order );
Result<Order, String> r = Result.failure( "not found" );

// Wrap a throwing supplier — catches all Throwable
Result<Order, Throwable> r = Result.catching( () -> orderService.find( id ) );

// Query
boolean ok     = r.isSuccess();
Order   order  = r.successValue;
String  reason = r.failureValue;

// Transform
Result<String, String>    r2 = r.mapSuccess( o -> o.id );
Result<Order, Throwable>  r3 = r.mapFailure( msg -> new RuntimeException( msg ) );

// Branch
r.ifSuccess( o -> log.info( "ok: {}", o.id ) )
 .ifFailure( e -> log.warn( "failed: {}", e ) );

// Terminate
Optional<Order> opt   = r.toOptional();
Order           order = r.orElse( defaultOrder );
Order           order = r.orElseThrow( msg -> new RuntimeException( msg ) );
```

---

## `oap.net.Inet`

```java
Optional<InetAddress> ip   = Inet.getLocalIp();
String                host = Inet.hostName();
```

---

## `oap.concurrent.Executors`

```java
// Named scheduled thread pool
ScheduledExecutorService exec = Executors.newScheduledThreadPool( 4, "my-service" );

// Named single-thread executor
ExecutorService exec = Executors.newSingleThreadExecutor( "my-worker" );
```

Thread names include the pool name for easy identification in thread dumps and profilers.
