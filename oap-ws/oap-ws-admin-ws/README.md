# oap-ws-admin-ws

Built-in administration endpoints for OAP applications. All endpoints are mounted on the `httpprivate` port (8081 by default) and are not exposed to public traffic.

Depends on: `oap-ws`

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
