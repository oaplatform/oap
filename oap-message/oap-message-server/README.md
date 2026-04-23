# oap-message-server

HTTP endpoint that receives messages from `MessageSender` clients, deduplicates them by MD5, and dispatches to registered `MessageListener` instances.

Depends on: `oap-http`

## `MessageHttpHandler`

Binds to a configured HTTP path and port. On each POST it:

1. Reads the binary message envelope (type, version, clientId, MD5, payload).
2. Checks `MessageHashStorage` — if the MD5 was already seen within `hashTtl`, replies `STATUS_ALREADY_WRITTEN` without invoking the listener.
3. Looks up the `MessageListener` registered for the message type byte.
4. Calls `listener.run(...)` and returns its `short` status code to the client.
5. On `STATUS_OK`, records the MD5 in the hash cache.

### Parameters

| Parameter | Default | Description |
|---|---|---|
| `context` | `/messages` | URL path to bind |
| `port` | `httpprivate` | Named server port (see `NioHttpServer`) |
| `hashTtl` | `6h` | How long a seen MD5 is remembered for deduplication (`-1` = forever) |
| `clientHashCacheSize` | `1024` | Per-client LRU MD5 cache size |
| `controlStatePath` | — | File path for persisting the hash cache across restarts |
| `listeners` | `[]` | List of `MessageListener` beans to register |

### OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-message-server]

services {
  oap-message-server.oap-http-message-handler.parameters {
    controlStatePath = /opt/oap/messages/server
    hashTtl          = 6h
    listeners        = [<modules.this.my-listener>]
  }

  my-listener {
    implementation = com.example.MyMessageListener
  }
}
```

Alternatively, link listeners from their own service block using `link.listeners`:

```hocon
services {
  my-listener {
    implementation = com.example.MyMessageListener
    link.listeners = <modules.oap-message-server.oap-http-message-handler>
  }
}
```

---

## `MessageListener`

Implement this interface to handle a specific message type.

```java
public interface MessageListener {
    byte getId();     // unique type byte — must not exceed 200 (201–255 are reserved)
    String getInfo(); // human-readable name for logging

    short run( int version, String hostName, int size, byte[] data, String md5 );
}
```

`run` is called exactly once per unique MD5 within `hashTtl`. Return a `STATUS_*` constant from `MessageProtocol`:

| Return value | Effect |
|---|---|
| `STATUS_OK` (0) | Success — MD5 recorded; client marks message delivered |
| `STATUS_UNKNOWN_ERROR` (1) | Transient failure — client will retry |
| `STATUS_UNKNOWN_ERROR_NO_RETRY` (2) | Permanent failure — client drops the message |
| Custom status (3–99) | Mapped to a name via `oap-messages.properties`; client retries |

Throwing an exception from `run` is equivalent to returning `STATUS_UNKNOWN_ERROR_NO_RETRY` — the handler catches it, logs the error, and sends the no-retry status.

### Example

```java
public class OrderMessageListener implements MessageListener {
    public static final byte MESSAGE_TYPE = 0x10;

    @Override public byte getId() { return MESSAGE_TYPE; }
    @Override public String getInfo() { return "order-events"; }

    @Override
    public short run( int version, String hostName, int size, byte[] data, String md5 ) {
        Order order = Binder.json.unmarshal( Order.class, data );
        orderService.process( order );
        return MessageProtocol.STATUS_OK;
    }
}
```

---

## `MessageListenerJson<T>`

Abstract convenience base class that JSON-deserializes the raw `byte[]` payload before calling a typed `run` method. Use it when the sender always serializes with `ContentWriter.ofJson()`.

```java
public class OrderMessageListener extends MessageListenerJson<Order> {

    public OrderMessageListener() {
        super( (byte) 0x10, "order-events", new TypeRef<Order>() {} );
    }

    @Override
    protected short run( int version, String hostName, Order order, String md5 ) {
        orderService.process( order );
        return MessageProtocol.STATUS_OK;
    }
}
```

---

## Custom status codes

Custom status codes (values 3–99) cause the client to retry the message. Assign names to them in `META-INF/oap-messages.properties` for meaningful log and metric labels:

```properties
# Custom retry-triggering statuses
map.quota-exceeded = 50
map.downstream-unavailable = 51
```

Return the numeric value from `run`:

```java
return 50; // client retries; logged as "quota-exceeded"
```
