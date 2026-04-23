# oap-message

Reliable, durable HTTP message delivery for the OAP platform. The sender buffers messages to disk when the network is unavailable and retries until acknowledged. The server deduplicates by MD5 so retries are always safe to replay.

## Architecture

```
MessageSender (client process)
  send(type, data)
    │
    ├─ in-memory queue ──► syncMemory() ──► POST /messages ──► MessageHttpHandler
    │                                                                │
    └─ disk (on shutdown/failure)                                   ├─ MD5 dedup (MessageHashStorage)
         syncDisk() reloads on restart                              │
                                                                    └─ MessageListener.run(...)
                                                                         → short status
```

## Wire protocol

### Request (client → server)

| Field | Type | Description |
|---|---|---|
| message type | `byte` | User-defined type identifier (0–200) |
| version | `short` | Message schema version |
| client ID | `long` | Unique sender ID (per `MessageSender` instance) |
| MD5 | `byte[16]` | MD5 digest of the payload |
| reserved | `byte[8]` | Reserved, always zero |
| data size | `int` | Payload length in bytes |
| payload | `byte[N]` | Message body |

### Response (server → client)

| Field | Type | Description |
|---|---|---|
| protocol version | `byte` | Always `1` |
| client ID | `long` | Echoed from request |
| MD5 | `byte[16]` | Echoed from request |
| reserved | `byte[8]` | Reserved |
| status | `short` | See status codes below |

## Status codes

| Constant | Value | Meaning |
|---|---|---|
| `STATUS_OK` | `0` | Processed successfully |
| `STATUS_UNKNOWN_ERROR` | `1` | Processing failed — client will retry |
| `STATUS_UNKNOWN_ERROR_NO_RETRY` | `2` | Processing failed — client drops the message |
| `STATUS_UNKNOWN_MESSAGE_TYPE` | `100` | No listener registered for this type — client drops the message |
| `STATUS_ALREADY_WRITTEN` | `101` | Duplicate — server already processed this MD5; treated as success by the client |

Custom status codes (causing retry) can be registered in `META-INF/oap-messages.properties` using the `map.*` prefix — see [oap-message-server](oap-message-server/README.md).

## Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-message-client](oap-message-client/README.md) | `MessageSender` — durable send queue with disk persistence | `oap-http` |
| [oap-message-server](oap-message-server/README.md) | `MessageHttpHandler`, `MessageListener`, `MessageListenerJson` | `oap-http` |
| [oap-message-test](oap-message-test/README.md) | `MessageListenerMock`, `MessageListenerJsonMock`, `MessageSenderUtils` | `oap-message-client`, `oap-message-server` |
