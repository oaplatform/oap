# oap-application

The IoC/DI kernel for the OAP framework. `Kernel` discovers service descriptors from every jar on the classpath, builds a dependency graph, instantiates and wires services, and manages their full lifecycle (start, scheduled runs, stop).

Services are plain Java classes — no framework annotations required. Everything is declared in HOCON files.

## Table of Contents

- [Overview](#overview)
- [Module Declaration (oap-module.oap)](#module-declaration-oap-moduleoap)
- [Application Configuration (application.conf)](#application-configuration-applicationconf)
- [Reference Syntax](#reference-syntax)
- [Supervision and Service Lifecycle](#supervision-and-service-lifecycle)
- [Dependency Injection Mechanics](#dependency-injection-mechanics)
- [Abstract Services](#abstract-services)
- [Module Discovery](#module-discovery)
- [Kernel API](#kernel-api)
- [KernelExt: Service Metadata Extensions](#kernelext-service-metadata-extensions)
- [Testing with KernelFixture](#testing-with-kernelfixture)
- [Production Boot](#production-boot)
- [Error Reference](#error-reference)

---

## Overview

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

## Module Declaration (oap-module.oap)

Every OAP jar ships a descriptor at `src/main/resources/META-INF/oap-module.oap`.

### Module fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | String | required | Unique module identifier; must match `[A-Za-z\-_0-9]+` |
| `enabled` | boolean | `true` | Disable the entire module and all its services |
| `dependsOn` | list\<String\> | `[]` | Module-level ordering: this module starts after listed modules |
| `services` | map | required | Map of service name → service block |

### Service fields

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

### Example: two-module setup

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

## Application Configuration (application.conf)

`application.conf` selects which modules activate and overrides their parameters at deployment time.

### Schema

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

### conf.d/ directory

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

### CONFIG.* environment variable overrides

Any environment variable starting with `CONFIG.` is stripped of the prefix and injected as a HOCON key. This allows per-deployment overrides without modifying config files.

```bash
export CONFIG.services.my-module.my-service.enabled=false
export CONFIG.services.my-module.my-service.parameters.val='"hello"'
```

### Programmatic startup (tests / embedded)

```java
kernel.start( Map.of(
    "boot.main", "m1",
    "services.m1.ServiceOneP1.parameters.i", "50"
) );
```

---

## Reference Syntax

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

## Supervision and Service Lifecycle

The `supervision` block controls how the kernel starts, runs, and stops a service.

### Fields

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

### Lifecycle examples

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

### Shutdown sequence

On `kernel.stop()`, the `Supervisor` stops services in reverse registration order:
1. Threads and scheduled tasks are interrupted/cancelled.
2. Supervised services have `preStop()` then `stop()` (or `close()`) called.

`shutdown.serviceTimeout` (default `5s`) is the warn threshold per service. Set `shutdown.serviceAsyncShutdownAfterTimeout = true` to continue shutdown after a timeout rather than waiting.

---

## Dependency Injection Mechanics

### Constructor injection

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

### Field injection

Parameters not consumed by the constructor are applied to public fields by name.

### Nested object parameters

Maps in `parameters` are bound to nested objects:

```hocon
parameters.complex {
  i = 2
  map.a.i = 1
}
```

### Listen wiring

`listen.name = <ref>` — after construction, calls `ref.addNameListener(this)`. The target service must have a method `addNameListener(T listener)`.

```hocon
ServiceTwo {
  listen.some = <modules.m1.ServiceOneP1>   # calls ServiceOneP1.addSomeListener(serviceTwo)
}
```

### Link wiring (reverse injection)

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

### Disabled services

A disabled service referenced via `<modules...>` resolves to `null` and is omitted from list parameters. No error is thrown.

### Cyclic dependencies

Cyclic module or service dependencies are detected at startup and throw `ApplicationException("cyclic dependency detected")`.

---

## Abstract Services

The abstract service pattern defines an interface slot in a module that must be filled with a concrete implementation — either by a `default` fallback or by an explicit assignment in `application.conf`.

### Declaring an abstract service

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

### Selecting an implementation in application.conf

```hocon
# application.conf
boot.main = my-module

services {
  my-module.abstract-service = <modules.my-module.default-impl>
}
```

### Test-time mock

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

### Error cases

- `abstract = true` not set but `implementation` is an interface → `ApplicationException: "abstract = true" property is missing`
- No concrete implementation registered and no default → `ApplicationException: No implementation has been declared`
- Implementations exist but none selected → `ApplicationException: No implementation specified ... Available implementations [...]`

---

## Module Discovery

`Module.CONFIGURATION.urlsFromClassPath()` scans all jars on the classpath for module descriptors in priority order:

1. `META-INF/oap-module.oap`
2. `META-INF/oap-module.conf`
3. `META-INF/oap-module.yaml` / `.yml`
4. `META-INF/oap-module.json`

All discovered modules are loaded. Only modules reachable via `boot.main`'s transitive `dependsOn` graph are activated.

Module and service names must match the pattern `^[A-Za-z\-_0-9]++$`.

### boot.main transitive activation

With `boot.main = [m1]` and the graph `m1 → m3 → m4`, modules m1, m3, and m4 are activated. Module m2 (if unreachable from m1) is silently ignored.

### oap-module-ext.conf

Attach typed metadata to service declarations by registering an extension type:

```
# META-INF/oap-module-ext.conf
services.ws.implementation = com.example.WsServiceExt
```

Then use the extension key freely in any module's service block (see [KernelExt](#kernelext-service-metadata-extensions)).

---

## Kernel API

### Construction

```java
// Use all module descriptors from the classpath
Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() );

// Named kernel (name appears in logs)
Kernel kernel = new Kernel( "my-app", Module.CONFIGURATION.urlsFromClassPath() );
```

### Starting

```java
kernel.start( Path.of( "/etc/myapp/application.conf" ) );
kernel.start( Path.of( "/etc/myapp/application.conf" ), Path.of( "/etc/myapp/conf.d" ) );
kernel.start( "classpath:application.conf", "conf.d" );
kernel.start( Map.of( "boot.main", "my-module" ) );   // programmatic (tests)
kernel.start( applicationConfiguration );              // pre-built config object
```

### Service lookup

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

### Stopping

```java
kernel.stop();

// Kernel implements Closeable — use try-with-resources in tests
try ( Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() ) ) {
    kernel.start( Map.of( "boot.main", "my-module" ) );
    // assertions
}
```

---

## KernelExt: Service Metadata Extensions

Arbitrary typed metadata can be attached to service declarations and queried at runtime. This is the mechanism used by `oap-ws` to discover HTTP-annotated services without scanning all services by type.

### Registration

Add a line to `META-INF/oap-module-ext.conf` mapping the extension key to a Java class:

```
services.ws.implementation = com.example.WsServiceExt
```

`WsServiceExt` is a plain POJO that the HOCON binder will populate.

### Usage in module file

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

### Querying at runtime

```java
List<ServiceExt<WsServiceExt>> endpoints = kernel.servicesByExt( "ws" );
for ( ServiceExt<WsServiceExt> ep : endpoints ) {
    System.out.println( ep.name + " → " + ep.ext.path );
}
```

---

## Testing with KernelFixture

`KernelFixture` (from `oap-application-test`) is a TestNG fixture that starts a real `Kernel` before each test method and stops it after.

### Variables automatically available in application.conf

| Variable | Value |
|---|---|
| `TEST_HTTP_PORT` | A free HTTP port allocated for the test |
| `TEST_DIRECTORY` | A per-test temp directory |
| `TEST_RESOURCE_PATH` | Path to the test's resource directory |
| `TEST_HTTP_PREFIX` | `http://localhost:${TEST_HTTP_PORT}` |

### Basic usage

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

### application.test.conf pattern

```hocon
boot.main = my-module

services {
  my-module {
    my-service.parameters.port = ${TEST_HTTP_PORT}
    my-service.parameters.dir  = ${TEST_DIRECTORY}
  }
}
```

### Fluent builder methods

```java
new KernelFixture( testDir, confUrl )
    .withProperties( Map.of( "MY_KEY", "value" ) )     // inject HOCON substitution vars
    .withConfResource( MyTest.class, "extra.conf" )     // merge extra conf file
    .withConfdResources( MyTest.class, "conf.d" );      // add conf.d directory
```

### Direct kernel pattern (no fixture)

For tests not using the TestNG fixture machinery:

```java
try ( Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() ) ) {
    kernel.start( Map.of( "boot.main", "my-module" ) );
    MyService svc = kernel.serviceOfClass2( MyService.class );
    // assertions
}
```

---

## Production Boot

`Boot.main` is the production entry point. It creates a `Kernel` from all classpath module descriptors and starts it.

```bash
java -cp <classpath> oap.application.Boot start \
  --config /etc/myapp/application.conf \
  --config-directory /etc/myapp/conf.d     # optional; defaults to <config parent>/conf.d
```

`SIGINT` and `SIGTERM` both trigger a graceful `kernel.stop()` followed by `System.exit(0)`.

`Boot.terminated` is a `public volatile boolean` that becomes `true` when shutdown begins. Useful for polling in application-level shutdown hooks.

---

## Error Reference

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
