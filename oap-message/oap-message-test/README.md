# oap-message-test

Test utilities for `oap-message`. Provides mock `MessageListener` implementations and a helper for waiting until all queued messages are delivered.

Depends on: `oap-message-client`, `oap-message-server`

## `MessageListenerMock`

A `MessageListener` that records received messages and supports controllable failure injection.

```java
MessageListenerMock listener = new MessageListenerMock( (byte) 0x10 );

// Register with MessageHttpHandler
handler.map.put( listener.getId(), listener );

// After messages are delivered:
assertThat( listener.getMessages() ).hasSize( 1 );
assertThat( listener.getMessages().get(0).data ).isEqualTo( "hello" );
assertThat( listener.accessCount.get() ).isEqualTo( 1 );
```

### `TestMessage`

Each delivered message is stored as a `TestMessage`:

| Field | Type | Description |
|---|---|---|
| `version` | `int` | Message schema version |
| `md5` | `String` | Hex-encoded MD5 of the payload |
| `data` | `String` | Payload decoded as UTF-8 |

### Error injection

```java
// Next N calls to run() return STATUS_UNKNOWN_ERROR (client retries)
listener.throwUnknownError( 2, false );

// Next N calls throw a RuntimeException (→ STATUS_UNKNOWN_ERROR_NO_RETRY, client drops)
listener.throwUnknownError( 2, true );

// Set a fixed status for all subsequent calls
listener.setStatus( MessageProtocol.STATUS_UNKNOWN_ERROR );
listener.setStatusOk();
```

### Reset

```java
listener.reset(); // clears messages, error injection, and status
```

---

## `MessageListenerJsonMock`

Extends `MessageListenerJson<String>` — automatically JSON-deserializes the raw payload before recording it. Use when the sender uses `ContentWriter.ofJson()`.

```java
MessageListenerJsonMock listener = new MessageListenerJsonMock( (byte) 0x11 );

// After delivery of a JSON string payload:
assertThat( listener.messages.get(0).data ).isEqualTo( "\"hello\"" );
```

Supports the same `throwUnknownError(int count)` injection as `MessageListenerMock`.

---

## `MessageSenderUtils`

### `waitSendAll`

Blocks the calling thread until the sender's ready, retry, and in-progress queues are all empty, or throws if the timeout expires.

```java
sender.send( MY_TYPE, payload, ContentWriter.ofJson() );
sender.syncMemory();

MessageSenderUtils.waitSendAll( sender, 5000 /* ms timeout */, 50 /* ms poll interval */ );

// All messages have been delivered at this point
assertThat( listener.getMessages() ).hasSize( 1 );
```

Useful in integration tests where the sender runs on a background scheduler and the test needs to synchronize delivery before asserting.
