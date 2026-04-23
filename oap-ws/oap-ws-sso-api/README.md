# oap-ws-sso-api

SSO (Single Sign-On) contracts and interceptors for OAP web services. Provides JWT-based authentication, API-key authentication, brute-force throttling, role-based permission checks, and the `@WsSecurity` annotation.

## Annotation

### `@WsSecurity`

Marks a `@WsMethod` as requiring authentication and specific permissions.

```java
@WsMethod( path = "/{id}", method = HttpMethod.DELETE )
@WsSecurity( realm = WsSecurity.SYSTEM, permissions = { "ADMIN", "SUPERUSER" } )
public Response delete( @WsParam( from = From.PATH ) String id ) { … }
```

| Attribute | Default | Description |
|---|---|---|
| `realm` | `"SYSTEM"` | Authentication realm. Built-in values: `SYSTEM`, `USER`. Can also be a method parameter name — the realm value is then resolved dynamically from that parameter at invocation time. |
| `permissions` | — | One or more permission strings the authenticated user must hold. Required. |

The `JWTSecurityInterceptor` short-circuits with **401** if no valid token is present, or **403** if the user lacks the required permissions.

---

## Interceptors

All interceptors implement `oap.ws.interceptor.Interceptor` and are registered as kernel services.

### `JWTSecurityInterceptor`

Validates JWT access tokens and enforces `@WsSecurity` permission checks.

- Reads the access token from the `Authorization` header (Bearer) or a session cookie.
- On success, stores the authenticated `User` in the session under `SESSION_USER_KEY`.
- On token expiry, attempts to refresh using the refresh token cookie.
- Passes silently if the invoked method has no `@WsSecurity` annotation.

**Registration in `oap-module.oap`:**

```hocon
services {
  my-ws {
    implementation = com.example.MyWS
    ws-service {
      path = api/v1/my
      sessionAware = true
      interceptors = [oap-ws-sso-jwt-security-interceptor]
    }
  }
}
```

### `ApiKeyInterceptor`

Authenticates requests by `?accessKey=…&apiKey=…` query parameters.

- If both parameters are present and valid, the user is placed in the session for the duration of the request and removed in `after()`.
- Returns **409 Conflict** if the session already has a logged-in user.
- Returns **401 Unauthorized** if the key pair is invalid.
- Passes silently if neither `accessKey` nor `apiKey` is present.

### `ThrottleLoginInterceptor`

Prevents brute-force attacks on login endpoints by enforcing a per-IP delay between failed attempts (default: 5 seconds).

- Tracks failed attempts in an in-memory `ConcurrentHashMap`.
- Returns **403 Forbidden** if a request arrives before the cooldown period expires.
- The delay is configurable via the `delay` parameter.

---

## Services

The module registers these services in its `oap-module.oap`:

| Service | Implementation | Description |
|---|---|---|
| `oap-ws-sso-roles-provider` | `ConfigSecurityRolesProvider` | Loads role→permission mappings from HOCON config |
| `oap-ws-sso-roles` | `SecurityRoles` | Role lookup used by `JWTSecurityInterceptor` |
| `oap-ws-sso-user-provider` | `UserProvider` | Remote reference to the user account service |
| `oap-ws-sso-api-key-interceptor` | `ApiKeyInterceptor` | API-key authentication interceptor |
| `oap-ws-sso-throttle-login-interceptor` | `ThrottleLoginInterceptor` | Login throttle interceptor |
| `oap-ws-sso-jwt-security-interceptor` | `JWTSecurityInterceptor` | JWT token validation + permission check interceptor |

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-ws-sso-api]
```

Configure roles in `application.conf`:

```hocon
services {
  oap-ws-sso-api.oap-ws-sso-roles-provider.parameters.roles {
    ADMIN      = [READ, WRITE, DELETE]
    SUPERUSER  = [READ, WRITE, DELETE, MANAGE]
    VIEWER     = [READ]
  }
}
```
