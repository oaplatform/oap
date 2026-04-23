# oap-ws-api-api

Shared contracts for OAP web service API introspection. Contains the data types used by both `oap-ws-api-ws` (runtime service registry) and `oap-ws-openapi` (OpenAPI generator) to describe registered web services.

## Key types

### `Info`

Introspects the live `WebServices` registry and returns structured metadata about every registered endpoint.

```java
Info info = new Info( webServices );
List<Info.WebServiceInfo> services = info.services();
```

Each `WebServiceInfo` exposes:
- Service class name and mount path
- Per-method: HTTP method(s), path pattern, parameter descriptors, `@WsSecurity` permissions

### `@OpenapiIgnore`

Method-level annotation that excludes a `@WsMethod` from OpenAPI generation while keeping it fully functional at runtime.

```java
@WsMethod( path = "/internal", method = HttpMethod.GET )
@OpenapiIgnore
public Response internalEndpoint() { … }
```

Apply this to endpoints that should not appear in the public OpenAPI spec (health checks, internal probes, etc.).
