# oap-ws

Annotation-driven HTTP web services for the OAP framework. Declare endpoints with `@WsMethod` and `@WsParam`, register the class in `oap-module.oap` with a `ws-service` block, and the framework wires everything to the Undertow HTTP server automatically — no servlet container, no boilerplate.

## Table of Contents

- [Architecture](#architecture)
- [Declaring a Web Service](#declaring-a-web-service)
  - [ws-service block reference](#ws-service-block-reference)
- [@WsMethod](#wsmethod)
- [@WsParam — parameter sources](#wsparam--parameter-sources)
  - [Implicit injection](#implicit-injection)
  - [Type coercion](#type-coercion)
- [Return types and Response](#return-types-and-response)
  - [Response builder](#response-builder)
- [Interceptors](#interceptors)
- [Session handling](#session-handling)
- [Validation](#validation)
- [Error handling](#error-handling)
- [OAP Module Integration](#oap-module-integration)

---

## Architecture

```
HTTP request
     │
     ▼
NioHttpServer (oap-http, port 8080 / 8081)
     │
     ▼
WebServices  ──── discovers ws-service extensions from Kernel
     │
     ▼
Interceptors (before) ─── ordered list; any can short-circuit
     │
     ▼
WebService (reflection dispatcher)
  ├── resolves matching @WsMethod
  ├── deserialises parameters (@WsParam)
  └── invokes method
     │
     ▼
Interceptors (after) ─── reverse order
     │
     ▼
Response serialised to HTTP
```

`WebServices` and `SessionManager` are provided by the `oap-ws` module and wired automatically when `oap-ws` is listed in `dependsOn`.

---

## Declaring a Web Service

**1. Annotate the service class:**

```java
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.WsParam.From;
import oap.http.server.nio.HttpServerExchange.HttpMethod;

public class ProductWS {

    @WsMethod( path = "/", method = HttpMethod.GET )
    public List<Product> list() {
        return productService.getAll();
    }

    @WsMethod( path = "/{id}", method = HttpMethod.GET )
    public Optional<Product> get( @WsParam( from = From.PATH ) String id ) {
        return productService.find( id );
    }

    @WsMethod( path = "/", method = HttpMethod.POST )
    public Response create( @WsParam( from = From.BODY ) Product product ) {
        productService.save( product );
        return Response.noContent();
    }
}
```

**2. Register in `oap-module.oap`:**

```hocon
services {
  product-ws {
    implementation = com.example.ProductWS
    parameters {
      productService = <modules.this.product-service>
    }
    ws-service {
      path = api/v1/products
    }
  }
}
```

**3. URLs produced:**

```
GET  /api/v1/products/
GET  /api/v1/products/{id}
POST /api/v1/products/
```

### ws-service block reference

All fields are optional unless noted.

| Field | Type | Default | Description |
|---|---|---|---|
| `path` | string or list | — | URL prefix(es) the service is mounted at. Required. |
| `enabled` | boolean | `true` | Set `false` to disable the service without removing it. |
| `sessionAware` | boolean | `false` | Enable session support; injects `Session` and `@WsParam(from=SESSION)`. |
| `interceptors` | list of strings | `[]` | Names of interceptor services applied in order. |
| `compression` | boolean | `true` | Enable gzip response compression. |
| `blocking` | boolean | `true` | `true` for blocking handler; `false` for non-blocking. |
| `port` | string | (default HTTP port) | Bind to a named port (`httpprivate`, etc.). |
| `portType` | list of strings | `[]` | Restrict to specific port types. |

```hocon
ws-service {
  path = api/v1/products
  sessionAware = true
  interceptors = [auth-interceptor]
  port = httpprivate
  compression = false
}
```

---

## @WsMethod

Marks a method as an HTTP endpoint.

```java
@WsMethod(
    path        = "/items/{id}",          // URL path pattern; empty = mount at service root
    method      = HttpMethod.GET,         // one or more HTTP methods (default: GET and POST)
    produces    = "application/json",     // Content-Type of the response (default: application/json)
    raw         = false,                  // true = send body as-is, skip JSON serialisation
    description = "Fetch item by ID"      // human-readable description for API tools
)
public Item getItem( @WsParam( from = From.PATH ) String id ) { … }
```

**`method` values:** `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS`.

Multiple methods on one endpoint:

```java
@WsMethod( path = "/submit", method = { HttpMethod.POST, HttpMethod.PUT } )
public Response submit( @WsParam( from = From.BODY ) Payload p ) { … }
```

**Path patterns** use `{paramName}` segments that are bound to `@WsParam(from=PATH)` parameters by name:

```java
@WsMethod( path = "/{category}/{id}", method = HttpMethod.GET )
public Item get(
    @WsParam( from = From.PATH ) String category,
    @WsParam( from = From.PATH ) String id
) { … }
```

---

## @WsParam — parameter sources

```java
@WsParam( from = From.QUERY )           // default
@WsParam( from = From.PATH )
@WsParam( from = From.BODY )
@WsParam( from = From.HEADER )
@WsParam( from = From.SESSION )
@WsParam( from = From.COOKIE )
```

| `From` | HTTP source | Notes |
|---|---|---|
| `QUERY` | `?name=value` | Default when `@WsParam` is omitted. Supports `List<T>` for multi-value. |
| `PATH` | `/{name}` in path pattern | Must match a `{name}` segment in `@WsMethod.path`. |
| `BODY` | Request body | `String`, `byte[]`, `InputStream`, or any POJO (JSON-deserialized). |
| `HEADER` | HTTP request header | `camelCaseName` is mapped to `X-Camel-Case-Name` automatically. |
| `SESSION` | Session map | Requires `sessionAware = true`. |
| `COOKIE` | HTTP cookie | Matched by parameter name or `name` attribute. |

**`name` attribute** — alternative names searched in order:

```java
@WsParam( from = From.HEADER, name = { "X-Auth-Token", "Authorization" } ) String token
```

**`description` attribute** — used by API descriptor tools; no runtime effect.

### Implicit injection

Parameters typed as `HttpServerExchange` or `Session` are injected automatically without any annotation:

```java
public Response handle( HttpServerExchange exchange, Session session ) { … }
```

### Type coercion

| Parameter type | Behaviour |
|---|---|
| `String` | Raw string value |
| primitives (`int`, `long`, `boolean`, …) | Parsed from string |
| `enum` | `Enum.valueOf()` from string |
| `Optional<T>` | `Optional.empty()` when missing; never throws for absent values |
| `List<String>` | All values for a repeated query parameter |
| any other POJO | JSON-deserialized from the string or body |

A missing required (non-`Optional`) parameter throws `WsClientException` (400).

---

## Return types and Response

The framework converts method return values to HTTP responses automatically:

| Return type | HTTP status | Body |
|---|---|---|
| `void` | 204 No Content | — |
| `Response` | as set on the object | as set on the object |
| `Optional<T>` | 200 if present, 404 if empty | JSON-serialized value |
| `Result<T, E>` | 200 if success, 500 if failure | JSON-serialized value or error |
| `Stream<T>` | 200 | Streaming JSON array |
| any POJO / `Collection` | 200 | JSON-serialized |

`@WsMethod(raw = true)` sends a `String` body without JSON escaping — useful for pre-serialized content.

### Response builder

Use `Response` when you need full control over status, headers, or cookies:

```java
import oap.ws.Response;

// factory methods
Response.ok()                         // 200
Response.noContent()                  // 204
Response.notFound()                   // 404
Response.jsonOk()                     // 200 + Content-Type: application/json
Response.redirect( "/new/location" )  // 302 + Location header

// builder
return Response.ok()
    .withBody( myObject )
    .withContentType( "application/json" )
    .withHeader( "X-Request-Id", requestId )
    .withCookie( new Cookie( "SID", sessionId ) )
    .withStatusCode( 201 )
    .withReasonPhrase( "Created" );
```

---

## Interceptors

Interceptors run before and after each endpoint invocation. They are applied in the order listed in `ws-service.interceptors`; `after()` is called in reverse order.

```java
import oap.ws.interceptor.Interceptor;
import oap.ws.InvocationContext;
import oap.ws.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Optional<Response> before( InvocationContext ctx ) {
        String token = ctx.exchange.getRequestHeader( "Authorization" );
        if( !isValid( token ) ) {
            return Optional.of( new Response( 401, "Unauthorized" ) );
        }
        return Optional.empty();   // continue to next interceptor / method
    }

    @Override
    public void after( Response response, InvocationContext ctx ) {
        response.withHeader( "X-Processed-By", "auth-interceptor" );
    }
}
```

**`InvocationContext` fields:**

| Field | Type | Description |
|---|---|---|
| `exchange` | `HttpServerExchange` | Full request/response exchange |
| `session` | `Session` | Current session (null if not session-aware) |
| `method` | `Reflection.Method` | Reflected method about to be invoked |

Helper: `ctx.getParameter("name")` returns a parsed parameter by name.

**Registration:**

```hocon
services {
  auth-interceptor {
    implementation = com.example.AuthInterceptor
  }

  my-ws {
    implementation = com.example.MyWS
    ws-service {
      path = api/my
      interceptors = [auth-interceptor]
    }
  }
}
```

---

## Session handling

**Enable sessions** in the `ws-service` block:

```hocon
ws-service {
  path = api/my
  sessionAware = true
}
```

When enabled, the framework reads the `SID` cookie on each request, creates a session if none exists, and sets the cookie on the response.

**Inject the whole session object:**

```java
public Response handle( Session session ) {
    session.set( "userId", "u-123" );
    Optional<String> uid = session.get( "userId" );
    session.remove( "userId" );
    session.invalidate();   // clear all entries
    return Response.ok();
}
```

**Inject a single session value:**

```java
public Response secured(
    @WsParam( from = From.SESSION ) Optional<String> userId
) {
    if( userId.isEmpty() ) return new Response( 401, "Not authenticated" );
    …
}
```

**Configure `SessionManager`** in `application.conf`:

```hocon
services {
  oap-ws.session-manager.parameters {
    expirationTime = 12h          # idle expiry (default: 24h)
    cookiePath     = "/"          # cookie Path attribute (default: "/")
    cookieDomain   = "example.com" # cookie Domain attribute (optional)
    cookieSecure   = true         # Secure flag; omit for HTTP-only environments
  }
}
```

---

## Validation

### @WsValidate

Runs a named validator bean against a parameter or the whole method invocation. Validation errors produce a 400 response with an `errors` array.

```java
public Response create(
    @WsParam( from = From.BODY ) @WsValidate( "product-validator" ) Product product
) { … }
```

The validator bean is a kernel service that implements `MethodValidator`.

### @WsValidateJson

Validates the raw JSON body against a named JSON Schema before deserialization.

```java
public Response create(
    @WsParam( from = From.BODY ) @WsValidateJson( schema = "product-schema" ) Product product
) { … }
```

`ignoreRequired = true` skips required-field checks (useful for PATCH).

### @WsPartialValidateJson

For `PATCH` endpoints — merges the incoming partial JSON with an existing object fetched by a helper method, then validates the merged result.

```java
public Response patch(
    @WsParam( from = From.PATH ) String id,
    @WsParam( from = From.BODY )
    @WsPartialValidateJson(
        schema          = "product-schema",
        methodName      = "findById",
        idParameterName = "id",
        path            = "$.product"
    ) Product patch
) { … }
```

---

## Error handling

| Throw / return | HTTP status | Body |
|---|---|---|
| `WsClientException( message )` | 400 | `{ "errors": ["message"] }` |
| `WsClientException( message, errors )` | 400 | `{ "errors": [ … ] }` |
| `WsClientException( message, code, errors )` | `code` | `{ "errors": [ … ] }` |
| any other unchecked exception | 500 | error details |
| `Response.notFound()` | 404 | — |

```java
public Response update( @WsParam( from = From.PATH ) String id,
                        @WsParam( from = From.BODY ) Product patch ) {
    if( !exists( id ) )
        throw new WsClientException( "product not found: " + id );

    List<String> errors = validate( patch );
    if( !errors.isEmpty() )
        throw new WsClientException( "validation failed", errors );

    save( id, patch );
    return Response.noContent();
}
```

---

## OAP Module Integration

A complete worked example: two web services sharing an interceptor, one session-aware.

**`oap-module.oap`:**

```hocon
name = my-app
dependsOn = [oap-ws]

services {

  auth-interceptor {
    implementation = com.example.AuthInterceptor
  }

  catalog-ws {
    implementation = com.example.CatalogWS
    parameters {
      catalogService = <modules.this.catalog-service>
    }
    ws-service {
      path = api/v1/catalog
      interceptors = [auth-interceptor]
    }
  }

  account-ws {
    implementation = com.example.AccountWS
    parameters {
      accountService = <modules.this.account-service>
    }
    ws-service {
      path = api/v1/account
      sessionAware = true
      interceptors = [auth-interceptor]
    }
  }
}
```

**`CatalogWS.java`:**

```java
public class CatalogWS {
    private final CatalogService catalogService;

    public CatalogWS( CatalogService catalogService ) {
        this.catalogService = catalogService;
    }

    @WsMethod( path = "/", method = HttpMethod.GET )
    public List<Item> list( @WsParam( from = From.QUERY ) Optional<String> category ) {
        return catalogService.list( category );
    }

    @WsMethod( path = "/{id}", method = HttpMethod.GET )
    public Optional<Item> get( @WsParam( from = From.PATH ) String id ) {
        return catalogService.find( id );
    }

    @WsMethod( path = "/{id}", method = HttpMethod.DELETE )
    public Response delete( @WsParam( from = From.PATH ) String id ) {
        catalogService.delete( id );
        return Response.noContent();
    }
}
```

**URL mapping:**

```
GET    /api/v1/catalog/              → list()
GET    /api/v1/catalog/{id}          → get()
DELETE /api/v1/catalog/{id}          → delete()
GET    /api/v1/account/…             → AccountWS endpoints
```

Override `SessionManager` parameters in `application.conf`:

```hocon
services {
  oap-ws.session-manager.parameters.expirationTime = 8h
}
```
