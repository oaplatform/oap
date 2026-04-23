# oap-ws-openapi-ws

Serves a generated OpenAPI 3.x specification over HTTP at runtime. No Swagger annotations required — the spec is derived entirely from `@WsMethod`, `@WsParam`, `@WsSecurity`, and `@OpenapiIgnore` annotations via reflection.

Depends on: `oap-ws`, `oap-ws-api-ws`

## Endpoint

```
GET /system/openapi
```

Returns the OpenAPI 3.x YAML document describing all registered web services (except those marked `@OpenapiIgnore`).

```bash
curl http://localhost:8080/system/openapi
```

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-ws-openapi-ws]
```

Configure the API info block in `application.conf`:

```hocon
services {
  oap-ws-openapi-ws {
    openapi-info.parameters {
      title       = "My Service API"
      description = "Public API for My Service"
      version     = "1.0.0"
    }
  }
}
```

## Annotating endpoints

**Document a method:**

```java
@WsMethod( path = "/items", method = HttpMethod.GET, description = "List all items" )
public List<Item> list() { … }
```

**Document a parameter:**

```java
public List<Item> list(
    @WsParam( from = From.QUERY, description = "Filter by category" ) Optional<String> category
) { … }
```

**Exclude an endpoint from the spec:**

```java
@WsMethod( path = "/probe", method = HttpMethod.GET )
@OpenapiIgnore
public String healthProbe() { return "ok"; }
```

**Optional parameters** are automatically marked as not required in the spec; all other parameters are treated as required.

## Comparison with oap-ws-api-ws

| | `oap-ws-openapi-ws` | `oap-ws-api-ws` |
|---|---|---|
| Format | OpenAPI 3.x (OAS) | OAP-native JSON |
| Tooling | Swagger UI, code generators | Internal only |
| Inclusion control | `@OpenapiIgnore` per method | All services |
| Schema | OAS `$ref` components | Simple type names |

Use `oap-ws-openapi-ws` for public APIs; use `oap-ws-api-ws` for lightweight internal introspection.

For **build-time** spec generation, use `oap-ws-openapi-maven-plugin` instead.
