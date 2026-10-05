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
| `ValidationErrors.error/errors/create( int, … )` | `ValidationErrors.error( 400, "bad" )` | `400` |
| `new WsClientException( message, int, errors )` | `new WsClientException( "x", 403, List.of() )` | `403` |
| `oap.ws.Response` constructor or `withStatusCode( int )` | `new Response( 409 )` | `409` |
| `oap.ws.Response.build401()` / `build403()` / `build404()` | `Response.build403().build()` | `401` / `403` / `404` |
| `oap.http.Response` constructor | `new oap.http.Response( url, 409, … )` | `409` |

Rules:

- Only codes `>= 400` are reported. `200`, `204` and `302` from `Response.ok()`, `noContent()` and `redirect()` are dropped.
- `401` points to the shared `UnauthorizedError` response component (it carries the `WWW-Authenticate` header). Other codes use `application/json` with the `ErrorResponse` schema. The description is the reason phrase (`400 Bad Request`, `403 Forbidden`, …); codes without a known phrase show `HTTP <code>`.
- Helper methods called from the scanned code are followed up to 5 levels deep, within the endpoint's class hierarchy and the `Response` classes.
- A code that is not a literal (held in a local variable or computed at runtime) is skipped and logged as a warning.
- A class processed under two contexts is generated once (class-name dedup), so its codes come from the first context only.

Example: an endpoint with a `400` validator and an interceptor that returns `401`:

```json
"responses": {
  "200": { "description": "" },
  "400": {
    "description": "Bad Request",
    "content": { "application/json": { "schema": { "$ref": "#/components/schemas/ErrorResponse" } } }
  },
  "401": { "$ref": "#/components/responses/UnauthorizedError" }
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
