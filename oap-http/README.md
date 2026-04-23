# oap-http

HTTP server and client infrastructure for the OAP platform. Provides an Undertow-based server with named ports, a high-performance non-blocking pipeline, Prometheus metrics exporters, and test utilities.

## Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-http](oap-http/README.md) | `NioHttpServer`, `OapHttpClient`, `HealthHttpHandler`, `HttpServerExchange` | — |
| [oap-pnio-v3](oap-pnio-v3/README.md) | High-performance non-blocking pipeline (`PnioHttpHandler`, `PnioExchange`) | `oap-http` |
| [oap-http-prometheus](oap-http-prometheus/README.md) | Prometheus scrape endpoint, JVM metrics, application info exporter | `oap-http` |
| [oap-http-test](oap-http-test/README.md) | `HttpAsserts`, `HttpServerExchangeStub`, `MockHttpContext` | `oap-http` |

---

## Quick start

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

See [oap-http](oap-http/README.md) for the full server reference.

---

## Optional add-ons

| Need | Add module |
|---|---|
| High-performance non-blocking pipeline | `oap-pnio-v3` |
| Prometheus metrics scrape endpoint | `oap-http-prometheus` |
| HTTP assertions in tests | `oap-http-test` |
