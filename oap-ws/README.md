# oap-ws

HTTP web service framework for the OAP platform. Provides annotation-driven endpoint declaration, session management, interceptors, validation, OpenAPI generation, SSO/JWT security, and file upload/download — all wired through the OAP Kernel with zero servlet-container boilerplate.

## Sub-modules

| Module                                                    | Description | Depends on |
|-----------------------------------------------------------|---|---|
| [oap-ws](oap-ws/README.md)                                         | Core framework: `@WsMethod`, `@WsParam`, `WebServices`, `SessionManager`, validation | `oap-http` |
| [oap-ws-admin-ws](oap-ws-admin-ws/README.md)                       | Built-in admin endpoints: log level control, JPath queries, JSON schema lookup | `oap-ws` |
| [oap-ws-api-api](oap-ws-api-api/README.md)                         | Shared API descriptor contracts (`Info`, `@OpenapiIgnore`) | `oap-ws` |
| [oap-ws-api-ws](oap-ws-api-ws/README.md)                           | HTTP endpoint that exposes the service registry as JSON (`GET /system/api`) | `oap-ws` |
| [oap-ws-file-ws](oap-ws-file-ws/README.md)                         | File upload and download over HTTP with multi-bucket storage | `oap-ws` |
| [oap-ws-openapi](oap-ws-openapi/README.md)                         | Core OpenAPI 3.x generation library (`OpenapiGenerator`, `WebServicesWalker`) | `oap-ws` |
| [oap-ws-openapi-ws](oap-ws-openapi-ws/README.md)                   | HTTP endpoint that serves the generated OpenAPI spec (`GET /system/openapi`) | `oap-ws`, `oap-ws-api-ws` |
| [oap-ws-openapi-maven-plugin](oap-ws-openapi-maven-plugin/README.md) | Maven plugin to generate `swagger.json` / YAML at build time | — |
| [oap-ws-sso-api](oap-ws-sso-api/README.md)                         | SSO contracts + interceptors: `@WsSecurity`, JWT, API key, throttle-login | — |
| [oap-ws-sso](oap-ws-sso/README.md)                                 | `AbstractSecureWS` base class for secured web services | `oap-ws-sso-api` |
| [oap-ws-test](oap-ws-test/README.md)                               | TestNG assertion helpers for validation errors | `oap-ws` |

---

## Quick start

**1.** Add `oap-ws` to your module's `dependsOn`:

```hocon
name = my-module
dependsOn = [oap-ws]
```

**2.** Annotate your service class and register it with a `ws-service` block:

```java
public class HelloWS {
    @WsMethod( path = "/hello", method = HttpMethod.GET )
    public String hello( @WsParam( from = From.QUERY ) String name ) {
        return "Hello, " + name + "!";
    }
}
```

```hocon
services {
  hello-ws {
    implementation = com.example.HelloWS
    ws-service.path = api
  }
}
```

**3.** The endpoint is available at `GET /api/hello?name=World`.

See the [oap-ws](oap-ws/) module for the full reference.

---

## Optional add-ons

| Need | Add module |
|---|---|
| Runtime API introspection | `oap-ws-api-ws` |
| OpenAPI / Swagger spec served at runtime | `oap-ws-openapi-ws` |
| OpenAPI spec generated at build time | `oap-ws-openapi-maven-plugin` |
| JWT / API-key authentication | `oap-ws-sso-api` |
| File upload / download | `oap-ws-file-ws` |
| Admin (log levels, JPath) | `oap-ws-admin-ws` |
