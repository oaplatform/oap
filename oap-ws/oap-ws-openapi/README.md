# oap-ws-openapi

Core OpenAPI 3.x generation library for OAP web services. Walks the live `WebServices` registry via reflection and produces an `OpenAPI` model without requiring Swagger annotations on service classes.

Depends on: `oap-ws`

## What it generates

- One OpenAPI **path** per `@WsMethod` endpoint
- **Parameters** derived from `@WsParam` (query, path, header, cookie)
- **Request body** for `BODY` parameters with JSON schema
- **Response schema** inferred from the method return type
- **Security requirements** from `@WsSecurity` annotations
- **Tags** from the service class name
- **Error responses** for every error code an endpoint can return (see below)

Methods annotated with `@OpenApiIgnore` are excluded from the output.

Bean fields and getters annotated with `@OpenApiIgnore` are left out of the generated schemas:

```java
public class Account {
    public String name;

    @OpenApiIgnore
    public String internalId;

    @OpenApiIgnore
    public String getSecret() { return secret; }
}
```

The annotation lives in the `oap-ws-openapi-annotations` module.

## Key classes

| Class | Description |
|---|---|
| `OpenapiGenerator` | Builds an `OpenAPI` model: `processWebservice( clazz, context[, interceptors] )` per service, then `build()` |
| `ErrorCodeScanner` | Reads the error codes of a method and its validators and interceptors from bytecode (ASM) |
| `WebServicesWalker` | Walks the module configs (`oap-module.oap`) and calls a `WebServiceVisitor` per `ws-service`, resolving its interceptors |
| `WebServiceVisitor` | Visitor interface: `visit( WsConfig, Class, basePath, interceptors )`, implemented by the Maven plugin |
| `OpenApiContentWriter` | Serialises the `OpenAPI` model to JSON or YAML |
| `ApiInfo` | Value object for the OpenAPI `info` block (title, description, version) |
| `OpenapiSchema` | Utilities for schema resolution and `$ref` generation |

## Usage

This module is a library. Use `oap-ws-openapi-ws` to serve the spec at runtime, or `oap-ws-openapi-maven-plugin` to generate it at build time.

To use the generator programmatically:

```java
OpenapiGenerator generator = new OpenapiGenerator( "My API", "Public API" );
generator.beforeProcesingServices();
generator.processWebservice( MyWS.class, "my-context" );
generator.processWebservice( OtherWS.class, "other", List.of( MyInterceptor.class ) );
generator.afterProcesingServices();
OpenAPI spec = generator.build();
String yaml = Yaml.pretty( spec );
```

Each `processWebservice` call adds one service. The optional third argument lists the service's interceptor classes (see [Interceptors](#interceptors)).

## Error responses

For each operation the generator adds one response per error code the endpoint can return. The codes are read from the compiled bytecode with ASM, so nothing needs to be annotated.

Scanned code: the endpoint method, the methods named in `@WsValidate` (on the method or on its parameters), and the `before` method of each interceptor.

| Code in scanned code | Example | Reported as |
|---|---|---|
| `ValidationErrors.statusCode( int ).error/errors/create(…)…endCode()` | `ValidationErrors.empty().statusCode( 400 ).error( "bad" ).endCode()` | `400` |
| `new WsClientException( message, int, errors )` | `new WsClientException( "x", 403, List.of() )` | `403` |
| `oap.ws.Response` constructor or `withStatusCode( int )` | `new Response( 409 )` | `409` |
| `oap.ws.Response.build401()` / `build403()` / `build404()` | `Response.build403().build()` | `401` / `403` / `404` |
| `oap.http.Response` constructor | `new oap.http.Response( url, 409, … )` | `409` |
| `statusCode( 400 ).error( E.X )` where `E` is a `ValidationMessage` enum | `NAME_REQUIRED( "1000010", "name is required" )` | `400`, code `"1000010"` / `"name is required"` |

An enum constant's code and text come from its constructor arguments `(String code, String message)`, read from the enum's static initializer. They must be literals; otherwise the message is skipped with a warning.

Rules:

- Only codes `>= 400` are reported. `200`, `204` and `302` from `Response.ok()`, `noContent()` and `redirect()` are dropped.
- `401` points to the shared `UnauthorizedError` response component (it carries the `WWW-Authenticate` header) *only when no messages were scanned for it*. When messages are found, `401` gets a local response like every other code, with the `WWW-Authenticate` header added back locally. Other codes use `application/json` with the `ErrorResponse` schema. The description is the reason phrase (`400 Bad Request`, `403 Forbidden`, …); codes without a known phrase show `HTTP <code>`.
- Helper methods called from the scanned code are followed up to 5 levels deep, within the endpoint's class hierarchy, the classes in the same package, and the `Response` classes. A **static** call into a class outside the `oap.` namespace is also followed — this supports a user's own shared validation helpers living in their own package — except into the JDK (`java.*`/`javax.*`) or this module's known third-party dependencies (Swagger, ASM, Jackson, Guava, Apache Commons, Lombok), which are never followed since they can't produce an error code anyway. A non-static call into another package is still not followed.
- A code that is not a literal (held in a local variable or computed at runtime) is skipped and logged as a warning.
- A class processed under two contexts is generated once (class-name dedup), so its codes come from the first context only.

### Messages in examples

Each validation message is also scanned. The `application/json` media type of every error response (except `401` with no scanned messages, see below) gets an `example` with the messages of that status code, in the shape of the real validation body `{"messages": [{"code", "message"}]}`:

| Code in scanned code | Example message |
|---|---|
| `statusCode( 400 ).error( E.X )`, `E.X( "1000001", "a" )` | `{ "code": "1000001", "message": "a" }` |
| `statusCode( 400 ).error( "a" )` | `{ "message": "a" }` (no `code`) |
| `statusCode( 400 ).error( "item " + id, Map.of() )` | `{ "message": "<runtime message>" }` |
| `statusCode( 404 ).error( "item ${id} not found", Map.of( "id", id ) )` | `{ "message": "item ${id} not found" }` |

Rules:

- A message has a code only when it is an enum constant (`ValidationMessage`). The code and text are literals in the constant's constructor; otherwise the message is skipped with a warning.
- A text that is not a literal is shown as the placeholder `<runtime message>`.
- Formatted messages (`error( text, Map )`) show their template text, with the `${name}` placeholders as written, since the rendered text is only known at runtime.
- Messages from the endpoint and from its interceptors are merged per status code. Duplicates are dropped.
- Messages in an example are sorted by code, ascending. Messages without a code come last; ties are sorted by text.
- Messages added through lists (`error(List)`, `errors(List)`, `pairs(...)`, `errors(Integer, List)`) are not listed, since their count is unknown at scan time. The status code is still reported.
- The receiver must be the chained form `statusCode( x ).error(...)`. A builder held in a local variable is skipped with a warning.
- `401` stays a `$ref` to `UnauthorizedError`, which cannot carry a per-operation example, only when no messages were scanned for it. Otherwise it gets a local response and example like any other code, with the `WWW-Authenticate` header re-added.
- The shared `ErrorResponse` schema (`statusCode`, `error`, `messages`) does not match the validation body `{"messages": [...]}`. The examples follow the real body; the schema is unchanged.

Example: an endpoint with a `400` validator (messages `"1000001" "a"` and a runtime text) and an interceptor that returns `401` with no message (e.g. `Response.build401()`):

```json
"responses": {
  "200": { "description": "" },
  "400": {
    "description": "Bad Request",
    "content": {
      "application/json": {
        "schema": { "$ref": "#/components/schemas/ErrorResponse" },
        "example": { "messages": [ { "code": "1000001", "message": "a" }, { "message": "<runtime message>" } ] }
      }
    }
  },
  "401": { "$ref": "#/components/responses/UnauthorizedError" }
}
```

When a `401` message is scanned instead (e.g. `statusCode( 401 ).error( "token expired" )`), it gets a local response instead of the `$ref`, with the header re-added:

```json
"401": {
  "description": "Unauthorized",
  "headers": {
    "WWW-Authenticate": {
      "description": "Defines the authentication method that should be used.",
      "schema": { "type": "string" }
    }
  },
  "content": {
    "application/json": {
      "schema": { "$ref": "#/components/schemas/ErrorResponse" },
      "example": { "messages": [ { "message": "token expired" } ] }
    }
  }
}
```

## Interceptors

Interceptors listed in `ws-service.interceptors` can end a request early with an error response, so their codes belong in the spec too. The generator scans each interceptor's `before( InvocationContext )` method and adds its codes to every operation of the service.

- Maven plugin: the walker resolves `ws-service.interceptors` against all module configs, so interceptors in other modules (for example `oap-ws-sso-api`) are found.
- Runtime (`/system/openapi`): `WebServices` keeps the interceptor instances per context and passes their classes to the generator.

`after` is not scanned, since it cannot change the status of the response.

## Customising the output

**Exclude an endpoint** from the spec:

```java
@WsMethod( path = "/probe", method = HttpMethod.GET )
@OpenApiIgnore
public String healthProbe() { return "ok"; }
```

**Document parameters** with `@WsParam(description = "…")`:

```java
@WsParam( from = From.QUERY, description = "Filter by status" ) String status
```

**Document endpoints** with `@WsMethod(description = "…")`:

```java
@WsMethod( path = "/items", method = HttpMethod.GET, description = "List all items" )
public List<Item> list() { … }
```
