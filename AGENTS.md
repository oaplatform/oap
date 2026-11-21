# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Build Commands

```bash
# Build the entire project
mvn install

# Build and skip tests
mvn install -DskipTests

# Build a single module
mvn install -pl oap-ws/oap-ws

# Publish to repository
mvn deploy
```

Tests use **TestNG**. The Maven parent POM is `java-convention`. The repository is `https://maven.xenoss.net/repository/oap-maven/`.

**Run tests via `intellij*` MCP tools** (e.g. `mcp__intellij__execute_run_configuration`), not `mvn test`.

## Code Search

Use `intellij* mcp` refactoring tools for renames and moves of code symbols, for class, method, variables search

## Tech Stack

- **Language:** Java
- **Build:** Maven
- **Tests:** TestNG
- **Checkstyle:** https://raw.githubusercontent.com/oaplatform/oap-maven/master/.idea/checkstyle.xml

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

One Maven submodule can declare several oap modules: besides `META-INF/oap-module.oap`, every `META-INF/oap-module-<suffix>.oap` file is loaded as a separate module (e.g. `oap-module-services.oap` with `name = services`, `dependsOn = [my-module]`). `oap-module-ext.*` is reserved for kernel extensions.

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
| `oap-maven-plugin`         | Maven plugins: `oap.copy`, `oap.startup-scripts`, `oap.dictionary` — build-time code generation and packaging |
| `oap-mcp`                  | `McpService` (SSE transport provider), `McpWS` (secured HTTP endpoint), `@McpTool`/`@McpPrompt`/`@McpToolParam`/`@McpPromptParam` annotations |
| `oap-mcp-admin`            | `McpLog` — MCP tools/prompts wrapping `LogWS` log-level management; `McpJPath` — MCP tools/prompts wrapping `JPathWS` for kernel service discovery and JPath queries |
| `oap-mcp-testing`          | Integration tests: `KernelFixture` + `HttpClientSseClientTransport` + API key auth        |

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

### JPathWS (`/system/admin/jpath`)

Query parameter `query`:
- `module.service.instance.field` — JPath expression evaluated against the kernel service tree
- `*` — list all services as `["module.service", ...]` sorted alphabetically
- `*suffix` / `prefix*` / `*contains*` — glob filter applied to the full `module.service` name

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

## checkstyle
- https://raw.githubusercontent.com/oaplatform/oap-maven/master/.idea/checkstyle.xml

