# OAP-WS: Web Services Framework

## Overview

**oap-ws** is a lightweight, annotation-driven web services framework for building REST APIs in Java. It provides a flexible and intuitive way to create HTTP endpoints with automatic parameter binding, type conversion, validation, session management, and response handling.

The framework is designed to work seamlessly with the OAP (Open Application Platform) ecosystem, leveraging reflection-based routing and Undertow as the underlying HTTP server.

### Key Features

- **Annotation-driven REST endpoints**: Use `@WsMethod` and `@WsParam` annotations to define HTTP methods
- **Automatic parameter binding**: Bind parameters from query strings, path variables, request body, headers, cookies, and sessions
- **Type conversion and parsing**: Automatic deserialization of JSON and other data formats
- **Flexible responses**: Built-in support for various response types (objects, streams, optionals, CompletableFutures)
- **Session management**: Cookie-based session support with configurable expiration
- **Interceptors**: Request/response interceptors for cross-cutting concerns
- **Validation**: Annotation-based parameter and method validation
- **Compression support**: GZIP compression for responses
- **OpenAPI integration**: Generate OpenAPI documentation from service definitions
- **Admin endpoints**: Built-in endpoints for API documentation, schema exploration, and logging
- **File handling**: Utilities for file upload and download operations
- **Security/SSO support**: Authentication and authorization support

## Maven Coordinates

**Parent module:**
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-ws-parent</artifactId>
    <version>${oap.project.version}</version>
</dependency>
```

**Core module:**
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-ws</artifactId>
    <version>${oap.project.version}</version>
</dependency>
```

**Additional modules:**
- `oap-ws-test`: Testing utilities
- `oap-ws-api-ws`: API documentation endpoints
- `oap-ws-admin-ws`: Admin/monitoring endpoints
- `oap-ws-openapi-ws`: OpenAPI 3.0+ documentation
- `oap-ws-file-ws`: File upload/download handling
- `oap-ws-sso`: Single Sign-On support
- `oap-ws-openapi-maven-plugin`: Maven plugin for OpenAPI generation

## Core Concepts

### Web Services (`@WsMethod`)

Web service methods are plain Java methods annotated with `@WsMethod`. The framework automatically maps HTTP requests to these methods based on the path and HTTP method.

```java
public class HelloWS {
    @WsMethod(method = GET, path = "/hello")
    public String hello() {
        return "Hello, World!";
    }

    @WsMethod(method = GET, path = "/greet/{name}", description = "Greets a user by name")
    public String greet(@WsParam(from = PATH) String name) {
        return "Hello, " + name + "!";
    }
}
```

### Parameter Binding (`@WsParam`)

Parameters are automatically bound from HTTP requests using the `@WsParam` annotation. Different sources are supported:

- **`QUERY`** (default): URL query parameters
- **`PATH`**: Path variables in curly braces `{varName}`
- **`BODY`**: Request body (typically JSON)
- **`HEADER`**: HTTP headers
- **`COOKIE`**: HTTP cookies
- **`SESSION`**: Session attributes (requires `sessionAware = true`)

```java
@WsMethod(method = POST, path = "/api/users")
public User createUser(
    @WsParam(from = BODY) User user,
    @WsParam(from = HEADER, name = "X-Request-ID") String requestId,
    @WsParam(from = COOKIE, name = "sessionId") String sessionId
) {
    // Process user creation
    return user;
}
```

When `@WsParam` is not specified on a parameter, it defaults to `QUERY` parameter binding.

### Response Handling

Methods can return various response types:

- **`void`**: Responds with 204 No Content
- **`Object`**: Serialized to JSON (200 OK)
- **`Optional<T>`**: Returns 404 if empty, otherwise returns the wrapped value
- **`Response`**: Fully customized response with status, headers, cookies, and body
- **`Stream<T>`**: Streamed JSON response
- **`Result<T, E>`**: Success or failure response
- **`CompletableFuture<T>`**: Asynchronous response handling

```java
@WsMethod(method = GET, path = "/users/{id}")
public Optional<User> getUser(@WsParam(from = PATH) String id) {
    return userRepository.findById(id);
}

@WsMethod(method = POST, path = "/upload")
public Response uploadFile(@WsParam(from = BODY) byte[] fileContent) {
    return Response.ok()
        .withBody(new UploadResult(fileId))
        .withContentType("application/json");
}

@WsMethod(method = GET, path = "/export")
public Stream<String> exportData() {
    return dataService.streamData();
}
```

### Routing and Path Matching

Paths can include placeholders for dynamic segments using curly braces:

- `/users/{userId}`: Matches `/users/123`
- `/items/{id}/details`: Matches `/items/abc/details`
- `/path/={id}/segment`: Pattern with equals sign (e.g., `/sort=3/test`)

If no `@WsMethod` annotation is present, the method name is used as the path:

```java
public int sumab(int a, int b) {  // Maps to /sumab?a=1&b=2
    return a + b;
}
```

### HTTP Methods

The `method` attribute specifies which HTTP methods are accepted:

```java
@WsMethod(method = GET, path = "/data")
public Data getData() { ... }

@WsMethod(method = POST, path = "/data")
public void saveData(@WsParam(from = BODY) Data data) { ... }

@WsMethod(method = { GET, POST }, path = "/dual")
public void dual() { ... }
```

Defaults to both GET and POST if not specified.

### Session Management

Session-aware services automatically create and manage user sessions via cookies:

```java
@WsMethod(method = GET, path = "/login")
public Response login(@WsParam(from = BODY) Credentials creds, Session session) {
    session.set("userId", user.getId());
    return Response.ok();
}

@WsMethod(method = GET, path = "/profile")
public User getProfile(Session session) {
    String userId = session.get("userId");
    return userService.findById(userId);
}
```

Session configuration:

```
services {
  oap-ws {
    session-manager.parameters {
      expirationTime = 24h
      cookiePath = "/"
      cookieDomain = "example.com"
      cookieSecure = true
    }
  }
}
```

### Interceptors

Interceptors allow you to execute code before and after request processing:

```java
public class AuthInterceptor implements Interceptor {
    @Override
    public Optional<Response> before(InvocationContext context) {
        String authHeader = context.exchange.getRequestHeader("Authorization");
        if (authHeader == null || !isValid(authHeader)) {
            return Optional.of(Response.unauthorized());
        }
        return Optional.empty();  // Continue processing
    }

    @Override
    public void after(Response response, InvocationContext context) {
        // Log response or modify headers
        response.withHeader("X-Processed", "true");
    }
}
```

Register interceptors in configuration:

```
services {
  my-interceptor {
    implementation = com.example.AuthInterceptor
  }
  
  my-ws-service {
    ext.ws-service.parameters {
      path = "/api"
      interceptors = ["my-interceptor"]
    }
  }
}
```

### Input Validation

Use `@WsValidate` annotation for schema-based validation:

```java
@WsMethod(method = POST, path = "/users")
@WsValidate("user-schema")
public User createUser(@WsParam(from = BODY) User user) {
    return userService.save(user);
}
```

### Content Negotiation

Control the response content type:

```java
@WsMethod(produces = "text/plain", path = "/export")
public String exportAsPlain() {
    return "Plain text response";
}

@WsMethod(produces = "application/json", path = "/data")
public Data getData() {
    return new Data();
}

@WsMethod(path = "/raw", raw = true)
public byte[] getRawData() {
    return new byte[] { /* ... */ };
}
```

## Getting Started

### 1. Create a Web Service Class

```java
package com.example.api;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.*;
import static oap.ws.WsParam.From.*;

public class MathWS {
    
    @WsMethod(method = GET, path = "/add")
    public int add(int a, int b) {
        return a + b;
    }

    @WsMethod(method = GET, path = "/multiply")
    public int multiply(int a, int b) {
        return a * b;
    }

    @WsMethod(method = POST, path = "/calculate")
    public CalculationResult calculate(
        @WsParam(from = BODY) Calculation calc
    ) {
        return new CalculationResult(calc.execute());
    }
}
```

### 2. Register in Module Configuration

Create `META-INF/oap-module.conf`:

```
name = math-module
dependsOn = oap-ws

services {
  math-ws {
    implementation = com.example.api.MathWS
    ext.ws-service.parameters {
      path = "/api/math"
      sessionAware = false
      compression = true
    }
  }
}
```

### 3. Include in Application Configuration

In `application.conf`:

```
boot.main = my-application

services {
  oap-http {
    oap-http-server.parameters {
      defaultPort.httpPort = 8080
    }
  }
  
  modules {
    math-module { }
  }
}
```

### 4. Access Your API

```bash
# GET request
curl http://localhost:8080/api/math/add?a=5&b=3

# POST request
curl -X POST http://localhost:8080/api/math/calculate \
  -H "Content-Type: application/json" \
  -d '{"operation":"add","values":[5,3]}'
```

## Configuration

### Service Configuration

Web services are configured via the OAP module system using extension points:

```
services {
  my-ws-service {
    implementation = com.example.MyWS
    
    ext.ws-service.parameters {
      # Paths where this service is bound
      path = ["/api", "/v1/api"]
      
      # Enable session support
      sessionAware = true
      
      # Enable GZIP compression
      compression = true
      
      # List of interceptors to apply
      interceptors = ["auth-interceptor", "logging-interceptor"]
      
      # Optional: specific port for this service
      port = "8081"
      
      # Optional: port types (http, https, etc.)
      portType = ["http"]
      
      # Enable blocking request handling
      blocking = true
    }
    
    supervision.supervise = true
  }
}
```

### Session Configuration

```
services {
  oap-ws {
    session-manager.parameters {
      # Session expiration time
      expirationTime = 24h
      
      # Cookie path
      cookiePath = "/"
      
      # Cookie domain
      cookieDomain = "localhost"
      
      # Secure flag (HTTPS only)
      cookieSecure = false
    }
  }
}
```

## API Reference

### Annotations

#### `@WsMethod`

Marks a method as a web service endpoint.

```java
@WsMethod(
    path = "/users/{id}",           // URL path (optional, defaults to method name)
    method = { GET, POST },          // HTTP methods (optional, defaults to GET, POST)
    produces = "application/json",   // Response content type (optional)
    raw = false,                     // Send raw bytes without JSON encoding (optional)
    description = "Get user by ID"   // OpenAPI description (optional)
)
```

#### `@WsParam`

Binds a method parameter to part of the HTTP request.

```java
@WsParam(
    from = QUERY,                   // Parameter source (default: QUERY)
    name = {"id", "userId"},        // Custom parameter name(s) (optional)
    description = "User identifier" // OpenAPI description (optional)
)
```

Supported sources:
- `QUERY`: URL query parameters (default)
- `PATH`: Path variables
- `BODY`: Request body
- `HEADER`: HTTP headers
- `COOKIE`: HTTP cookies
- `SESSION`: Session attributes

#### `@WsValidate`

Enables validation for method or parameter.

```java
@WsValidate("schema-name")
public void method(@WsParam(...) Data data) { ... }
```

### Core Classes

#### `Response`

Build custom HTTP responses:

```java
// Static factory methods
Response.ok()                          // 200 OK
Response.notFound()                    // 404 Not Found
Response.noContent()                   // 204 No Content
Response.redirect(uri)                 // 302 Found with location

// Builder methods
response.withStatusCode(201)           // Set HTTP status code
response.withReasonPhrase("Created")   // Set reason phrase
response.withContentType("application/json")
response.withBody(object)              // Set response body
response.withHeader("X-Custom", "value")
response.withCookie(cookie)            // Add response cookie
```

#### `Session`

Store and retrieve session data:

```java
session.set("key", value);             // Store attribute
Object value = session.get("key");     // Retrieve attribute
String id = session.id;                // Get session ID
```

#### `WebService`

Handles individual web service instances:

```java
WebService service = new WebService(
    instance,                          // Service object
    sessionAware,                       // Enable sessions
    sessionManager,                     // Session manager
    interceptors,                       // List of interceptors
    compressionSupport                  // Enable compression
);
```

#### `WebServices`

Manages multiple web services:

```java
webServices.bind(path, serviceInstance, sessionAware, 
    sessionManager, interceptors, compression, blocking, port, portType);
webServices.start();
webServices.stop();
```

### Parameter Handling

The framework automatically converts HTTP parameters to method parameter types:

```java
// Primitive types
@WsMethod
public void example(int id, String name, boolean active) { ... }

// Collections
@WsMethod
public void example(List<String> tags, Map<String, String> metadata) { ... }

// Optional
@WsMethod
public void example(Optional<String> filter) { ... }

// Custom objects (JSON deserialization)
@WsMethod
public void example(@WsParam(from = BODY) User user) { ... }

// Enums
@WsMethod
public void example(SortOrder order) { ... }

// Special parameters (injected, no @WsParam needed)
@WsMethod
public void example(HttpServerExchange exchange, Session session) { ... }
```

## Sub-Modules

### oap-ws-test

Testing utilities for web service testing.

### oap-ws-api-ws

Generates human-readable API documentation from service definitions:

```
GET /system/api              # API documentation in plain text
GET /system/api?deprecated=false  # Exclude deprecated methods
```

### oap-ws-admin-ws

Admin endpoints for system introspection:

```
GET /system/schema           # Browse JSON schemas
GET /system/logging          # View/configure logging
GET /system/jpath            # Query system objects
```

### oap-ws-openapi-ws

Generates OpenAPI 3.0+ documentation:

```
GET /openapi.json            # OpenAPI specification
```

Configuration:

```
oap-ws-openapi-ws.openapi-info.parameters {
  title = "My API"
  description = "API description"
  version = "1.0.0"
}
```

### oap-ws-file-ws

File upload and download handling:

```
POST /files/upload           # Upload files
GET /files/download/{id}     # Download files
```

### oap-ws-sso

Single Sign-On support with JWT tokens and TOTP:

```java
@WsMethod(method = POST, path = "/login")
@WsSecurity(realm = "app", permissions = {"user:read"})
public LoginResponse login(@WsParam(from = BODY) Credentials creds) { ... }
```

### oap-ws-openapi-maven-plugin

Maven plugin for generating OpenAPI documentation during build:

```xml
<plugin>
    <groupId>oap</groupId>
    <artifactId>oap-ws-openapi-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Error Handling

### Exception Responses

Exceptions thrown in web service methods are automatically converted to HTTP error responses:

```java
@WsMethod
public User getUser(String id) {
    return userService.findById(id)
        .orElseThrow(() -> new WsClientException(404, "User not found"));
}
```

### Validation Errors

Validation failures return 400 Bad Request with error details:

```json
{
  "errors": [
    {
      "path": "email",
      "message": "Invalid email format"
    }
  ]
}
```

### Response Status Codes

- **200 OK**: Successful GET/POST/PUT/DELETE request
- **204 No Content**: Successful void method
- **302 Found**: Redirect response
- **404 Not Found**: Resource not found (Optional.empty())
- **400 Bad Request**: Validation error
- **500 Internal Server Error**: Unhandled exception

## Compression

Enable GZIP compression in configuration:

```
ext.ws-service.parameters {
  compression = true  # Default: true
}
```

The server automatically handles request decompression and response compression based on client capabilities.

## Performance Considerations

- **Blocking mode**: Set `blocking = false` for non-blocking I/O
- **Compression**: Disable for small responses
- **Session expiration**: Configure appropriate timeout
- **Interceptors**: Minimize processing in before/after hooks
- **Type conversion**: Use native types when possible

## Examples

### RESTful API

```java
public class UserAPI {
    private final UserService userService;

    @WsMethod(method = GET, path = "/users", description = "List all users")
    public List<User> listUsers(@WsParam Optional<String> filter) {
        return userService.findAll(filter.orElse(""));
    }

    @WsMethod(method = GET, path = "/users/{id}", description = "Get user by ID")
    public Optional<User> getUser(@WsParam(from = PATH) String id) {
        return userService.findById(id);
    }

    @WsMethod(method = POST, path = "/users", description = "Create new user")
    public User createUser(@WsParam(from = BODY) User user) {
        return userService.save(user);
    }

    @WsMethod(method = PUT, path = "/users/{id}", description = "Update user")
    public User updateUser(
        @WsParam(from = PATH) String id,
        @WsParam(from = BODY) User user
    ) {
        return userService.update(id, user);
    }

    @WsMethod(method = DELETE, path = "/users/{id}", description = "Delete user")
    public void deleteUser(@WsParam(from = PATH) String id) {
        userService.delete(id);
    }
}
```

### File Upload/Download

```java
public class FileAPI {
    @WsMethod(method = POST, path = "/upload", produces = "application/json")
    public UploadResult uploadFile(
        @WsParam(from = BODY) byte[] fileContent,
        @WsParam Optional<String> filename
    ) {
        String fileId = fileService.save(fileContent, filename);
        return new UploadResult(fileId);
    }

    @WsMethod(method = GET, path = "/download/{id}", produces = "application/octet-stream")
    public byte[] downloadFile(@WsParam(from = PATH) String id) {
        return fileService.read(id);
    }
}
```

### Streaming Data

```java
public class DataAPI {
    @WsMethod(method = GET, path = "/stream", description = "Stream data items")
    public Stream<DataItem> streamData() {
        return dataService.stream();
    }

    @WsMethod(method = POST, path = "/bulk", description = "Process bulk items")
    public List<ProcessResult> processBulk(
        @WsParam(from = BODY) List<DataItem> items
    ) {
        return items.stream()
            .map(dataService::process)
            .toList();
    }
}
```

## Testing

Use the OAP testing fixtures:

```java
public class UserAPITest extends Fixtures {
    private final KernelFixture kernel;

    public UserAPITest() {
        kernel = fixture(new KernelFixture(
            new TestDirectoryFixture(),
            urlOrThrow(getClass(), "/application.test.conf")
        ));
    }

    @Test
    public void testListUsers() {
        assertGet(kernel.httpUrl("/api/users"))
            .responded(OK, "OK", APPLICATION_JSON, 
                "[{\"id\":\"1\",\"name\":\"John\"}]");
    }

    @Test
    public void testCreateUser() {
        assertPost(kernel.httpUrl("/api/users"), 
            "{\"name\":\"Jane\"}", APPLICATION_JSON)
            .respondedJson(CREATED, "{\"id\":\"2\",\"name\":\"Jane\"}");
    }
}
```

## License

MIT License - See LICENSE file in the project root

## Contributors

Open Application Platform Authors
