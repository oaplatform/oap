# oap-mail (core)

Core email module. Provides `Message`, the `Mailman` delivery loop, a pluggable `MailQueue`, SMTP transport, and a Velocity-based `Template` engine.

## `Message`

```java
Message msg = new Message( "Subject", "Body text", List.of() );
msg.from = new MailAddress( "sender@example.com" );
msg.to.add( new MailAddress( "recipient@example.com" ) );
msg.contentType = "text/html";   // default: text/plain
```

| Field | Type | Default |
|---|---|---|
| `to` | `List<MailAddress>` | empty |
| `cc` | `List<MailAddress>` | empty |
| `bcc` | `List<MailAddress>` | empty |
| `subject` | `String` | — |
| `body` | `String` | — |
| `attachments` | `List<Attachment>` | empty |
| `from` | `MailAddress` | — |
| `contentType` | `String` | `text/plain` |
| `created` | `DateTime` | now (UTC) |

`MailAddress` accepts `"name <email>"` or plain `"email"` strings.

---

## `Mailman`

A supervised background thread that drains the queue and delivers messages via the configured `Transport`.

```java
Mailman mailman = new Mailman( transport, queue, Duration.ofMinutes( 1 ) );
mailman.send( message );   // enqueue + wake delivery loop
```

| Parameter | Default | Description |
|---|---|---|
| `transport` | (required) | `Transport` implementation to use |
| `queue` | (required) | `MailQueue` to drain |
| `retryPeriod` | `1m` | How often to retry failed messages |

Kernel service name: `oap-mail-mailman` (supervise=true, runs on its own thread).

---

## `MailQueue`

Wraps a `MailQueuePersistence` and manages the delivery lifecycle. Messages that fail delivery and remain in the queue longer than `brokenMessageTTL` are automatically dropped.

| Parameter | Default | Description |
|---|---|---|
| `mailQueuePersistence` | (required) | Storage backend |
| `brokenMessageTTL` | `2w` | Max age of a broken message before discard |

Kernel service name: `oap-mail-queue`.

### Persistence backends

#### `MailQueuePersistenceFile` (default)

Persists the queue to `<location>/mail.gz` (gzip-compressed JSON). Survives restarts. Writes on every dequeue.

```hocon
services.oap-mail.mail-queue-persistence-file.parameters.location = /var/spool/myapp/mail
```

#### `MailQueuePersistenceMemory`

In-memory queue — no durability. Disabled by default in the module descriptor. Enable explicitly when persistence is not needed:

```hocon
services.oap-mail.mail-queue-persistence-memory.enabled = true
services.oap-mail.mail-queue-persistence.default = <modules.oap-mail.mail-queue-persistence-memory>
```

---

## `Transport`

Single-method interface. Implement it to add custom delivery backends.

```java
@FunctionalInterface
public interface Transport {
    void send( Message message );
}
```

Abstract Kernel service: `oap-mail-transport`. Wire a concrete implementation in `application.conf`:

```hocon
services.oap-mail.oap-mail-transport.default = <modules.oap-mail.oap-mail-transport-smtp>
```

### `SmtpTransport`

JavaMail SMTP transport. Kernel service: `oap-mail-transport-smtp`.

| Parameter | Description |
|---|---|
| `host` | SMTP server hostname |
| `port` | SMTP server port |
| `tls` | Enable STARTTLS |
| `tlsVersion` | TLS protocol version (e.g. `TLSv1.2`) |
| `authenticator` | `PasswordAuthenticator` service reference |

Supports plain text and HTML multipart messages. Attachments can reference:
- Local file paths
- Classpath resources (`classpath:...`)
- HTTP URLs

HTML messages support CID-referenced inline images via `Attachment` with a `contentId`.

---

## `Template`

Velocity-based template engine that renders to a `Message`. Two file formats:

| Format | Extension | Parser |
|---|---|---|
| Text | `.mail` | `TextMessageParser` |
| XML | `.xmail` | `XmlMessageParser` |

### Text format (`.mail`)

```
--subject--
Order Confirmation #${order.id}
--body--
Dear ${customer.name},

Your order has been placed.
--attachment--
Optional inline text attachment
```

Sections are delimited by `--subject--`, `--body--`, and `--attachment--` markers (multiple `--attachment--` sections allowed).

### Loading a template

```java
// From classpath (looks for MyTemplate.mail or MyTemplate.xmail next to the calling class)
Template t = Template.of( "MyTemplate" ).orElseThrow();

// From filesystem
Template t = Template.of( Path.of( "/etc/myapp/welcome.mail" ) ).orElseThrow();
```

### Rendering

```java
t.bind( "order", order );
t.bind( "customer", customer );
Message msg = t.buildMessage();

msg.to.add( new MailAddress( customer.email ) );
mailman.send( msg );
```

Call `t.clear()` to reset bindings between uses.
