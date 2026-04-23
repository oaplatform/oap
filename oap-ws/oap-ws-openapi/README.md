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

Methods annotated with `@OpenapiIgnore` are excluded from the output.

## Key classes

| Class | Description |
|---|---|
| `OpenapiGenerator` | Builds an `OpenAPI` model from a `WebServices` instance |
| `WebServicesWalker` | Iterates registered services and calls a `WebServiceVisitor` |
| `WebServiceVisitor` | Visitor interface — implement to consume endpoint metadata |
| `OpenApiContentWriter` | Serialises the `OpenAPI` model to JSON or YAML |
| `ApiInfo` | Value object for the OpenAPI `info` block (title, description, version) |
| `OpenapiSchema` | Utilities for schema resolution and `$ref` generation |

## Usage

This module is a library — use `oap-ws-openapi-ws` to serve the spec at runtime, or `oap-ws-openapi-maven-plugin` to generate it at build time.

To use the generator programmatically:

```java
OpenapiGenerator generator = new OpenapiGenerator( webServices, apiInfo );
OpenAPI spec = generator.generate();
String yaml = Yaml.pretty( spec );
```

## Customising the output

**Exclude an endpoint** from the spec:

```java
@WsMethod( path = "/probe", method = HttpMethod.GET )
@OpenapiIgnore
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
