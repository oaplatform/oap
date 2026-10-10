# oap-ws-sso

Base class for secured OAP web services. Provides a convenience method for checking whether the current user is authenticated.

Depends on: `oap-ws-sso-api`

## `AbstractSecureWS`

Extend this class instead of writing the authentication guard manually:

```java
public class AccountWS extends AbstractSecureWS {

    @WsMethod( path = "/profile", method = HttpMethod.GET )
    @WsSecurity( realm = WsSecurity.USER, permissions = { "READ" } )
    public Response getProfile( @WsParam( from = From.SESSION ) Optional<User> loggedUser ) {
        validateUserLoggedIn( loggedUser ).throwIfInvalid();
        return Response.ok().withBody( loggedUser.get() );
    }
}
```

### `validateUserLoggedIn( Optional<User> loggedUser )`

Returns `ValidationErrors.empty()` when the user is present, or `ValidationErrors.empty().statusCode( 401 ).error( "not logged in" ).endCode()` when the `Optional` is empty.

`throwIfInvalid()` throws a `WsClientException` with the code chosen by `resolvedCode()`. `ValidationErrors.empty().statusCode( 401 ).error( … ).endCode()` takes priority over other codes, as described in `oap-ws/oap-ws/README.md` under "Validation errors".
