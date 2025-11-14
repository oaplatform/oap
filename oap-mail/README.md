# OAP Mail

Email functionality module providing email sending, queuing, and transport management with support for multiple email providers.

## Overview

OAP Mail is a comprehensive email service for the Open Application Platform. It provides:
- Email message creation and queuing
- Pluggable transport backends (SMTP, SendGrid, etc.)
- Message persistence with failover support
- Template-based email generation with Velocity
- Retry logic and error handling
- In-memory and file-based persistence

The module consists of several sub-modules:
- **oap-mail** - Core email functionality
- **oap-mail-sendgrid** - SendGrid email transport
- **oap-mail-mongo** - MongoDB-based message persistence
- **oap-mail-test** - Testing utilities

## Maven Coordinates

### Core Module
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-mail-parent</artifactId>
    <version>${oap.version}</version>
    <type>pom</type>
</dependency>

<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-mail</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### Optional Email Transport
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-mail-sendgrid</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### Optional Persistence
```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-mail-mongo</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **Message Model** - Structured email message with recipients, subject, body, attachments
- **Template Support** - Velocity template engine for dynamic email content
- **Message Queuing** - Persistent queue with automatic retry
- **Multiple Transports** - SMTP, SendGrid, and mock transports
- **Persistence Options** - File-based, in-memory, or MongoDB-backed
- **Attachments** - Support for file and content-based attachments
- **Error Handling** - Graceful failure handling with logging

## Key Classes

- `Mailman` - Main email sender service that processes the queue
- `Message` - Email message model with recipients, subject, body
- `MailQueue` - FIFO queue for pending emails
- `Transport` - Interface for email transport backends
- `SmtpTransport` - SMTP transport implementation
- `Template` - Template-based email content
- `MailQueuePersistence` - Interface for persistence backends

## Quick Example

```java
// Create a message
Message message = new Message()
    .setFrom("sender@example.com")
    .setTo("recipient@example.com")
    .setSubject("Hello")
    .setBody("Welcome to OAP Mail!");

// Send through mailman
mailman.send(message);

// Or use queue for async processing
mailQueue.put(message);
```

## Configuration

Enable OAP Mail in `oap-module.yaml`:

```yaml
dependsOn:
  - oap-mail
```

Configure in `application.conf`:

```hocon
oap-mail-smtp-transport.parameters {
    host = "smtp.example.com"
    port = 25
    user = "username"
    password = "password"
}

oap-mail-queue.parameters {
    transport = "@service:oap-mail-smtp-transport"
    persistenceType = "file"
}
```

### SendGrid Configuration

```hocon
oap-mail-sendgrid-transport.parameters {
    sendGridKey = "SG.xxxxx"
}

oap-mail-queue.parameters {
    transport = "@service:oap-mail-sendgrid-transport"
}
```

## Sub-Module Details

### oap-mail-sendgrid
Integrates SendGrid REST API for email delivery. Requires SendGrid API key.
See `oap-mail-sendgrid/README.md` for setup details.

### oap-mail-mongo
Uses MongoDB for persistent message storage. Useful for distributed systems
where messages need to survive restarts.

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-template` - Template processing
