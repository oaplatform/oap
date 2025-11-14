# OAP Message

Lightweight, efficient messaging protocol and framework for inter-process communication with pluggable transports.

## Overview

OAP Message provides a binary messaging protocol with support for client-server communication patterns. It features:
- Efficient binary protocol with MD5 integrity checking
- Pluggable transport implementations
- Protocol versioning and status codes
- Message type routing
- Connection-based message handling
- Async and batched operation support
- Comprehensive error handling

The module consists of:
- **oap-message-common** - Protocol definition and shared interfaces
- **oap-message-server** - Server-side message handling
- **oap-message-client** - Client-side messaging
- **oap-message-test** - Testing utilities

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-message</artifactId>
    <version>${oap.version}</version>
    <type>pom</type>
</dependency>

<!-- Core Message Framework -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-message-common</artifactId>
    <version>${oap.version}</version>
</dependency>

<!-- Server-side Messaging -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-message-server</artifactId>
    <version>${oap.version}</version>
</dependency>

<!-- Client-side Messaging -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-message-client</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **Binary Protocol** - Efficient, compact message format
- **Integrity Checking** - MD5 checksums for message validation
- **Message Types** - Support for multiple message types with routing
- **Status Codes** - Comprehensive protocol status codes (OK, errors, etc.)
- **Version Support** - Protocol versioning for compatibility
- **Connection Management** - Per-connection message handling
- **Async Processing** - Non-blocking message handling capabilities
- **Batching** - Support for batched message processing
- **Error Recovery** - Graceful handling of transport errors

## Key Classes

- `MessageProtocol` - Protocol constants and definitions
- `MessageStatus` - Message status codes and enums
- `Connection` - Represents a client connection
- `MessageHandler` - Interface for handling received messages
- `Transport` - Base interface for message transports

## Protocol Details

### Message Format
- Protocol Version: 1
- Reserved: 8 bytes
- Message Type: 1 byte
- Message Content: Variable length
- MD5 Checksum: 16 bytes (optional)

### Status Codes
- `STATUS_OK (0)` - Successful operation
- `STATUS_UNKNOWN_ERROR (1)` - Unknown error, may retry
- `STATUS_UNKNOWN_ERROR_NO_RETRY (2)` - Fatal error, no retry
- `STATUS_UNKNOWN_MESSAGE_TYPE (100)` - Message type not supported
- `STATUS_ALREADY_WRITTEN (101)` - Message already processed

### Special Messages
- `EOF_MESSAGE_TYPE (0xFF)` - End of stream marker

## Quick Example

### Server Usage
```java
import oap.message.server.MessageServer;
import oap.message.server.MessageHandler;

// Implement message handler
class MyMessageHandler implements MessageHandler {
    @Override
    public void handle(Connection connection, byte messageType, byte[] data) {
        // Process message
        System.out.println("Received: " + new String(data));
        // Send response
        connection.send(responseType, responseData);
    }
}

// Create and start server
MessageServer server = new MessageServer(port, new MyMessageHandler());
server.start();
```

### Client Usage
```java
import oap.message.client.MessageClient;

// Create client
MessageClient client = new MessageClient("localhost", port);
client.connect();

// Send message
byte[] data = "Hello Server".getBytes();
client.send((byte)1, data);

// Receive response
byte[] response = client.receive();

client.disconnect();
```

## Configuration

Enable OAP Message in `oap-module.yaml`:

```yaml
dependsOn:
  - oap-message
```

Configure in `application.conf`:

```hocon
oap-message-server {
    port = 9999
    backlog = 100
    readTimeout = 60000
    writeTimeout = 60000
}

oap-message-client {
    connectTimeout = 10000
    readTimeout = 30000
}
```

## Sub-Module Details

### oap-message-common
Core protocol definitions and shared interfaces used by both client and server.
Contains protocol constants, status codes, and base interfaces.

### oap-message-server
Server implementation for handling incoming connections and messages.
Manages connection lifecycle and message dispatching.

### oap-message-client
Client implementation for connecting to servers and sending messages.
Handles connection pooling and async message handling.

### oap-message-test
Testing utilities including mock transports and test fixtures for messaging.

## Message Type System

Message types are user-defined. The framework provides:
- Type routing to appropriate handlers
- Type-based error handling
- Type versioning support

Example message types:
```java
public interface MessageTypes {
    byte HEARTBEAT = 1;
    byte DATA = 2;
    byte CONFIG = 3;
    byte CONTROL = 4;
}
```

## Error Handling

The protocol includes comprehensive error codes for:
- Unknown message types
- Processing failures
- Connection issues
- Timeout conditions
- Data integrity errors

## Performance Characteristics

- Minimal overhead binary protocol
- Efficient MD5 checksum validation
- Connection pooling support
- Async processing capable
- Suitable for high-throughput messaging

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-io` - I/O utilities for transport
- `oap-statsdb` - Often used for metrics transmission

## Testing

See `oap-message/oap-message-test/` for test fixtures and examples.
