# oap-pnio-v3

High-performance non-blocking HTTP pipeline for the OAP platform. Routes requests through a chain of `ComputeTask` (blocking) and `AsyncTask` (non-blocking) steps with fixed-size memory buffers, timeouts, and a lifecycle tracked by `ProcessState`.

Depends on: `oap-http`

## Architecture

```
HTTP request
    │
    ▼
PnioHttpHandler ──► PnioExchange (request buffer, response buffer, state)
    │
    ▼
ComputeTask / AsyncTask  ──►  ComputeTask / AsyncTask  ──►  …
    │
    ▼
PnioListener  (onDone / onTimeout / onException / …)
    │
    ▼
HTTP response
```

## `PnioHttpHandler`

Entry point that receives Undertow requests and drives the pipeline. Configure it in `oap-module.oap`:

```hocon
services {
  my-pnio-handler {
    implementation = com.example.MyPnioHandler
    parameters {
      server       = <modules.oap-http.oap-http-server>
      requestSize  = 65536   # 64 KiB default
      responseSize = 32768   # 32 KiB default
      important    = false   # log WARN instead of DEBUG on overflow/rejection
    }
  }
}
```

| Field | Default | Description |
|---|---|---|
| `requestSize` | `65536` (64 KiB) | Size of the pre-allocated request buffer |
| `responseSize` | `32768` (32 KiB) | Size of the pre-allocated response buffer |
| `important` | `false` | Escalate buffer overflow and rejection log messages to WARN |

## `PnioExchange`

Carries the request, the response, and the current `ProcessState` through the pipeline. Each task reads from and writes to the same exchange object.

### `ProcessState`

| State | Meaning |
|---|---|
| `RUNNING` | Pipeline is actively processing |
| `DONE` | Processing completed successfully |
| `TIMEOUT` | Deadline exceeded before completion |
| `EXCEPTION` | Unhandled exception in a task |
| `INTERRUPTED` | Processing thread was interrupted |
| `REJECTED` | Request was rejected (e.g., queue full) |
| `REQUEST_BUFFER_OVERFLOW` | Incoming request body exceeded `requestSize` |
| `RESPONSE_BUFFER_OVERFLOW` | Outgoing response body exceeded `responseSize` |

Transition methods on `PnioExchange`:

```java
exchange.complete();           // → DONE
exchange.fail( exception );    // → EXCEPTION
```

## Tasks

### `ComputeTask`

Blocking, CPU-bound step. Runs on a worker thread.

```java
@FunctionalInterface
public interface ComputeTask<RequestState> {
    void run( PnioExchange<RequestState> exchange );
}
```

```java
ComputeTask<MyState> deserialize = exchange -> {
    MyState state = Json.unmarshal( exchange.getRequestBytes(), MyState.class );
    exchange.setRequestState( state );
};
```

### `AsyncTask`

Non-blocking, I/O-bound step. Returns a `CompletableFuture` so the thread is not held while waiting.

```java
@FunctionalInterface
public interface AsyncTask<T, RequestState> {
    CompletableFuture<T> apply( PnioExchange<RequestState> exchange );
}
```

```java
AsyncTask<String, MyState> fetchData = exchange ->
    httpClient.sendAsync( buildRequest( exchange.getRequestState() ), ofString() )
              .thenApply( HttpResponse::body );
```

## `PnioListener`

Callback interface invoked after the pipeline finishes. Implement it to write the HTTP response.

```java
public interface PnioListener<RequestState> {
    void onDone( PnioExchange<RequestState> exchange );
    void onTimeout( PnioExchange<RequestState> exchange );
    void onException( PnioExchange<RequestState> exchange );
    void onRequestBufferOverflow( PnioExchange<RequestState> exchange );
    void onResponseBufferOverflow( PnioExchange<RequestState> exchange );
    void onRejected( PnioExchange<RequestState> exchange );
    void onUnknown( PnioExchange<RequestState> exchange );
}
```

Use `PnioListenerDefault` for a base implementation that handles all error states with a standard HTTP error response, then override only `onDone`.

Use `PnioListenerNoContent` when the response body is always empty (e.g., 204 No Content endpoints).

## Example

```java
public class MyPnioHandler extends PnioHttpHandler<MyState> {

    public MyPnioHandler( NioHttpServer server ) {
        super( server );
        server.bind( "/api/process", this );
    }

    @Override
    protected List<Object> buildTasks() {
        return List.of(
            (ComputeTask<MyState>) this::deserialize,
            (AsyncTask<String, MyState>) this::callUpstream,
            (ComputeTask<MyState>) this::serialize
        );
    }

    @Override
    protected PnioListener<MyState> buildListener() {
        return new PnioListenerDefault<>() {
            @Override
            public void onDone( PnioExchange<MyState> exchange ) {
                exchange.responseJson( exchange.getRequestState().result );
            }
        };
    }
}
```
