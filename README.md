# <img src="oap_logo.png" width="165" height="90"> Open Application Platform

A light-weight application framework to build high performant and distributed java applications.

## Modules

| Module | Description                                                                                                 |
|---|-------------------------------------------------------------------------------------------------------------|
| [oap-application](#oap-application) | IoC/DI Kernel — discovers services from HOCON descriptors, wires dependencies, manages start/stop lifecycle |
| [oap-stdlib](#oap-stdlib) | Core utilities — `Binder` (JSON/HOCON/YAML/XML), `Files`, `IoStreams`, `Cuid`, `Dates`, `Stream`, `Result`  |
| [oap-http](#oap-http) | Undertow-based HTTP server with named ports, PNIO high-performance pipeline, and HTTP client                |
| [oap-ws](#oap-ws) | Annotation-driven web services (`@WsMethod`, `@WsParam`) with session and interceptor support               |
| [oap-jpath](#oap-jpath) | JPath expression language for navigating object graphs: `${var.field.method().array[n]}`                    |
| [oap-formats](#oap-formats) | Template engine, TSV/CSV parsing, JSON schema validation, and log streaming                                 |
| [oap-statsdb](#oap-statsdb) | Distributed in-memory statistics tree with hierarchical rollup and MongoDB persistence                      |
| [oap-message](#oap-message) | Reliable binary message delivery with disk spill, retry, and MD5-based deduplication                        |
| [oap-storage](#oap-storage) | In-memory object store (`MemoryStorage`) with MongoDB sync and cloud object storage                         |
| [oap-highload](#oap-highload) | CPU affinity utility for pinning threads to specific CPU cores                                              |
| [oap-mail](#oap-mail) | Email sending via SMTP and SendGrid with a persistent delivery queue                                        |
| [oap-maven-plugin](#oap-maven-plugin) | Build-time code generation: startup scripts and dictionary enum source files                                |

## Guides

| Guide | Description |
|---|---|
| [Ext — Pluggable Field Extensions](docs/extension.md) | Attach pluggable typed sub-objects to bean fields via `oap.json.ext.Ext`; covers JSON deserialization and template engine integration |
| [Testing](docs/testing.md) | Fixture lifecycle, `KernelFixture`, assertion helpers, MongoDB/S3 mocks, and benchmark harness |

## Related Projects

| Project | Description |
|---|---|
| [oap-config](https://github.com/oaplatform/config) | HOCON-based configuration library used by the Kernel to parse `oap-module.oap` / `application.conf` |
| [oap-config-plugin](https://github.com/oaplatform/intellij-hocon) | IntelliJ IDEA plugin: HOCON language support (syntax highlighting, references) |
| [oap-application-plugin](https://github.com/oaplatform/oap-application-plugin) | IntelliJ IDEA plugin: navigation/completion for OAP Kernel service definitions |

---

## oap-application

The IoC/DI kernel for the OAP framework. `Kernel` discovers service descriptors from every jar on the classpath, builds a dependency graph, instantiates and wires services, and manages their full lifecycle (start, scheduled runs, stop).

Services are plain Java classes — no framework annotations required. Everything is declared in HOCON files.

### Table of Contents

- [Overview](#overview)
- [Module Declaration (oap-module.oap)](#module-declaration-oap-moduleoap)
- [Application Configuration (application.conf)](#application-configuration-applicationconf)
- [Reference Syntax](#reference-syntax)
- [Supervision and Service Lifecycle](#supervision-and-service-lifecycle)
- [Lifecycle Annotations](#lifecycle-annotations)
- [Dependency Injection Mechanics](#dependency-injection-mechanics)
- [Abstract Services](#abstract-services)
- [Module Discovery](#module-discovery)
- [Kernel API](#kernel-api)
- [KernelExt: Service Metadata Extensions](#kernelext-service-metadata-extensions)
- [Testing with KernelFixture](#testing-with-kernelfixture)
- [Production Boot](#production-boot)
- [Error Reference](#error-reference)

---

### Overview

Startup sequence:

```
1. Scan classpath for all META-INF/oap-module.oap files
2. Load and validate each module descriptor
3. Read application.conf (+ conf.d/ + CONFIG.* env vars)
4. Select modules reachable via boot.main transitive dependsOn graph
5. Topological sort modules and services
6. Instantiate each service (constructor or field injection)
7. Wire references, listeners, and links
8. Start supervised services via Supervisor
```

`Boot` is the production entry point. `KernelFixture` is the test entry point.

---

### Module Declaration (oap-module.oap)

Every OAP jar ships a descriptor at `src/main/resources/META-INF/oap-module.oap`.

#### Module fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | String | required | Unique module identifier; must match `[A-Za-z\-_0-9]+` |
| `enabled` | boolean | `true` | Disable the entire module and all its services |
| `dependsOn` | list\<String\> | `[]` | Module-level ordering: this module starts after listed modules |
| `services` | map | required | Map of service name → service block |

#### Service fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `implementation` | String | required | Fully-qualified class name |
| `abstract` | boolean | `false` | Marks an interface/abstract-class slot; must be filled at deployment via `application.conf` |
| `default` | reference | — | Default concrete implementation used when `abstract=true` and nothing is specified in `application.conf` |
| `enabled` | boolean | `true` | Disable this service individually |
| `parameters` | map | `{}` | Constructor or field values; may contain `<...>` references |
| `supervision` | block | — | Lifecycle management (see [Supervision](#supervision-and-service-lifecycle)) |
| `dependsOn` | list | `[]` | Explicit start-order hints within a module |
| `listen` | map | `{}` | Listener registration: `listenerName = <ref>` calls `ref.addListenerNameListener(this)` |
| `link` | map | `{}` | Reverse wiring: `fieldName = <ref>` calls `ref.addFieldName(this)` / `ref.setFieldName(this)` / appends to collection |

> The `service` and `services` keys are aliases for the `services` map.

#### Example: two-module setup

**m1.oap**
```hocon
name = m1

services {
  cm {
    implementation = com.example.ComplexMap
  }

  ServiceOneP1 {
    implementation = com.example.ServiceOne
    parameters {
      i          = 2ms
      kernel     = <kernel.self>
      complexMap = <modules.this.cm>
      complex {
        i = 2
        map.a.i = 1
      }
      complexes = [{i = 2}]
    }
    supervision.delay = 5ms
  }
}
```

**m2.oap**
```hocon
name = m2
dependsOn = m1

services {
  ServiceTwo {
    implementation = com.example.ServiceTwo
    parameters {
      j   = 1
      one = <modules.m1.ServiceOneP1>
    }
    listen.some = <modules.m1.ServiceOneP1>
    supervision.supervise = true
  }

  ServiceScheduled {
    implementation = com.example.ServiceScheduled
    supervision {
      schedule = true
      delay    = 1s
    }
  }
}
```

**m3.oap** (list parameter referencing services from two modules)
```hocon
name = m3
dependsOn = [m1, m2]

services {
  ServiceDepsList {
    implementation = com.example.ServiceDepsList
    parameters {
      deps = [
        <modules.m1.ServiceOneP1>
        <modules.m2.ServiceTwo>
      ]
    }
  }
}
```

---

### Application Configuration (application.conf)

`application.conf` selects which modules activate and overrides their parameters at deployment time.

#### Schema

```hocon
boot.main = [m1, m2, m3]   # one or more module names

shutdown {
  serviceTimeout                = 5s      # warn timeout per service during stop
  serviceAsyncShutdownAfterTimeout = false  # continue stopping if timeout exceeded
}

services {
  # Override a parameter
  m1.ServiceOneP1.parameters.i = 100ms

  # Disable a service
  m2.ServiceTwo.enabled = false

  # Assign a concrete implementation to an abstract service slot
  my-module.my-abstract-service = <modules.impl-module.ConcreteImpl>
}
```

#### conf.d/ directory

All `*.conf` and `*.yaml` files in `conf.d/` are merged with `application.conf`. The default `conf.d` path is `<application.conf parent>/conf.d`. Useful for splitting deployment-specific values across files.

```
# conf.d/ports.conf
services.my-module.my-service.parameters.port = 9090

# conf.d/db.yaml
services:
  my-module:
    db-service:
      parameters:
        url: jdbc:postgresql://localhost/mydb
```

#### CONFIG.* environment variable overrides

Any environment variable starting with `CONFIG.` is stripped of the prefix and injected as a HOCON key. This allows per-deployment overrides without modifying config files.

```bash
export CONFIG.services.my-module.my-service.enabled=false
export CONFIG.services.my-module.my-service.parameters.val='"hello"'
```

#### Programmatic startup (tests / embedded)

```java
kernel.start( Map.of(
    "boot.main", "m1",
    "services.m1.ServiceOneP1.parameters.i", "50"
) );
```

---

### Reference Syntax

`<...>` expressions in parameter values are resolved by the kernel before service construction.

| Expression | Resolves to |
|---|---|
| `<modules.moduleName.serviceName>` | The live instance of the named service |
| `<modules.this.serviceName>` | A service in the same module |
| `<modules.self.serviceName>` | Alias for `this` |
| `<modules.*.serviceName>` | First matching service across all modules |
| `<kernel.self>` | The `Kernel` instance itself |
| `<services.self.name>` | The string name of the current service |
| `location.module` | The `URL` of the module's own `.oap` file |

References work in `parameters`, list parameters, and map parameters.

```hocon
# Inject the kernel itself
parameters.kernel = <kernel.self>

# Cross-module reference
parameters.server = <modules.oap-http.oap-http-server>

# Same-module reference
parameters.cache = <modules.this.cache-service>

# Wildcard: first service named "config" in any module
parameters.config = <modules.*.config>

# Service's own registered name
parameters.serviceName = <services.self.name>
```

---

### Supervision and Service Lifecycle

The `supervision` block controls how the kernel starts, runs, and stops a service.

#### Fields

| Field | Type | Default | Effect |
|---|---|---|---|
| `supervise` | boolean | `false` | Call lifecycle methods on start/stop |
| `thread` | boolean | `false` | Run service as a `Runnable` in a dedicated daemon thread |
| `schedule` | boolean | `false` | Run service periodically (combine with `delay` or `cron`) |
| `delay` | duration | `0` | Fixed-delay interval; supports HOCON duration units (`1s`, `5ms`, `1h`) |
| `cron` | String | — | Quartz cron expression for scheduled runs |
| `preStartWith` | list\<String\> | `["preStart"]` | Method names called before `start` |
| `startWith` | list\<String\> | `["start"]` | Method names called on start |
| `preStopWith` | list\<String\> | `["preStop"]` | Method names called before stop |
| `stopWith` | list\<String\> | `["stop","close"]` | Method names called on stop; services implementing `Closeable` get `close()` called automatically |

Missing lifecycle methods are silently skipped.

#### Lifecycle examples

**Supervised service** — lifecycle methods called in order:

```hocon
service {
  implementation = com.example.MyService   # has preStart/start/preStop/stop methods
  supervision.supervise = true
}
```

Result on start: `preStart()` → `start()`  
Result on stop: `preStop()` → `stop()`

**Supervised thread** — runs `Runnable.run()` in a daemon thread:

```hocon
thread {
  implementation = com.example.WorkerService
  supervision {
    supervise = true
    thread    = true
  }
}
```

**Delay-scheduled** — runs `Runnable.run()` every N ms with fixed delay:

```hocon
poller {
  implementation = com.example.PollerService
  supervision {
    schedule = true
    delay    = 30s
  }
}
```

**Cron-scheduled** — runs `Runnable.run()` on a Quartz cron schedule:

```hocon
nightly-cleanup {
  implementation = com.example.CleanupJob
  supervision {
    supervise = true
    schedule  = true
    cron      = "0 0 2 * * ? *"   # every day at 02:00 UTC
  }
}
```

When `supervise = true` is combined with `schedule = true`, lifecycle methods are called around the entire scheduler lifetime (start before first run, stop after last run).

#### Shutdown sequence

On `kernel.stop()`, the `Supervisor` stops services in reverse registration order:
1. Threads and scheduled tasks are interrupted/cancelled.
2. Supervised services have `preStop()` then `stop()` (or `close()`) called.

`shutdown.serviceTimeout` (default `5s`) is the warn threshold per service. Set `shutdown.serviceAsyncShutdownAfterTimeout = true` to continue shutdown after a timeout rather than waiting.

#### Lifecycle Annotations

As an alternative to relying on method names (`preStart`, `start`, etc.), lifecycle hooks can be declared with annotations from the `oap.application.annotation` package. The kernel discovers annotated methods at startup regardless of their name.

| Annotation | Phase | Equivalent supervision field |
|---|---|---|
| `@PreStart` | Before service start | `preStartWith` |
| `@Start` | Service start | `startWith` |
| `@PreStop` | Before service stop | `preStopWith` |
| `@Stop` | Service stop | `stopWith` |

Annotations and name-based discovery are independent — both are applied. An annotated method named `start` is invoked by both paths; an annotated method with any other name is invoked only via the annotation.

```java
import oap.application.annotation.PreStart;
import oap.application.annotation.Start;
import oap.application.annotation.PreStop;
import oap.application.annotation.Stop;

public class MyService {
    @PreStart
    public void onBeforeStart() { /* runs before start */ }

    @Start
    public void onStart() { /* runs on start */ }

    @PreStop
    public void onBeforeStop() { /* runs before stop */ }

    @Stop
    public void onStop() { /* runs on stop */ }
}
```

The service still needs `supervision.supervise = true` in its `oap-module.oap` declaration.

---

### Dependency Injection Mechanics

#### Constructor injection

The kernel reflects on the service class and calls a constructor whose parameter names match keys in `parameters`. References are resolved first, then scalars are type-coerced.

```java
public class ServiceTwo {
    public ServiceTwo( ServiceOne one, int j ) { ... }
}
```

```hocon
ServiceTwo {
  implementation = com.example.ServiceTwo
  parameters {
    one = <modules.m1.ServiceOneP1>
    j   = 42
  }
}
```

#### Field injection

Parameters not consumed by the constructor are applied to public fields by name.

#### Nested object parameters

Maps in `parameters` are bound to nested objects:

```hocon
parameters.complex {
  i = 2
  map.a.i = 1
}
```

#### Listen wiring

`listen.name = <ref>` — after construction, calls `ref.addNameListener(this)`. The target service must have a method `addNameListener(T listener)`.

```hocon
ServiceTwo {
  listen.some = <modules.m1.ServiceOneP1>   # calls ServiceOneP1.addSomeListener(serviceTwo)
}
```

#### Link wiring (reverse injection)

`link.name = <ref>` — after construction, registers `this` on the target service. The kernel attempts in order:
1. `ref.addName(this)`
2. `ref.setName(this)`
3. `ref.addNameListener(this)`
4. `ref.name` field (appends if collection, sets otherwise)

```hocon
ti1 {
  implementation = com.example.Impl
  link.registry = <modules.this.service-registry>
}
```

#### Disabled services

A disabled service referenced via `<modules...>` resolves to `null` and is omitted from list parameters. No error is thrown.

#### Cyclic dependencies

Cyclic module or service dependencies are detected at startup and throw `ApplicationException("cyclic dependency detected")`.

---

### Abstract Services

The abstract service pattern defines an interface slot in a module that must be filled with a concrete implementation — either by a `default` fallback or by an explicit assignment in `application.conf`.

#### Declaring an abstract service

```hocon
# oap-module.oap
name = my-module

services {
  abstract-service {
    abstract        = true
    implementation  = com.example.AbstractService   # interface or abstract class
    default         = <modules.my-module.default-impl>  # optional fallback
  }

  service {
    implementation = com.example.Container
    parameters {
      dep        = <modules.this.abstract-service>
      fieldParam = <modules.this.abstract-service>
      listParam  = [<modules.this.abstract-service>]
    }
  }

  default-impl {
    implementation = com.example.DefaultImpl
  }
}
```

#### Selecting an implementation in application.conf

```hocon
# application.conf
boot.main = my-module

services {
  my-module.abstract-service = <modules.my-module.default-impl>
}
```

#### Test-time mock

Create a test module that `dependsOn` the production module, disable the default implementation, and point the abstract service at the mock:

```hocon
# test-module.oap
name = my-module-test
dependsOn = my-module

services {
  mock {
    implementation = com.example.MockImpl
  }
}
```

```hocon
# application-test.conf
boot.main = my-module-test

services {
  my-module {
    default-impl.enabled   = false
    abstract-service       = <modules.my-module-test.mock>
  }
}
```

#### Error cases

- `abstract = true` not set but `implementation` is an interface → `ApplicationException: "abstract = true" property is missing`
- No concrete implementation registered and no default → `ApplicationException: No implementation has been declared`
- Implementations exist but none selected → `ApplicationException: No implementation specified ... Available implementations [...]`

---

### Module Discovery

`Module.CONFIGURATION.urlsFromClassPath()` scans all jars on the classpath for module descriptors in priority order:

1. `META-INF/oap-module.oap`
2. `META-INF/oap-module.conf`
3. `META-INF/oap-module.yaml` / `.yml`
4. `META-INF/oap-module.json`

All discovered modules are loaded. Only modules reachable via `boot.main`'s transitive `dependsOn` graph are activated.

Module and service names must match the pattern `^[A-Za-z\-_0-9]++$`.

#### boot.main transitive activation

With `boot.main = [m1]` and the graph `m1 → m3 → m4`, modules m1, m3, and m4 are activated. Module m2 (if unreachable from m1) is silently ignored.

#### oap-module-ext.conf

Attach typed metadata to service declarations by registering an extension type:

```
# META-INF/oap-module-ext.conf
services.ws.implementation = com.example.WsServiceExt
```

Then use the extension key freely in any module's service block (see [KernelExt](#kernelext-service-metadata-extensions)).

---

### Kernel API

#### Construction

```java
// Use all module descriptors from the classpath
Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() );

// Named kernel (name appears in logs)
Kernel kernel = new Kernel( "my-app", Module.CONFIGURATION.urlsFromClassPath() );
```

#### Starting

```java
kernel.start( Path.of( "/etc/myapp/application.conf" ) );
kernel.start( Path.of( "/etc/myapp/application.conf" ), Path.of( "/etc/myapp/conf.d" ) );
kernel.start( "classpath:application.conf", "conf.d" );
kernel.start( Map.of( "boot.main", "my-module" ) );   // programmatic (tests)
kernel.start( applicationConfiguration );              // pre-built config object
```

#### Service lookup

```java
// Exact module + name
Optional<MyService> s = kernel.service( "my-module", "my-service" );

// By reference string
Optional<MyService> s = kernel.service( "my-module.my-service" );
Optional<MyService> s = kernel.service( "<modules.my-module.my-service>" );

// All services with a given name across all modules (use "*" for any module)
List<MyService> list = kernel.services( "*", "my-service" );

// By class
List<MyService>         all   = kernel.ofClass( MyService.class );
Optional<MyService>     first = kernel.serviceOfClass( MyService.class );
MyService               req   = kernel.serviceOfClass2( MyService.class );  // throws if not found

// Scoped to a module
List<MyService> list = kernel.ofClass( "my-module", MyService.class );

// By extension key (see KernelExt section)
List<ServiceExt<WsConfig>> wsServices = kernel.servicesByExt( "ws" );
```

#### Stopping

```java
kernel.stop();

// Kernel implements Closeable — use try-with-resources in tests
try ( Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() ) ) {
    kernel.start( Map.of( "boot.main", "my-module" ) );
    // assertions
}
```

---

### KernelExt: Service Metadata Extensions

Arbitrary typed metadata can be attached to service declarations and queried at runtime. This is the mechanism used by `oap-ws` to discover HTTP-annotated services without scanning all services by type.

#### Registration

Add a line to `META-INF/oap-module-ext.conf` mapping the extension key to a Java class:

```
services.ws.implementation = com.example.WsServiceExt
```

`WsServiceExt` is a plain POJO that the HOCON binder will populate.

#### Usage in module file

```hocon
services {
  my-api {
    implementation = com.example.MyApi
    ws {
      path = /api/v1
      port = httpprivate
    }
  }
}
```

#### Querying at runtime

```java
List<ServiceExt<WsServiceExt>> endpoints = kernel.servicesByExt( "ws" );
for ( ServiceExt<WsServiceExt> ep : endpoints ) {
    System.out.println( ep.name + " → " + ep.ext.path );
}
```

---

### Testing with KernelFixture

`KernelFixture` (from `oap-application-test`) is a TestNG fixture that starts a real `Kernel` before each test method and stops it after.

#### Variables automatically available in application.conf

| Variable | Value |
|---|---|
| `TEST_HTTP_PORT` | A free HTTP port allocated for the test |
| `TEST_DIRECTORY` | A per-test temp directory |
| `TEST_RESOURCE_PATH` | Path to the test's resource directory |
| `TEST_HTTP_PREFIX` | `http://localhost:${TEST_HTTP_PORT}` |

#### Basic usage

```java
@Listeners( Fixtures.class )
public class MyServiceTest {
    private final TestDirectoryFixture testDirectory = fixture( new TestDirectoryFixture() );
    private final KernelFixture kernel = fixture( new KernelFixture(
        testDirectory,
        Resources.url( MyServiceTest.class, "application.test.conf" ).orElseThrow(),
        List.of( Resources.url( MyServiceTest.class, "oap-module.oap" ).orElseThrow() )
    ) );

    @Test
    public void myTest() {
        MyService svc = kernel.service( "*", MyService.class ).orElseThrow();
        // test assertions
    }
}
```

#### application.test.conf pattern

```hocon
boot.main = my-module

services {
  my-module {
    my-service.parameters.port = ${TEST_HTTP_PORT}
    my-service.parameters.dir  = ${TEST_DIRECTORY}
  }
}
```

#### Fluent builder methods

```java
new KernelFixture( testDir, confUrl )
    .withProperties( Map.of( "MY_KEY", "value" ) )     // inject HOCON substitution vars
    .withConfResource( MyTest.class, "extra.conf" )     // merge extra conf file
    .withConfdResources( MyTest.class, "conf.d" );      // add conf.d directory
```

#### Direct kernel pattern (no fixture)

For tests not using the TestNG fixture machinery:

```java
try ( Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() ) ) {
    kernel.start( Map.of( "boot.main", "my-module" ) );
    MyService svc = kernel.serviceOfClass2( MyService.class );
    // assertions
}
```

---

### Production Boot

`Boot.main` is the production entry point. It creates a `Kernel` from all classpath module descriptors and starts it.

```bash
java -cp <classpath> oap.application.Boot start \
  --config /etc/myapp/application.conf \
  --config-directory /etc/myapp/conf.d     # optional; defaults to <config parent>/conf.d
```

`SIGINT` and `SIGTERM` both trigger a graceful `kernel.stop()` followed by `System.exit(0)`.

`Boot.terminated` is a `public volatile boolean` that becomes `true` when shutdown begins. Useful for polling in application-level shutdown hooks.

---

### Error Reference

| Message | Cause |
|---|---|
| `boot.main must contain at least one module name` | `boot.main` is empty or missing in `application.conf` |
| `<url>: module.name is blank` | A module file has no `name` field |
| `unknown application configuration module: X` | `application.conf` references a module not found on the classpath |
| `unknown application configuration services: M.[S]` | `application.conf` overrides a service that does not exist in module M |
| `main.boot: unknown module name 'X'` | `boot.main` names a module not found on the classpath |
| `module name X does not match ...` | Module name contains characters outside `[A-Za-z\-_0-9]` |
| `service name X does not match ...` | Service name contains illegal characters |
| `failed to initialize service: M:S. implementation == null` | Service block has no `implementation` field |
| `[M:*] dependencies are not enabled` | Module's `dependsOn` target is disabled |
| `[M:S] dependencies are not enabled. Required service [X] is disabled` | A service parameter references a disabled service |
| `cyclic dependency detected` | Module-level dependency cycle |
| `services cyclic dependency detected` | Service-level dependency cycle |
| `No implementation has been declared for the abstract service <M.S>` | `abstract=true` with no concrete impl registered and no `default` |
| `No implementation specified for abstract service <M.S> ... Available implementations [...]` | Concrete impls exist but none was selected in `application.conf` |
| `Service <M.S> has an abstract implementation, but the "abstract = true" property is missing` | Interface/abstract class used without `abstract = true` |
| `Unknown service X in reference <modules.M.X>` | Abstract service `default` or `application.conf` assignment references a non-existent service |
| `M:S Service X is already registered` | Two enabled services in the same module have the same name |
| `for S listening object <ref> is not found` | `listen` reference does not resolve to a known service |
| `listener L should have method addLListener in <ref>` | `listen.L` target has no `addLListener(T)` method |

---

## oap-stdlib

Core utility library for the OAP platform. Provides serialization, reflection, file I/O, collections, concurrency primitives, and identifier abstractions used across all OAP modules.

### Packages

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

### `oap.json.Binder`

Pre-configured Jackson `ObjectMapper` wrappers. All instances are thread-safe singletons.

#### Static instances

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

#### Serialization

```java
// Object → String
String json = Binder.json.marshal( order );
String pretty = Binder.json.marshalWithDefaultPrettyPrinter( order );

// Object → Path (auto-detects encoding from extension)
Binder.json.marshal( Path.of( "/data/order.json.gz" ), order );

// Streaming JSON array to OutputStream
Binder.json.marshal( outputStream, List.of( order1, order2 ) );
```

#### Deserialization

```java
Order order = Binder.json.unmarshal( Order.class, jsonString );
Order order = Binder.json.unmarshal( Order.class, path );
Order order = Binder.json.unmarshal( Order.class, url );
Order order = Binder.json.unmarshal( Order.class, inputStream );

// Generic types — use TypeRef
List<Order> orders = Binder.json.unmarshal( new TypeRef<List<Order>>() {}, jsonString );
```

#### Partial update

```java
// Apply HOCON-format overrides to an existing object (preserves unmentioned fields)
Binder.update( order, Map.of( "status", "SHIPPED" ) );
Binder.update( order, "{ status: SHIPPED, total: 99.99 }" );
```

#### Dynamic config binders

```java
// Parse HOCON with extra fallback properties merged in
Binder b = Binder.hoconWithConfig( Map.of( "host", "localhost", "port", 8080 ) );
MyConfig cfg = b.unmarshal( MyConfig.class, "classpath:config.conf" );
```

#### Jackson customization

Extra Jackson modules are registered by listing them in `META-INF/jackson.modules` (one class name per line). The default configuration:
- Field visibility: `ANY` (no getters required)
- Accepts case-insensitive property names
- Accepts single-quoted strings
- Skips null input values on deserialization
- Omits null fields on serialization
- Joda-Time, JDK8, JavaTime modules registered

---

### `oap.reflect.TypeRef<T>`

Java generic type token. Use it anywhere a `Class<T>` cannot carry generic parameters.

```java
TypeRef<List<Order>> ref = new TypeRef<List<Order>>() {};
List<Order> orders = Binder.json.unmarshal( ref, json );

// Also accepted by Reflect
Reflection r = Reflect.reflect( ref );
```

---

### `oap.reflect.Reflect` / `Reflection`

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

#### Optional constructor parameters

A constructor parameter can be omitted from the `args` map passed to `newInstance` if it is annotated `@javax.annotation.Nullable` or `@com.fasterxml.jackson.annotation.JsonProperty(required = false)` — it is then passed as `null`. All other parameters must still be present as keys in `args`.

```java
public Order( String id, @Nullable String note ) { ... }

Order o = r.newInstance( Map.of( "id", "o-1" ) ); // note == null, no exception
```

#### Parameter aliasing via `@JsonProperty`

`@JsonProperty("xxx")` (or `@JsonProperty(value = "xxx")`) renames the lookup key for a constructor parameter — `newInstance` reads it from `args.get("xxx")` instead of the Java parameter name.

Note: `JsonProperty.required()` defaults to `false`, so a plain `@JsonProperty("xxx")` also makes the parameter optional (see above) — pass `required = true` if the aliased parameter must still be present in `args`.

```java
public Order( @JsonProperty( value = "order_id", required = true ) String id ) { ... }

Order o = r.newInstance( Map.of( "order_id", "o-1" ) ); // id == "o-1"
```

`@JsonAlias({ "a", "b" })` adds further acceptable lookup keys *on top of* the parameter's existing name/`@JsonProperty` value — `newInstance` accepts args under any of them:

```java
public Order( @JsonAlias( { "order_id", "orderId" } ) String id ) { ... }

r.newInstance( Map.of( "id", "o-1" ) );       // still matches (Java parameter name)
r.newInstance( Map.of( "order_id", "o-1" ) ); // also matches
r.newInstance( Map.of( "orderId", "o-1" ) );  // also matches
```

---

### `oap.id.Identifier<I, T>`

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

### `oap.io.Files`

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

### `oap.io.IoStreams`

Stream I/O with transparent compression support.

#### `Encoding`

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

#### Reading

```java
InputStream in = IoStreams.in( path );
InputStream in = IoStreams.in( path, Encoding.GZIP );

Stream<String> lines = IoStreams.lines( path );
Stream<String> lines = IoStreams.lines( path, Encoding.GZIP );
Stream<String> lines = IoStreams.lines( url );
Stream<String> lines = IoStreams.lines( inputStream );
```

#### Writing

```java
OutputStream out = IoStreams.out( path );
OutputStream out = IoStreams.out( path, Encoding.GZIP );
OutputStream out = IoStreams.out( path, Encoding.GZIP, /* append */ true );

IoStreams.write( path, Encoding.GZIP, "text content" );
IoStreams.write( path, Encoding.GZIP, inputStream );
IoStreams.write( path, Encoding.PLAIN, lineStream );  // Stream<String>, one line per element
```

---

### `oap.io.Resources`

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

### `oap.io.content.ContentReader` / `ContentWriter`

Typed I/O adapters passed to `Files.read()`, `IoStreams.write()`, and cloud storage APIs.

#### `ContentReader` factories

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

#### `ContentWriter` factories

| Factory | Writes |
|---|---|
| `ContentWriter.ofString()` | String → UTF-8 bytes |
| `ContentWriter.ofBytes()` | `byte[]` pass-through |
| `ContentWriter.ofJson()` | Any object → JSON bytes via `Binder.json` |
| `ContentWriter.ofObject()` | Java serialization (`ObjectOutputStream`) |

---

### `oap.util.Cuid`

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

### `oap.util.Dates`

Joda-Time utilities. All operations use UTC unless otherwise noted.

#### Formatters

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

#### Duration constants (return milliseconds as `long`)

```java
Dates.s( 30 )   // 30 seconds in ms
Dates.m( 5 )    // 5 minutes in ms
Dates.h( 2 )    // 2 hours in ms
Dates.d( 7 )    // 7 days in ms
Dates.w( 2 )    // 2 weeks in ms

String human = Dates.durationToString( Dates.h(1) + Dates.m(30) ); // "1h 30m"
```

#### Controllable clock (for tests)

```java
Dates.setTimeFixed( 2024, 6, 1, 14, 30, 0 );   // freeze at 14:30:00 UTC
Dates.incFixed( Dates.h( 1 ) );                  // advance by 1 hour
DateTimeUtils.setCurrentMillisSystem();           // restore real clock
```

---

### `oap.util.Stream<E>`

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

### `oap.util.Lists` / `Maps` / `Sets` / `Strings`

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

### `oap.util.Result<S, F>`

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

### `oap.net.Inet`

```java
Optional<InetAddress> ip   = Inet.getLocalIp();
String                host = Inet.hostName();
```

---

### `oap.concurrent.Executors`

```java
// Named scheduled thread pool
ScheduledExecutorService exec = Executors.newScheduledThreadPool( 4, "my-service" );

// Named single-thread executor
ExecutorService exec = Executors.newSingleThreadExecutor( "my-worker" );
```

Thread names include the pool name for easy identification in thread dumps and profilers.

---

## oap-http

HTTP server and client infrastructure for the OAP platform. Provides an Undertow-based server with named ports, a high-performance non-blocking pipeline, Prometheus metrics exporters, and test utilities.

### Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-http](oap-http/oap-http/README.md) | `NioHttpServer`, `OapHttpClient`, `HealthHttpHandler`, `HttpServerExchange` | — |
| [oap-pnio-v3](oap-http/oap-pnio-v3/README.md) | High-performance non-blocking pipeline (`PnioHttpHandler`, `PnioExchange`) | `oap-http` |
| [oap-http-prometheus](oap-http/oap-http-prometheus/README.md) | Prometheus scrape endpoint, JVM metrics, application info exporter | `oap-http` |
| [oap-http-test](oap-http/oap-http-test/README.md) | `HttpAsserts`, `HttpServerExchangeStub`, `MockHttpContext` | `oap-http` |

---

### Quick start

**1.** Add `oap-http` to your module's `dependsOn`:

```hocon
name = my-module
dependsOn = [oap-http]
```

**2.** Reference the server and bind a handler:

```hocon
services {
  my-handler {
    implementation = com.example.MyHandler
    parameters {
      server = <modules.oap-http.oap-http-server>
    }
  }
}
```

```java
public class MyHandler implements HttpHandler {
    public MyHandler( NioHttpServer server ) {
        server.bind( "/api/hello", this );
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        exchange.responseBody( "hello" );
    }
}
```

The endpoint is available at `GET http://localhost:8080/api/hello`.

See [oap-http](oap-http/oap-http/README.md) for the full server reference.

---

### Optional add-ons

| Need | Add module |
|---|---|
| High-performance non-blocking pipeline | `oap-pnio-v3` |
| Prometheus metrics scrape endpoint | `oap-http-prometheus` |
| HTTP assertions in tests | `oap-http-test` |

---

## oap-ws

HTTP web service framework for the OAP platform. Provides annotation-driven endpoint declaration, session management, interceptors, validation, OpenAPI generation, SSO/JWT security, and file upload/download — all wired through the OAP Kernel with zero servlet-container boilerplate.

### Sub-modules

| Module                                                    | Description | Depends on |
|-----------------------------------------------------------|---|---|
| [oap-ws](oap-ws/oap-ws/README.md)                                         | Core framework: `@WsMethod`, `@WsParam`, `WebServices`, `SessionManager`, validation | `oap-http` |
| [oap-ws-admin-ws](oap-ws/oap-ws-admin-ws/README.md)                       | Built-in admin endpoints: log level control, JPath queries, JSON schema lookup | `oap-ws` |
| [oap-ws-api-api](oap-ws/oap-ws-api-api/README.md)                         | Shared API descriptor contracts (`Info`, `@OpenapiIgnore`) | `oap-ws` |
| [oap-ws-api-ws](oap-ws/oap-ws-api-ws/README.md)                           | HTTP endpoint that exposes the service registry as JSON (`GET /system/api`) | `oap-ws` |
| [oap-ws-file-ws](oap-ws/oap-ws-file-ws/README.md)                         | File upload and download over HTTP with multi-bucket storage | `oap-ws` |
| [oap-ws-openapi](oap-ws/oap-ws-openapi/README.md)                         | Core OpenAPI 3.x generation library (`OpenapiGenerator`, `WebServicesWalker`) | `oap-ws` |
| [oap-ws-openapi-ws](oap-ws/oap-ws-openapi-ws/README.md)                   | HTTP endpoint that serves the generated OpenAPI spec (`GET /system/openapi`) | `oap-ws`, `oap-ws-api-ws` |
| [oap-ws-openapi-maven-plugin](oap-ws/oap-ws-openapi-maven-plugin/README.md) | Maven plugin to generate `swagger.json` / YAML at build time | — |
| [oap-ws-sso-api](oap-ws/oap-ws-sso-api/README.md)                         | SSO contracts + interceptors: `@WsSecurity`, JWT, API key, throttle-login | — |
| [oap-ws-sso](oap-ws/oap-ws-sso/README.md)                                 | `AbstractSecureWS` base class for secured web services | `oap-ws-sso-api` |
| [oap-ws-test](oap-ws/oap-ws-test/README.md)                               | TestNG assertion helpers for validation errors | `oap-ws` |

---

### Quick start

**1.** Add `oap-ws` to your module's `dependsOn`:

```hocon
name = my-module
dependsOn = [oap-ws]
```

**2.** Annotate your service class and register it with a `ws-service` block:

```java
public class HelloWS {
    @WsMethod( path = "/hello", method = HttpMethod.GET )
    public String hello( @WsParam( from = From.QUERY ) String name ) {
        return "Hello, " + name + "!";
    }
}
```

```hocon
services {
  hello-ws {
    implementation = com.example.HelloWS
    ws-service.path = api
  }
}
```

**3.** The endpoint is available at `GET /api/hello?name=World`.

See the [oap-ws](oap-ws/oap-ws/) module for the full reference.

---

### Optional add-ons

| Need | Add module |
|---|---|
| Runtime API introspection | `oap-ws-api-ws` |
| OpenAPI / Swagger spec served at runtime | `oap-ws-openapi-ws` |
| OpenAPI spec generated at build time | `oap-ws-openapi-maven-plugin` |
| JWT / API-key authentication (query params or headers) | `oap-ws-sso-api` |
| File upload / download | `oap-ws-file-ws` |
| Admin (log levels, JPath) | `oap-ws-admin-ws` |

---

## oap-jpath

JPath expression language for navigating Java objects and maps using reflection. Expressions are parsed by an ANTLR4 grammar and evaluated against a variable map, traversing public and private fields, calling methods, and indexing arrays or lists — all in a single `${…}` expression.

### Expression syntax

Every JPath expression is wrapped in `${…}`. The first segment names a variable from the provided map; subsequent segments are chained with `.`.

```
${variable}
${variable.field}
${variable.field.nestedField}
${variable.method()}
${variable.method("arg", 2)}
${variable.array[0]}
${variable.list[1].field}
```

#### Path segment types

| Form | Example | Resolves via |
|---|---|---|
| `identifier` | `name` | Field access — public or private, via reflection |
| `name(args…)` | `getLabel("x", 2)` | Method call — public or private, via reflection |
| `name[n]` | `items[1]` | Array element or `List.get(n)` |

Segments can be chained freely:

```
${order.lines[0].product.getPrice("USD")}
```

#### Method arguments

Methods accept string literals and decimal integer literals as arguments.

| Literal | Example | Parsed as |
|---|---|---|
| String | `"hello"` | `String` |
| Decimal integer | `42` | Parsed as `Long`, auto-coerced to the target parameter type (`int`, `long`, `float`, `double`, `short`, `byte`) |

### API

#### `JPath.evaluate`

```java
StringBuilderJPathOutput output = new StringBuilderJPathOutput();

JPath.evaluate(
    "${user.address.city}",
    Map.of( "user", user ),
    output
);

String result = output.toString();
```

Static shorthand — builds a `JPath` instance and evaluates in one call. For repeated evaluation against the same variable set, construct a `JPath` instance directly:

```java
JPath jpath = new JPath( Map.of( "user", user ) );

jpath.evaluate( "${user.name}", output );
output.reset();
jpath.evaluate( "${user.email}", output );
```

#### `StringBuilderJPathOutput`

Built-in `JPathOutput` implementation that collects results into a `StringBuilder`.

| Method | Description |
|---|---|
| `toString()` | Returns the accumulated string value |
| `reset()` | Clears the buffer for re-use |

### Examples

```java
// Simple variable lookup
JPath.evaluate( "${id}", Map.of( "id", 42 ), output );
// → "42"

// Nested field access (public field)
JPath.evaluate( "${order.status}", Map.of( "order", order ), output );

// Private field access
JPath.evaluate( "${bean.internalState}", Map.of( "bean", bean ), output );

// Private method call
JPath.evaluate( "${bean.computeScore()}", Map.of( "bean", bean ), output );

// Method with string argument
JPath.evaluate( "${bean.format(\"prefix\")}", Map.of( "bean", bean ), output );

// Method with multiple arguments (string + integer)
JPath.evaluate( "${bean.pad(\"x\", 5)}", Map.of( "bean", bean ), output );

// Array element access
JPath.evaluate( "${data.scores[2]}", Map.of( "data", data ), output );

// List element + field chain
JPath.evaluate( "${order.lines[0].productName}", Map.of( "order", order ), output );

// Chaining Java API calls
JPath.evaluate( "${map.keySet().stream().count()}", Map.of( "map", map ), output );
```

### Custom output

`JPathOutput` is a `@FunctionalInterface`. Implement it to collect typed values without converting to a string:

```java
List<Object> collected = new ArrayList<>();

JPathOutput collector = pointer -> collected.add( pointer.get() );

JPath.evaluate( "${item.price}", Map.of( "item", item ), collector );

BigDecimal price = (BigDecimal) collected.get( 0 );
```

The `Pointer` passed to `write` is one of:

| Implementation | `get()` returns |
|---|---|
| `ObjectPointer<T>` | The resolved object |
| `MapPointer` | The resolved `Map` |
| `NullPointer` | `null` |

### Errors

| Exception | Thrown when |
|---|---|
| `PathNotFoundException` | A field or method named in the expression does not exist on the target object |
| `ReflectionException` | Reflection access fails (e.g., module access denied) |

### See also

- [`JPathWS`](oap-ws/oap-ws-admin-ws/README.md#jpath-query--get-systemadminjpath) — exposes JPath evaluation over the live Kernel service tree as a JSON HTTP endpoint.
- [`InspectorWS`](oap-ws/oap-ws-admin-ws/README.md#inspector-ui--get-systemadmininspector) — browsable HTML UI built on top of the same JPath queries.

---

## oap-formats

Format processing modules for the OAP platform: template engine, TSV/CSV, JSON schema validation, and log streaming.

### Sub-modules

| Module | Description |
|---|---|
| [oap-template](oap-formats/oap-template/README.md) | Compile-time template engine — parses once, compiles to Java, renders at near-native speed |
| [oap-template-test](oap-formats/oap-template-test/README.md) | `TemplateEngineFixture` — TestNG fixture for template engine tests |
| [oap-json](oap-formats/oap-json/README.md) | JSON schema validation (HOCON format) and structural diff |
| [oap-tsv](oap-formats/oap-tsv/README.md) | TSV/CSV parsing, streaming, and printing |
| [oap-logstream](oap-formats/oap-logstream/README.md) | High-throughput transactional log streaming to time-bucketed gzip files |

---

## oap-statsdb

Distributed, in-memory statistics database for the OAP platform. Data is organized as a typed key hierarchy — each level of the tree holds a `Node.Value` that knows how to merge itself with another value of the same type. Parent nodes optionally aggregate over their children after each update.

### Architecture

```
StatsDBNode (process A)          StatsDBNode (process B)
  update("k1","k2", v -> v.n++)    update("k1","k3", v -> v.n++)
  sync() ─────────────────────┐    sync() ────────────────────┐
                               ▼                              ▼
                        StatsDBMaster (in-memory tree)
                          k1 → MockChild (aggregate)
                            k2 → MockValue
                            k3 → MockValue
                          ▼ (periodically)
                        StatsDBStorage (MongoDB / NULL)
```

In a single-process deployment, use `StatsDBMaster` directly without a `StatsDBNode`.

### Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-statsdb-common](oap-statsdb/oap-statsdb-common/README.md) | Core: `Node.Value`, `Node.Container`, `NodeSchema`, `StatsDB` API | — |
| [oap-statsdb-master](oap-statsdb/oap-statsdb-master/README.md) | `StatsDBMaster`, `StatsDBStorage`, MongoDB persistence, message listener | `oap-statsdb-common` |
| [oap-statsdb-node](oap-statsdb/oap-statsdb-node/README.md) | `StatsDBNode`, `StatsDBTransport`, message-based sync transport | `oap-statsdb-common` |
| [oap-statsdb-test](oap-statsdb/oap-statsdb-test/README.md) | `StatsDBTransportMock` for integration tests | `oap-statsdb-master`, `oap-statsdb-node` |

---

### Data model

#### `Node.Value<T>`

The value stored at each tree node. Must implement `merge(T other)` — called when a sync from a remote node arrives — and `Serializable`.

```java
public class Counters implements Node.Value<Counters> {
    public long requests;
    public long errors;

    @Override
    public Counters merge( Counters other ) {
        requests += other.requests;
        errors   += other.errors;
        return this;
    }
}
```

#### `Node.Container<T, TChild>`

A value at an intermediate tree level that rolls up metrics from its children. `aggregate(List<TChild>)` is called automatically after every update on any descendant.

```java
public class RollupCounters implements Node.Container<RollupCounters, Counters> {
    public long totalRequests;

    @Override
    public RollupCounters merge( RollupCounters other ) {
        // merge is additive — called when syncing from remote nodes
        return this;
    }

    @Override
    public RollupCounters aggregate( List<Counters> children ) {
        totalRequests = children.stream().mapToLong( c -> c.requests ).sum();
        return this;
    }
}
```

Mark computed fields `@JsonIgnore` if they should not be persisted (they are re-derived from children on load).

#### `NodeSchema`

Declares the `Node.Value` class at each key level, ordered from root to leaf.

```java
NodeSchema schema = new NodeSchema(
    nc( "endpoint",  RollupCounters.class ),  // level 0 — root
    nc( "date",      Counters.class )          // level 1 — leaf
);
```

`nc(String key, Class<T>)` is a static factory on `NodeSchema`.

Register value classes in `oap-module.oap` so the JSON binder can deserialize them:

```hocon
configurations = [
  {
    loader = oap.json.TypeIdFactory
    config {
      counters         = com.example.Counters
      rollup-counters  = com.example.RollupCounters
    }
  }
]
```

---

### `StatsDB` API

All update and query methods are available on both `StatsDBMaster` and `StatsDBNode`.

#### Writing

```java
// 1-key update (leaf at level 0)
db.<Counters>update( "endpoint-a", v -> v.requests++ );

// 2-key update (leaf at level 1)
db.<Counters>update( "endpoint-a", "2024-06-01", v -> {
    v.requests++;
    v.errors++;
} );

// Up to 5 keys supported
db.<Counters>update( k1, k2, k3, k4, k5, v -> v.requests++ );
```

#### Reading

```java
// Get value at a path (returns null if not present)
Counters c = db.get( "endpoint-a", "2024-06-01" );

// Get all child values under a prefix
Stream<Counters> daily = db.children( "endpoint-a" );
```

#### Typed select streams

Use `select2()` … `select5()` to stream over the full tree with typed key-value tuples:

```java
// 2-level tree: (id1, v1) → (id2, v2)
db.<RollupCounters, Counters>select2().forEach( row -> {
    System.out.println( row.id1 + " " + row.id2 + " requests=" + row.v2.requests );
} );

// 3-level tree
db.<T1, T2, T3>select3().forEach( row -> { … } );
// also select4(), select5()
```

| Method | Fields |
|---|---|
| `select2()` | `id1, v1, id2, v2` |
| `select3()` | `id1, v1, id2, v2, id3, v3` |
| `select4()` | `id1, v1, id2, v2, id3, v3, id4, v4` |
| `select5()` | `id1, v1, id2, v2, id3, v3, id4, v4, id5, v5` |

#### Clearing

```java
db.removeAll();  // clears in-memory state only
```

---

### Quick start — single process

```java
NodeSchema schema = new NodeSchema(
    nc( "endpoint", RollupCounters.class ),
    nc( "date",     Counters.class )
);

try( StatsDBMaster master = new StatsDBMaster( schema, StatsDBStorage.NULL ) ) {
    master.<Counters>update( "search", "2024-06-01", v -> v.requests += 5 );
    master.<Counters>update( "search", "2024-06-02", v -> v.requests += 3 );

    // Roll-up is automatic
    assertThat( master.<RollupCounters>get( "search" ).totalRequests ).isEqualTo( 8 );
}
```

---

## oap-message

Reliable, durable HTTP message delivery for the OAP platform. The sender buffers messages to disk when the network is unavailable and retries until acknowledged. The server deduplicates by MD5 so retries are always safe to replay.

### Architecture

```
MessageSender (client process)
  send(type, data)
    │
    ├─ in-memory queue ──► syncMemory() ──► POST /messages ──► MessageHttpHandler
    │                                                                │
    └─ disk (on shutdown/failure)                                   ├─ MD5 dedup (MessageHashStorage)
         syncDisk() reloads on restart                              │
                                                                    └─ MessageListener.run(...)
                                                                         → short status
```

### Wire protocol

#### Request (client → server)

| Field | Type | Description |
|---|---|---|
| message type | `byte` | User-defined type identifier (0–200) |
| version | `short` | Message schema version |
| client ID | `long` | Unique sender ID (per `MessageSender` instance) |
| MD5 | `byte[16]` | MD5 digest of the payload |
| reserved | `byte[8]` | Reserved, always zero |
| data size | `int` | Payload length in bytes |
| payload | `byte[N]` | Message body |

#### Response (server → client)

| Field | Type | Description |
|---|---|---|
| protocol version | `byte` | Always `1` |
| client ID | `long` | Echoed from request |
| MD5 | `byte[16]` | Echoed from request |
| reserved | `byte[8]` | Reserved |
| status | `short` | See status codes below |

### Status codes

| Constant | Value | Meaning |
|---|---|---|
| `STATUS_OK` | `0` | Processed successfully |
| `STATUS_UNKNOWN_ERROR` | `1` | Processing failed — client will retry |
| `STATUS_UNKNOWN_ERROR_NO_RETRY` | `2` | Processing failed — client drops the message |
| `STATUS_UNKNOWN_MESSAGE_TYPE` | `100` | No listener registered for this type — client drops the message |
| `STATUS_ALREADY_WRITTEN` | `101` | Duplicate — server already processed this MD5; treated as success by the client |

Custom status codes (causing retry) can be registered in `META-INF/oap-messages.properties` using the `map.*` prefix — see [oap-message-server](oap-message/oap-message-server/README.md).

### Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-message-client](oap-message/oap-message-client/README.md) | `MessageSender` — durable send queue with disk persistence | `oap-http` |
| [oap-message-server](oap-message/oap-message-server/README.md) | `MessageHttpHandler`, `MessageListener`, `MessageListenerJson` | `oap-http` |
| [oap-message-test](oap-message/oap-message-test/README.md) | `MessageListenerMock`, `MessageListenerJsonMock`, `MessageSenderUtils` | `oap-message-client`, `oap-message-server` |

---

## oap-storage

Persistent, in-memory storage layer for the OAP platform. Objects are kept in a `ConcurrentHashMap`-backed `MemoryStorage` and optionally synced to MongoDB or cloud object stores.

### Architecture

```
                  ┌─────────────────────────────┐
                  │      MemoryStorage<Id,Data>  │
                  │  (ConcurrentHashMap + Lock)  │
                  └──────────┬──────────────────┘
                             │  TransactionLog (change log)
              ┌──────────────┴──────────────────┐
              │                                 │
   MongoPersistence<I,T>          ReplicationMaster / RemoteStorage
   (periodic bulk write,          (diff-based replication
    change stream watch)           between nodes)
```

Cloud storage (`FileSystem`) is a separate, stateless API over object stores — it does not integrate with `MemoryStorage`.

### Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-storage](oap-storage/oap-storage/README.md) | `Storage<Id,Data>`, `MemoryStorage`, `Metadata`, `DataListener`, `Migration` | `oap-stdlib` |
| [oap-storage-mongo](oap-storage/oap-storage-mongo/README.md) | `MongoPersistence`, `MongoClient`, `MongoIndex`, `Version` | `oap-storage` |
| [oap-storage-cloud](oap-storage/oap-storage-cloud/README.md) | `FileSystem`, `CloudURI`, `FileSystemConfiguration`, `FileSystemCloudApi` | `oap-stdlib` |
| [oap-storage-cloud-aws-s3](oap-storage/oap-storage-cloud-aws-s3/) | AWS S3 backend (`s3://` scheme) | `oap-storage-cloud` |
| [oap-storage-mongo-test](oap-storage/oap-storage-mongo-test/README.md) | `MongoFixture` — in-memory MongoDB for tests | `oap-storage-mongo` |
| [oap-storage-cloud-test](oap-storage/oap-storage-cloud-test/README.md) | `S3MockFixture` — LocalStack S3 for tests | `oap-storage-cloud-aws-s3` |

### Quick start

```java
// Define an identifier — extracts/assigns the String key from your object
Identifier<String, MyData> id = Identifier.forId( d -> d.id, ( d, newId ) -> d.id = newId )
    .suggestion( d -> d.name )
    .build();

// In-memory store, concurrent reads and writes
MemoryStorage<String, MyData> storage = new MemoryStorage<>( id, Lock.CONCURRENT );

// Store
storage.store( new MyData( "item-1", "hello" ), "system" );

// Read
Optional<MyData> found = storage.getNullable( "item-1" );

// Update in place
storage.update( "item-1", d -> { d.name = "world"; return d; } );

// Listen to changes
storage.addDataListener( new Storage.DataListener<String, MyData>() {
    @Override
    public void updated( IdObject<String, MyData> previous, IdObject<String, MyData> updated ) {
        System.out.println( "changed: " + updated.id );
    }
} );
```

---

## oap-highload

CPU affinity utility for the OAP platform. Pins the calling thread to a specific CPU core via `net.openhft.affinity`, reducing cross-core cache misses in high-throughput loops (network I/O, encoding, scheduling).

### `Affinity`

A plain utility class — not a managed OAP service. Instantiate it directly wherever you need to control thread placement.

#### CPU set syntax

The constructor accepts a string that describes which CPU cores to use:

| Expression | Meaning | Example → CPUs |
|---|---|---|
| `*` | No affinity (disabled) | `*` → `[]` |
| `n` | Single core | `3` → `[3]` |
| `n-m` | Inclusive range | `1-3` → `[1, 2, 3]` |
| `n+` | Core `n` through the last available | `4+` on 8-core → `[4, 5, 6, 7]` |
| Comma-separated | Combine any of the above | `1-3, 8` → `[1, 2, 3, 8]` |

```java
Affinity affinity = new Affinity( "2-5" );   // cores 2, 3, 4, 5
Affinity affinity = new Affinity( "0+" );    // all cores from 0 upward
Affinity affinity = new Affinity( "*" );     // disabled — no pinning
Affinity affinity = Affinity.any();          // same as "*"
```

#### API

| Method | Description |
|---|---|
| `set()` | Pin the calling thread to the next core in the set (round-robin); no-op when disabled |
| `isEnabled()` | `false` when constructed with `*`; `true` otherwise |
| `size()` | Number of CPU cores in the configured set |
| `getCpus()` | Raw `int[]` of configured core indices |

#### Usage

Call `set()` once per thread at startup, or at the top of a processing loop when you want round-robin distribution across the configured cores:

```java
Affinity affinity = new Affinity( "4+" );   // dedicate upper cores to this pool

ExecutorService pool = Executors.newFixedThreadPool( affinity.size(), r -> {
    Thread t = new Thread( () -> {
        affinity.set();   // pin this thread before doing any work
        r.run();
    } );
    return t;
} );
```

When `isEnabled()` is `false` (e.g. `*` in config), `set()` is a no-op and the JVM scheduler assigns cores freely — no code path changes needed.

---

## oap-mail

Email delivery for the OAP platform. Provides a persistent delivery queue, Velocity-based message templates, and swappable transports (SMTP, SendGrid).

### Architecture

```
Template → Message → Mailman → MailQueue → Transport
                                    ↕
                            MailQueuePersistence
                         (file / memory / MongoDB)
```

`Mailman` runs as a supervised background thread that drains `MailQueue`. Failed messages are retried on a configurable schedule; messages that remain broken past `brokenMessageTTL` are dropped.

### Sub-modules

| Module | Description |
|---|---|
| [oap-mail](oap-mail/oap-mail/README.md) | Core: `Message`, `Mailman`, `MailQueue`, `SmtpTransport`, `Template` |
| [oap-mail-sendgrid](oap-mail/oap-mail-sendgrid/README.md) | SendGrid REST API transport |
| [oap-mail-mongo](oap-mail/oap-mail-mongo/README.md) | MongoDB-backed queue persistence |
| [oap-mail-test](oap-mail/oap-mail-test/README.md) | `TransportMock`, `MessageAssertion`, `MessagesAssertion`, `MailBox` |

---

## oap-maven-plugin

Build-time code generation and packaging utilities for OAP projects. All goals share the prefix `oap`.

### Goals

| Goal | Module | Phase | Description |
|---|---|---|---|
| [`oap:generate`](oap-maven-plugin/oap-dictionary-maven/README.md) | `oap-dictionary-maven` | `generate-sources` | Generate Java enums from dictionary JSON/HOCON files |
| [`oap:startup-scripts`](oap-maven-plugin/oap-application-maven/README.md) | `oap-application-maven` | `prepare-package` | Generate OS service scripts (systemd, sysvinit, shell) |
| [`oap:copy`](oap-maven-plugin/oap-maven/README.md) | `oap-maven` | `prepare-package` | Copy file sets into a directory with optional property filtering |
