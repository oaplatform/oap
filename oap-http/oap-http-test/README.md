# oap-http-test

TestNG test utilities for OAP HTTP services. Provides a fluent assertion API for HTTP responses, a stub `HttpServerExchange` for unit-testing handlers without a live server, and `MockHttpContext` for legacy Apache HTTP context tests.

Depends on: `oap-http`

## `HttpAsserts`

Fluent HTTP client and assertion builder backed by `OapHttpClient`. Use its static methods to issue requests and assert on the response in a single chain.

### Making requests

```java
import static oap.http.test.HttpAsserts.*;

// GET with query parameters
assertGet( httpUrl( port, "/api/items" ), Map.of( "page", 1 ), Map.of() )
    .isOk()
    .respondedJson( """
        [{"id":1},{"id":2}]
        """ );

// POST with a string body
assertPost( httpUrl( port, "/api/items" ), """
    {"name":"Widget"}
    """, "application/json" )
    .isOk();

// PUT
assertPut( httpUrl( port, "/api/items/1" ), "{}", "application/json" )
    .hasCode( 204 );

// DELETE
assertDelete( httpUrl( port, "/api/items/1" ) )
    .hasCode( 204 );
```

### `HttpAssertion` methods

| Method | Description |
|---|---|
| `isOk()` | Assert HTTP 200 |
| `hasCode( int )` | Assert exact status code |
| `codeIsIn( int... )` | Assert status code is one of the given values |
| `hasReason( String )` | Assert reason phrase (e.g., `"OK"`) |
| `reasonContains( String )` | Assert reason phrase contains substring |
| `hasContentType( String )` | Assert `Content-Type` header value |
| `hasBody( String )` | Assert exact response body |
| `bodyContains( String )` | Assert body contains substring |
| `bodyContainsPattern( String )` | Assert body matches regex |
| `containsHeader( String, String )` | Assert header name and value |
| `containsHeader( String )` | Assert header is present |
| `doesNotContainHeader( String )` | Assert header is absent |
| `containsCookie( String )` | Assert `Set-Cookie` by raw value |
| `containsCookie( Cookie )` | Assert `Set-Cookie` by `Cookie` object |
| `respondedJson( int, String, String )` | Assert code, reason, and JSON body |
| `respondedJson( String )` | Assert 200 OK with JSON body |
| `respondedJson( Class<?>, String )` | Assert 200 OK with JSON from test resource file |
| `unmarshal( Class<T> )` | Parse response body as JSON into `T` |
| `body()` | Return `StringAssertion` for the body |
| `satisfies( Consumer<Response> )` | Assert via custom lambda |

### URL helpers

```java
String prefix = HttpAsserts.httpPrefix( port );          // "http://localhost:8080"
String url    = HttpAsserts.httpUrl( port, "/api/item" ); // "http://localhost:8080/api/item"
```

### Using a custom HTTP client

All assertion methods have an overload accepting a `org.eclipse.jetty.client.HttpClient` for tests that require custom settings (e.g., bearer tokens, custom timeouts):

```java
HttpClient client = OapHttpClient.customHttpClient()
    .connectionTimeout( Duration.ofSeconds( 2 ) )
    .build();

assertGet( client, httpUrl( port, "/secure" ), Map.of(), Map.of( "Authorization", "Bearer token" ) )
    .isOk();
```

## `HttpServerExchangeStub`

A fully constructed `io.undertow.server.HttpServerExchange` that can be used in unit tests without starting a real server. Useful for testing `HttpHandler` implementations in isolation.

```java
HttpServerExchangeStub stub = new HttpServerExchangeStub( "/api/test?foo=bar" );
myHandler.handleRequest( stub.exchange() );
assertThat( stub.getResponseBody() ).isEqualTo( "expected" );
```

## `MockHttpContext` _(deprecated)_

A minimal `org.apache.http.protocol.HttpContext` backed by a `HashMap`. Kept for compatibility with legacy tests. Prefer the Jetty-based utilities for new code.
