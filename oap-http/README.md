# OAP HTTP Framework

A high-performance, non-blocking HTTP server framework built on top of Undertow, designed for building scalable web applications with virtual thread support and advanced request handling capabilities.

## Overview

The OAP HTTP framework provides a foundation for building HTTP-based services with:

- **Non-blocking I/O**: Built on Undertow and XNIO for high-concurrency request handling
- **Virtual Thread Support**: Uses Java virtual threads for efficient request processing
- **Pluggable Handlers**: Extensible handler pipeline architecture for request/response manipulation
- **Multiple Port Support**: Run HTTP services on multiple ports with different configurations
- **SSL/HTTPS Support**: Built-in support for HTTPS with configurable keystore
- **Compression**: Automatic gzip and deflate compression support
- **Keep-Alive Management**: Connection keep-alive configuration and lifecycle management
- **Health Checks**: Built-in health endpoint support
- **Metrics Collection**: Micrometer integration for Prometheus metrics
- **Remote Services**: Distributed service invocation over HTTP with serialization support

## Maven Coordinates

```xml
<groupId>oap</groupId>
<artifactId>oap-http</artifactId>
<version>${oap.project.version}</version>
```

## Sub-Modules

### oap-http (Core HTTP Server)

The core HTTP server implementation built on Undertow. Provides:

- **NioHttpServer**: Main HTTP server class managing port listeners and request handling
- **HttpServerExchange**: Wrapper around Undertow's exchange with convenience methods
- **HttpHandler**: Interface for implementing custom request handlers
- **Built-in Handlers**: Compression, bandwidth limiting, keep-alive management, blocking read timeout

**Maven Dependency**:
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-http</artifactId>
    <version>${oap.project.version}</version>
</dependency>
```

### oap-pnio-v3 (Advanced NIO Handler Implementation)

Advanced PNIO (Pluggable NIO) v3 implementation providing:

- Request routing and pattern matching
- Asynchronous task processing
- Worker pool management
- WebSocket support
- Advanced metrics collection

**Use Case**: For complex routing scenarios and advanced async processing requirements.

### oap-remote (Remote Service Invocation)

Enables distributed service calls over HTTP with:

- Service registry integration
- Binary serialization (FST - Fast Serialization)
- Automatic method invocation routing
- Support for CompletableFuture async returns
- Metrics tracking for remote calls

**Maven Dependency**:
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-remote</artifactId>
    <version>${oap.project.version}</version>
</dependency>
```

**Configuration Example**:
```
remoting {
  implementation = oap.remote.Remote
  parameters {
    server = <modules.oap-http.oap-http-server>
    context = /remote/
    serialization = DEFAULT
    services = <service-reference>
    port = httpprivate
  }
}
```

### oap-remote-application

Application-level remote service support with kernel integration. Handles:

- Automatic service discovery and registration
- Dependency injection for remote services
- Service lifecycle management

### oap-http-prometheus (Metrics Export)

Prometheus metrics exporter providing:

- HTTP metrics endpoint at `/metrics`
- JVM metrics (memory, GC, threads, classloaders)
- Application info export
- Micrometer integration

**Maven Dependency**:
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-http-prometheus</artifactId>
    <version>${oap.project.version}</version>
</dependency>
```

**Configuration Example**:
```
oap-prometheus-metrics {
  implementation = oap.http.prometheus.PrometheusExporter
  parameters {
    port = httpprivate
    server = <modules.oap-http.oap-http-server>
  }
}

prometheus-jvm-exporter {
  implementation = oap.http.prometheus.PrometheusJvmExporter
  parameters {
    enableClassLoaderMetrics = true
    enableJvmMemoryMetrics = true
    enableJvmGcMetrics = true
    enableLogbackMetrics = true
    enableJvmThreadMetrics = true
  }
}
```

### oap-http-test (Testing Utilities)

Testing utilities for HTTP-based tests:

- **HttpServerExchangeStub**: Mock HTTP server exchange for testing
- **HttpAsserts**: Assertion helpers for HTTP responses
- **MockHttpContext**: Mock HTTP context for unit tests
- **MockCookieStore**: Cookie store mock implementation

**Maven Dependency**:
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-http-test</artifactId>
    <version>${oap.project.version}</version>
    <scope>test</scope>
</dependency>
```

## Core Concepts

### HTTP Server (NioHttpServer)

The main server class that manages HTTP listeners and request routing:

```java
// Create server on port 8080
NioHttpServer server = new NioHttpServer(new NioHttpServer.DefaultPort(8080));

// Add handlers to the pipeline
server.handlers.add(new KeepaliveRequestsHandler(1000));
server.handlers.add(new CompressionNioHandler());
server.handlers.add(new BandwidthHandler());

// Bind request handler to a path
server.bind("/api/users", exchange -> {
    exchange.responseJson(new User("John", "Doe"));
});

// Start the server
server.start();
```

### HTTP Handler (HttpHandler)

Simple functional interface for handling HTTP requests:

```java
public interface HttpHandler {
    void handleRequest(HttpServerExchange exchange) throws Exception;
}
```

### Server Exchange (HttpServerExchange)

Wrapper around Undertow's HttpServerExchange providing convenient methods for:

- Reading request data (headers, parameters, body, method)
- Writing responses (JSON, plain text, streams, binary data)
- Managing cookies and headers
- Request/response lifecycle management

### Handler Pipeline

Handlers are applied in a chain, allowing you to:

1. Process requests before they reach your business logic
2. Manipulate responses before sending to client
3. Collect metrics
4. Implement cross-cutting concerns

Built-in handlers include:

- **KeepaliveRequestsHandler**: Manages HTTP keep-alive connection limits
- **CompressionNioHandler**: Handles gzip/deflate compression
- **BandwidthHandler**: Limits request/response bandwidth per connection
- **BlockingReadTimeoutHandler**: Enforces read timeout on request body

### Port Management

Run services on multiple ports with different configurations:

```java
server.additionalHttpPorts.put("admin", 8081);
server.additionalHttpPorts.put("metrics", 8082);

// Bind handler to specific port
server.bind("/admin", adminHandler, "admin");
server.bind("/metrics", metricsHandler, "metrics");
```

### HTTPS/SSL Support

Enable HTTPS with certificate configuration:

```java
NioHttpServer server = new NioHttpServer(
    new NioHttpServer.DefaultPort(
        8080,                                    // HTTP port
        8443,                                    // HTTPS port
        Resources.urlOrThrow(class, "/cert.jks"), // KeyStore path
        "password"                               // KeyStore password
    )
);
```

## Getting Started

### Simple Echo Server

```java
import oap.http.server.nio.NioHttpServer;
import oap.http.Http;
import oap.testng.Ports;

public class HelloWorldServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        
        try (NioHttpServer server = new NioHttpServer(
            new NioHttpServer.DefaultPort(port))) {
            
            // Bind a simple handler
            server.bind("/hello", exchange -> {
                String name = exchange.getStringParameter("name");
                String message = name != null 
                    ? "Hello, " + name + "!" 
                    : "Hello, World!";
                    
                exchange.responseOk(message, Http.ContentType.TEXT_PLAIN);
            });
            
            server.start();
            System.out.println("Server started on port " + port);
            
            // Keep the server running
            Thread.currentThread().join();
        }
    }
}
```

**Test it**:
```bash
curl "http://localhost:8080/hello"
curl "http://localhost:8080/hello?name=Alice"
```

### JSON API Handler

```java
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;

public class UserApiHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().name();
        String path = exchange.getRelativePath();
        
        switch (method) {
            case "GET":
                handleGet(exchange, path);
                break;
            case "POST":
                handlePost(exchange);
                break;
            case "PUT":
                handlePut(exchange, path);
                break;
            case "DELETE":
                handleDelete(exchange, path);
                break;
            default:
                exchange.setStatusCode(405);
        }
    }
    
    private void handleGet(HttpServerExchange exchange, String path) {
        // Extract user ID from path and fetch user
        exchange.responseJson(new User(1L, "John", "john@example.com"));
    }
    
    private void handlePost(HttpServerExchange exchange) throws Exception {
        // Read JSON body
        byte[] body = exchange.readBody();
        User user = Binder.json.unmarshal(User.class, body);
        // Save user to database
        exchange.responseJson(201, user);
    }
    
    // ... other methods
}
```

### Using Handlers

```java
// Create server with handlers
try (NioHttpServer server = new NioHttpServer(
    new NioHttpServer.DefaultPort(8080))) {
    
    // Add handlers to the pipeline (they execute in reverse order of addition)
    server.handlers.add(new KeepaliveRequestsHandler(1000));
    server.handlers.add(new CompressionNioHandler());
    
    // Bind handlers
    server.bind("/users", new UserApiHandler());
    server.bind("/products", new ProductApiHandler());
    
    // Health check endpoint
    HealthHttpHandler health = new HealthHttpHandler(
        server, "/health", "default-http");
    health.addProvider(new DatabaseHealthProvider());
    health.start();
    
    server.start();
}
```

### Testing HTTP Handlers

```java
import oap.http.test.HttpAsserts;
import oap.http.test.MockHttpContext;

public class UserApiHandlerTest {
    @Test
    public void testGetUser() throws Exception {
        // Use test utilities
        HttpAsserts.assertGet("http://localhost:8080/users/1")
            .hasCode(Http.StatusCode.OK)
            .hasContentType(ContentTypes.APPLICATION_JSON);
    }
}
```

## Configuration Reference

### oap-module.conf - HTTP Server Configuration

```
oap-http-server {
  implementation = oap.http.server.nio.NioHttpServer
  parameters {
    defaultPort {
      httpPort = 8080
      httpsPort = 8443
      keyStore = /path/to/keystore.jks
      password = keystorePassword
    }
    
    # Additional ports for separate concerns
    additionalHttpPorts {
      httpprivate = 8081
      metrics = 8082
    }
    
    # Connection settings
    backlog = -1                    # Accept queue size (-1 = default)
    idleTimeout = -1               # Connection idle timeout in ms (-1 = no timeout)
    tcpNodelay = true              # Disable Nagle's algorithm
    
    # Thread pool configuration
    ioThreads = -1                 # I/O threads (-1 = max(2, CPU count))
    workerThreads = -1             # Worker threads (-1 = CPU count * 8)
    
    # Request constraints
    maxEntitySize = -1             # Max request body size (-1 = unlimited)
    maxParameters = -1             # Max query/form parameters (-1 = 1000)
    maxHeaders = -1                # Max headers per request (-1 = 200)
    maxHeaderSize = -1             # Max header size (-1 = 1MB)
    
    # Response configuration
    alwaysSetDate = true           # Always add Date header
    alwaysSetKeepAlive = true      # Always set Connection header
    
    # Monitoring
    statistics = false             # Enable connector statistics
    
    # Handler pipeline
    handlers = [
      <modules.this.keepalive-requests-handler>
      <modules.this.nio-bandwidth-handler>
      <modules.this.nio-compression-handler>
      <modules.this.blocking-read-timeout-handler>
    ]
  }
  supervision.supervise = true
}

# Keep-alive handler configuration
keepalive-requests-handler {
  implementation = oap.http.server.nio.handlers.KeepaliveRequestsHandler
  parameters {
    keepaliveRequests = 1000
  }
}

# Compression handler (no configuration needed)
nio-compression-handler {
  implementation = oap.http.server.nio.handlers.CompressionNioHandler
}

# Bandwidth limiting handler
nio-bandwidth-handler {
  implementation = oap.http.server.nio.handlers.BandwidthHandler
  supervision.supervise = true
}

# Read timeout handler
blocking-read-timeout-handler {
  implementation = oap.http.server.nio.handlers.BlockingReadTimeoutHandler
  parameters {
    readTimeout = 60s
  }
}

# Health endpoint configuration
oap-http-health-handler {
  implementation = oap.http.server.nio.health.HealthHttpHandler
  parameters {
    server = <modules.this.oap-http-server>
    prefix = /healtz
    port = httpprivate
    providers = []
  }
  supervision.supervise = true
}

# Zero PNG tracking pixel (disabled by default)
oap-http-zero-png-handler {
  enabled = false
  implementation = oap.http.server.nio.ZeroPngHttpHandler
  parameters {
    server = <modules.this.oap-http-server>
    prefix = /static/pixel.png
  }
}
```

### Handler Configuration Examples

```
# Bandwidth Handler - Rate limiting per connection
nio-bandwidth-handler {
  implementation = oap.http.server.nio.handlers.BandwidthHandler
  supervision.supervise = true
}

# Compression - Automatic gzip/deflate
nio-compression-handler {
  implementation = oap.http.server.nio.handlers.CompressionNioHandler
}

# Keep-alive - Close connection after N requests
keepalive-requests-handler {
  implementation = oap.http.server.nio.handlers.KeepaliveRequestsHandler
  parameters {
    keepaliveRequests = 1000
  }
}

# Read Timeout - Enforce body read timeout
blocking-read-timeout-handler {
  implementation = oap.http.server.nio.handlers.BlockingReadTimeoutHandler
  parameters {
    readTimeout = 60s
  }
}
```

## API Reference

### NioHttpServer

Main HTTP server class managing ports and handlers.

**Key Methods**:

- `start()`: Start the HTTP server
- `bind(String prefix, HttpHandler handler)`: Bind handler to path with compression
- `bind(String prefix, HttpHandler handler, boolean blocking)`: Bind with optional blocking
- `bind(String prefix, HttpHandler handler, boolean blocking, String port)`: Bind to specific port
- `preStop()`: Stop the server gracefully
- `close()`: Close server and cleanup resources

**Key Fields**:

- `defaultPort`: Default HTTP/HTTPS port configuration
- `defaultPorts`: Map of default port names to ports
- `additionalHttpPorts`: Map of custom port names to ports
- `handlers`: List of NioHandlerBuilder instances in pipeline
- `ioThreads`: Number of I/O threads (-1 = auto)
- `workerThreads`: Number of worker threads (-1 = auto)
- `statistics`: Enable connector statistics collection

**Example**:

```java
NioHttpServer server = new NioHttpServer(
    new NioHttpServer.DefaultPort(8080));

server.additionalHttpPorts.put("admin", 8081);
server.handlers.add(new CompressionNioHandler());

server.bind("/api", myHandler);
server.start();
```

### HttpServerExchange

Wrapper around Undertow's exchange with convenience methods.

**Request Methods**:

```java
// URI and path information
String getRequestURI()
String getRelativePath()
String getResolvedPath()
String getFullRequestURL()

// HTTP method
HttpMethod getRequestMethod()
HttpServerExchange setRequestMethod(HttpMethod method)

// Query parameters
Map<String, Deque<String>> getQueryParameters()
Deque<String> getQueryParameter(String name)
String getStringParameter(String name)
boolean getBooleanParameter(String name)

// Headers
HeaderMap getRequestHeaders()
String getRequestHeader(String name)
String getRequestHeader(String name, String defaultValue)

// Cookies
String getRequestCookieValue(String name)

// Body
InputStream getInputStream()
byte[] readBody() throws IOException

// Client info
String ip()
String ua()
String referrer()

// Compression
boolean gzipSupported()
boolean isRequestGzipped()
```

**Response Methods**:

```java
// Status codes
int getStatusCode()
HttpServerExchange setStatusCode(int statusCode)
HttpServerExchange setStatusCodeReasonPhrase(int statusCode, String message)

// Headers
HttpServerExchange setResponseHeader(String name, String value)

// Cookies
HttpServerExchange setResponseCookie(Cookie cookie)

// Body - Simple responses
void responseOk(String body, String contentType)
void responseOk(byte[] content, String contentType)
void responseJson(Object body)
void responseJson(int statusCode, Object body)
void responseJson(int statusCode, String reasonPhrase, Object body)

// Body - Streaming
void responseStream(Stream<?> content, boolean raw, String contentType)

// Special responses
void responseNotFound()
void responseNoContent()
void redirect(String url)

// Raw output
OutputStream getOutputStream()
void send(String data)
void send(String data, Charset charset)
void send(ByteBuffer buffer)
void send(byte[] bytes)

// Lifecycle
boolean isResponseStarted()
HttpServerExchange endExchange()
void closeConnection()
```

**Example**:

```java
public void handleRequest(HttpServerExchange exchange) throws Exception {
    // Check client IP and method
    String clientIp = exchange.ip();
    HttpServerExchange.HttpMethod method = exchange.getRequestMethod();
    
    if (method == HttpServerExchange.HttpMethod.GET) {
        // Read query parameter
        String userId = exchange.getStringParameter("id");
        
        // Response with JSON
        User user = database.getUser(userId);
        exchange.responseJson(Http.StatusCode.OK, user);
    } else {
        exchange.setStatusCode(405);
    }
}
```

### HttpHandler

Simple functional interface for request handling:

```java
public interface HttpHandler {
    void handleRequest(HttpServerExchange exchange) throws Exception;
}
```

**Lambda Implementation**:

```java
server.bind("/hello", exchange -> {
    exchange.responseOk("Hello World", Http.ContentType.TEXT_PLAIN);
});
```

**Class Implementation**:

```java
public class MyHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // Handle request
    }
}
```

### Built-in Handlers

#### CompressionNioHandler

Automatic gzip and deflate compression:

```java
server.handlers.add(new CompressionNioHandler());
```

Supports both response compression and request decompression.

#### KeepaliveRequestsHandler

Close connection after N keep-alive requests:

```java
server.handlers.add(new KeepaliveRequestsHandler(1000));
```

#### BandwidthHandler

Rate limit bandwidth per connection:

```java
BandwidthHandler bwHandler = new BandwidthHandler();
server.handlers.add(bwHandler);
bwHandler.start();
```

#### BlockingReadTimeoutHandler

Enforce timeout on request body read:

```java
server.handlers.add(
    new BlockingReadTimeoutHandler(Duration.ofSeconds(60)));
```

### HealthHttpHandler

Built-in health check endpoint:

```java
HealthHttpHandler health = new HealthHttpHandler(
    server, 
    "/health",      // endpoint path
    "default-http"  // port name
);

// Add custom health providers
health.addProvider(new DatabaseHealthProvider());
health.addProvider(new CacheHealthProvider());

health.start();
```

Responds with:
- `204 No Content` if no secret or secret doesn't match
- `200 OK` with JSON health data if secret matches (optional)

### Remote Service Handler

Expose services over HTTP:

```java
Remote remote = new Remote(
    FST.SerializationMethod.DEFAULT,
    "/remote/",
    services,
    server,
    "httpprivate"
);
remote.start();
```

Handles binary RPC-style invocations with automatic serialization.

### PrometheusExporter

Export metrics in Prometheus format:

```java
PrometheusExporter exporter = new PrometheusExporter(
    server,
    "httpprivate"
);
```

Metrics available at `/metrics` endpoint in Prometheus text format.

## HTTP Status Codes Reference

Available through `Http.StatusCode` interface:

- **2xx Success**: OK(200), CREATED(201), NO_CONTENT(204)
- **3xx Redirection**: FOUND(302), TEMPORARY_REDIRECT(307)
- **4xx Client Errors**: BAD_REQUEST(400), UNAUTHORIZED(401), FORBIDDEN(403), NOT_FOUND(404)
- **5xx Server Errors**: INTERNAL_SERVER_ERROR(500), NOT_IMPLEMENTED(501)

## Content Types Reference

Available through `Http.ContentType` interface:

- `TEXT_PLAIN`
- `TEXT_HTML`
- `APPLICATION_JSON`
- `APPLICATION_OCTET_STREAM`
- `APPLICATION_FORM_URLENCODED`

## Advanced Features

### Virtual Threads

The HTTP server uses Java virtual threads for I/O operations via Undertow's configurable worker pool:

```java
// Handler execution runs on virtual threads automatically
server.bind("/api", exchange -> {
    // This executes on a virtual thread
    assert Thread.currentThread().isVirtual();
});
```

### Metrics Collection

When `statistics = true` in config, metrics are collected:

```
nio_requests{port="8080",type="total|active|errors"}
nio_connections{port="8080",type="active"}
nio_pool_size{port="8080",name="worker",type="active|core|max|busy|queue"}
```

Access via Micrometer:

```java
Metrics.gauge("nio_requests", Tags.of("port", "8080"), ...);
```

### Multiple Port Configuration

Run separate concerns on isolated ports:

```java
server.additionalHttpPorts.put("admin", 8081);
server.additionalHttpPorts.put("metrics", 8082);
server.additionalHttpPorts.put("health", 8083);

server.bind("/admin", adminHandler, "admin");
server.bind("/metrics", metricsHandler, "metrics");
server.bind("/health", healthHandler, "health");
```

This allows:
- Public API on port 8080
- Admin/internal tools on port 8081
- Metrics on port 8082
- Health checks on port 8083

### Custom Headers

```java
exchange.setResponseHeader("X-Custom-Header", "value");
exchange.setResponseHeader("Cache-Control", "no-cache");
exchange.setResponseHeader("Access-Control-Allow-Origin", "*");
```

## License

The MIT License (MIT)

Copyright (c) Open Application Platform Authors

Permission is granted for free use, modification, and distribution with appropriate attribution.

