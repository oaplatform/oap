# oap-ws-admin-ws

Built-in administration endpoints for OAP applications. All endpoints are mounted on the `httpprivate` port (8081 by default) and are not exposed to public traffic.

Depends on: `oap-ws`

## Menu

- [Log level control](#log-level-control--get-systemadminlogs)
- [JPath query](#jpath-query--get-systemadminjpath)
- [JSON schema](#json-schema--get-systemadminschema)
- [Inspector UI](#inspector-ui--get-systemadmininspector)

## Endpoints

### Log level control — `GET /system/admin/logs`

| Endpoint | Description |
|---|---|
| `GET /system/admin/logs/` | Returns all loggers with an explicitly set level as `{ "loggerName": "LEVEL" }` |
| `GET /system/admin/logs/reset` | Reloads Logback configuration from `logback.xml` on the classpath |
| `GET /system/admin/logs/{level}/{packageName}` | Sets the Logback level for the given logger name |

**Level values:** `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`

```bash
# list all explicit logger levels
curl http://localhost:8081/system/admin/logs/

# raise debug for a package at runtime
curl http://localhost:8081/system/admin/logs/DEBUG/com.example.mypackage

# reset to logback.xml defaults
curl http://localhost:8081/system/admin/logs/reset
```

---

### JPath query — `GET /system/admin/jpath`

Evaluates a [JPath](../../oap-jpath/) expression against the live Kernel service map and returns the result as JSON.

```
GET /system/admin/jpath/?query=my-module.my-service.someField
```

Returns the value of `someField` on the `my-service` service instance inside `my-module`. Returns 400 if the module or service name is unknown.

```bash
curl "http://localhost:8081/system/admin/jpath/?query=oap-ws.session-manager.expirationTime"
```

---

### JSON schema — `GET /system/admin/schema`

Returns a pretty-printed JSON Schema document by classpath path.

```
GET /system/admin/schema/?path=/oap/ws/file/schema/data.conf
```

---

### Inspector UI — GET /system/admin/inspector

Browsable HTML UI over the live Kernel service tree, built on the same JPath query engine as `/system/admin/jpath`.

| Endpoint | Description |
|---|---|
| `GET /system/admin/inspector/ui` | Lists all `module.service` names as links, with a client-side filter box |
| `GET /system/admin/inspector/ui/{moduleName}.{serviceName}` | Service details (implementation, enabled, dependsOn, supervision, listen, link, parameters) plus fields and methods tables, drillable via the value page |
| `GET /system/admin/inspector/ui/value?query=...&mode=inspect\|json` | Evaluates a JPath query (same grammar as `/system/admin/jpath`). `mode=inspect` (default) shows fields/methods tables for the resulting object when it's not a leaf value (String/primitive/number); `mode=json` pretty-prints the JSON result via `Binder.json.marshal`. Either mode shows the stack trace if evaluation throws |

```bash
# service list with filter box
curl http://localhost:8081/system/admin/inspector/ui

# inspect a single service
curl http://localhost:8081/system/admin/inspector/ui/oap-ws.session-manager

# evaluate a JPath query, pretty-printed
curl "http://localhost:8081/system/admin/inspector/ui/value?query=oap-ws.session-manager.expirationTime"
```

---

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-ws-admin-ws]
```

The module self-registers all three services on the `httpprivate` port. No additional configuration is required.

To override the port name for any service, add to `application.conf`:

```hocon
services {
  oap-ws-admin-ws.ws-log.ws-service.port = httpprivate
}
```
