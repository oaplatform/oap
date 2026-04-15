# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the entire project
mvn clean install

# Build and skip tests
mvn clean install -DskipTests

# Build a single module and its dependencies
mvn -pl oap-ws/oap-ws -am install

# Run all tests in a module
mvn -pl oap-ws/oap-ws test

# Run a single test class
mvn -pl oap-ws/oap-ws test -Dtest=WebServicesTest

# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates
```

Tests use **TestNG**. The Maven repository is `https://maven.xenoss.net/repository/oap-maven/`.

## Architecture Overview

OAP is a lightweight DI/IoC framework inspired by Erlang/OTP for building distributed Java applications. The core abstraction is the **Kernel** — a service container that wires components declared in `oap-module.oap` HOCON files.

### Module System

Each Maven submodule exposes services via `src/main/resources/META-INF/oap-module.oap`:

```hocon
name = my-module
dependsOn = [oap-http]

services {
  my-service {
    implementation = com.example.MyService
    parameters {
      port = 8080
      server = <modules.oap-http.oap-http-server>  # cross-module reference
      kernel = <kernel.self>                         # kernel self-reference
    }
    supervision.supervise = true  # start/stop managed by Kernel
  }
}
```

Application startup is driven by `application.conf` (HOCON), which names the root modules:

```hocon
boot.main = my-module

services {
  my-module.my-service.parameters.port = 9090  # override per-environment
}
```

Service references use the syntax `<modules.{module-name}.{service-name}>`. Within the same module use `<modules.this.{service-name}>`. Abstract/interface services can be declared with `abstract = true` and resolved at runtime via `application.conf`.

Services implementing `Runnable` can be scheduled via `supervision.schedule = true` with `cron` or `delay` parameters. Services implementing `Closeable` are shut down by the Kernel.

### Key Modules

| Module                     | Role                                                                                      |
|----------------------------|-------------------------------------------------------------------------------------------|
| `oap-application`          | `Kernel` (IoC container), `ApplicationConfiguration`, service lifecycle                   |
| `oap-stdlib`               | Core utilities, JSON/HOCON binder, reflection, collections                                |
| `oap-stdlib-test`          | `KernelFixture`, `TestDirectoryFixture`, `Asserts`, `Ports` — test infrastructure         |
| `oap-http`                 | Undertow-based `NioHttpServer` (port 8080 http, 8081 httpprivate by default)              |
| `oap-http/oap-pnio-v3`     | High-performance non-blocking pipeline handler (`PnioExchange`, `PnioHttpHandler`)        |
| `oap-ws`                   | Annotation-driven web services (`@WsMethod`, `@WsParam`), `WebServices`, `SessionManager` |
| `oap-formats/oap-template` | Template engine                                                                           |
| `oap-formats`              | JSON, TSV, template engine, logstream (transactional file writing)                        |
| `oap-storage`              | Persistence: `oap-storage-mongo` (MongoDB), `oap-storage-cloud` (S3)                      |
| `oap-message`              | Binary messaging protocol (client/server)                                                 |
| `oap-statsdb`              | Statistics database                                                                       |
| `oap-mail`                 | Email via SMTP and SendGrid                                                               |
| `oap-jpath`                | JPath expression language for JSON/object navigation                                      |
| `oap-maven-plugin`         | Build-time code generation                                                                |

### Web Services (`oap-ws`)

Services are registered in `oap-module.oap` with a `ws-service` block:

```hocon
my-ws {
  implementation = com.example.MyWS
  ws-service {
    path = api/v1/resource
    port = httpprivate      # optional, defaults to main HTTP port
    sessionAware = true     # optional
    interceptors = [...]    # optional
  }
}
```

Endpoints are annotated Java methods:

```java
@WsMethod(path = "/items/{id}", method = HttpMethod.GET)
public Response getItem(@WsParam(from = From.PATH) String id) { /** code **/ }
```

`@WsParam` sources: `QUERY`, `PATH`, `BODY`, `HEADER`, `SESSION`, `COOKIE`.

### HTTP Server

`oap-http-server` (port 8080) serves public traffic. `httpprivate` (port 8081) serves admin/health endpoints (`/healtz`). Configuration in `oap-http`'s `oap-module.oap`.

### PNIO (`oap-http/oap-pnio-v3`)

`PnioExchange` is the high-performance request processing pipeline. Tasks are chained as `ComputeTask` (blocking) or `AsyncTask` (non-blocking). State flows through `ProcessState` (RUNNING → DONE/TIMEOUT/EXCEPTION/etc.).

### Testing

Integration tests use `KernelFixture` (from `oap-application-test`) to boot a real Kernel from test `application.conf` and `oap-module.oap` files:

```java
@Listeners(Fixtures.class)
public class MyTest {
    @Fixture
    private final KernelFixture kernel = new KernelFixture(
        testDirectoryFixture,
        Resources.url(MyTest.class, "application.conf").orElseThrow()
    );

    @Test
    public void test() {
        MyService svc = kernel.service("my-module", "my-service");
    }
}
```

Available test variables injected by `AbstractKernelFixture`: `TEST_HTTP_PORT`, `TEST_DIRECTORY`, `TEST_RESOURCE_PATH`, `TEST_HTTP_PREFIX`.
