# oap-ws-api-ws

Exposes the live OAP web service registry as a JSON HTTP endpoint. Useful for service discovery, documentation generation, and debugging which endpoints are registered and with what parameters.

Depends on: `oap-ws`

## Endpoint

```
GET /system/api
```

Returns a JSON array describing every registered web service — class name, mount path, HTTP methods, parameter sources, and `@WsSecurity` permission requirements.

```bash
curl http://localhost:8080/system/api
```

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-ws-api-ws]
```

The `api-ws` service is registered automatically. No additional configuration is needed.

The endpoint is served on the default HTTP port. To move it to the private port:

```hocon
services {
  oap-ws-api-ws.api-ws.ws-service.port = httpprivate
}
```

## Comparison with oap-ws-openapi-ws

`oap-ws-api-ws` uses the OAP-native `Info` format. `oap-ws-openapi-ws` generates an OpenAPI 3.x document. Use `oap-ws-openapi-ws` when you need standard tooling (Swagger UI, code generators); use `oap-ws-api-ws` for lightweight internal introspection.
