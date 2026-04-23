# oap-http

Core HTTP module for the OAP platform. Provides an Undertow-based server (`NioHttpServer`) with named ports and built-in handlers, a Jetty-based HTTP client (`OapHttpClient`), health endpoint, and low-level `HttpServerExchange` API.

## NioHttpServer

### OAP Module Integration

```hocon
name = my-module
dependsOn = [oap-http]

services {
  my-handler {
    implementation = com.example.MyHandler
    parameters {
      server = <modules.oap-http.oap-http-server>
    }
  }
}
```

Override server parameters in `application.conf`:

```hocon
services {
  oap-http.oap-http-server.parameters {
    defaultPort.httpPort = 9090
    workerThreads = 64
    statistics = true
  }
}
```

### Server parameters

| Parameter | Default | Description |
|---|---|---|
| `defaultPort.httpPort` | `8080` | Public HTTP port |
| `defaultPort.httpsPort` | — | HTTPS port (requires `keyStore` + `password`) |
| `additionalHttpPorts` | `{ httpprivate = 8081 }` | Named extra ports |
| `backlog` | `-1` (OS default) | TCP accept backlog |
| `idleTimeout` | `-1` (disabled) | Idle connection timeout |
| `tcpNodelay` | `true` | Disable Nagle algorithm |
| `ioThreads` | `max(2, CPU count)` | Undertow IO threads |
| `workerThreads` | `CPU count × 8` | Undertow worker threads |
| `maxEntitySize` | `-1` (unlimited) | Maximum request body size in bytes |
| `maxParameters` | `-1` (unlimited) | Maximum number of query/form parameters |
| `maxHeaders` | `-1` (unlimited) | Maximum number of request headers |
| `maxHeaderSize` | `-1` (unlimited) | Maximum size of a single header value |
| `statistics` | `false` | Enable connector statistics |
| `alwaysSetDate` | `true` | Always include `Date` response header |
| `alwaysSetKeepAlive` | `true` | Always include `Connection: keep-alive` |

### Named ports

The `httpprivate` port (8081) is pre-configured for internal endpoints (health, admin, metrics). Handlers can be bound to a named port:

```java
server.bind( "/metrics", metricsHandler, "httpprivate" );
```

Omitting the port name binds to the default public port.

### Built-in handlers

The default handler chain (configured in `oap-module.oap`) includes:

| Handler | Parameter | Default | Description |
|---|---|---|---|
| `keepalive-requests-handler` | `keepaliveRequests` | `1000` | Close connection after N requests |
| `nio-compression-handler` | — | — | Gzip/deflate response compression |
| `blocking-read-timeout-handler` | `readTimeout` | `60s` | Abort slow request body reads |

### `HttpServerExchange`

Low-level request/response object passed to every `HttpHandler`.

**Reading the request:**

```java
String method   = exchange.getRequestMethod();
String path     = exchange.getRequestPath();
String query    = exchange.getQueryString();
String header   = exchange.requestHeader( "X-Trace-Id" );
String param    = exchange.getQueryParameter( "page" );
byte[] body     = exchange.getRequestBytes();
String bodyStr  = exchange.getRequestString();
```

**Writing the response:**

```java
exchange.setStatusCode( 200 );
exchange.responseHeader( "X-Custom", "value" );
exchange.responseBody( "hello" );
exchange.responseJson( "{\"ok\":true}" );
exchange.responseBytes( bytes, "application/octet-stream" );
```

### Health endpoint

`HealthHttpHandler` serves `GET /healtz` on `httpprivate`. It returns a JSON object built from all registered `HealthDataProvider` instances:

```java
public interface HealthDataProvider<T> {
    String name();
    T data();
}
```

Register additional data providers:

```hocon
services {
  my-health {
    implementation = com.example.MyHealthProvider
    parameters {
      healthHandler = <modules.oap-http.oap-http-health>
    }
  }
}
```

```java
public class MyHealthProvider implements HealthDataProvider<Map<String, Object>> {
    public MyHealthProvider( HealthHttpHandler healthHandler ) {
        healthHandler.addProvider( this );
    }

    @Override public String name() { return "my-service"; }
    @Override public Map<String, Object> data() { return Map.of( "status", "up" ); }
}
```

The endpoint accepts an optional `secret` query parameter for protecting the response.

---

## OapHttpClient

Jetty-based HTTP client built on virtual threads. Use `OapHttpClient.customHttpClient()` to build one:

```java
HttpClient client = OapHttpClient.customHttpClient()
    .connectionTimeout( Duration.ofSeconds( 5 ) )
    .followRedirects( true )
    .maxConnectionsPerDestination( 128 )
    .pool( OapHttpClientBuilder.Pool.ROUND_ROBIN )
    .build();

ContentResponse response = client.newRequest( "http://example.com/api" )
    .method( HttpMethod.GET )
    .send();
```

### Builder parameters

| Parameter | Default | Description |
|---|---|---|
| `connectionTimeout` | `10s` | TCP connection timeout |
| `followRedirects` | `false` | Follow HTTP 3xx redirects |
| `maxConnectionsPerDestination` | `64` | Max concurrent connections per host |
| `dnsjava` | `false` | Use dnsjava resolver instead of JVM default |
| `pool` | `RANDOM` | Connection pool strategy (`RANDOM` or `ROUND_ROBIN`) |

The client uses virtual threads (`VirtualThreadPool`) for all blocking operations.

---

## HTTP utilities

`Http` contains `StatusCode` constants for common HTTP status codes:

```java
Http.StatusCode.OK              // 200
Http.StatusCode.BAD_REQUEST     // 400
Http.StatusCode.UNAUTHORIZED    // 401
Http.StatusCode.NOT_FOUND       // 404
Http.StatusCode.INTERNAL_ERROR  // 500
```
