# oap-mail-test

Test utilities for oap-mail. Provides an in-memory transport mock and AssertJ-style assertions for verifying sent messages.

Depends on: `oap-mail`

## `TransportMock`

In-memory `Transport` implementation — captures sent messages instead of delivering them. Use it in unit tests to verify that the right messages were sent.

```java
TransportMock transport = new TransportMock();
Mailman mailman = new Mailman( transport, queue, Duration.ofMinutes( 1 ) );

mailman.send( message );

assertThat( transport.getSent() ).hasSize( 1 );
```

---

## `MessageAssertion`

AssertJ-style fluent assertions for a single `Message`.

```java
import static oap.mail.test.MessageAssertion.assertMessage;

assertMessage( message )
    .isFrom( "sender@example.com" )
    .isSentTo( "alice@example.com", "bob@example.com" )
    .isCopiedTo( "cc@example.com" )
    .hasSubject( "Order Confirmation" )
    .hasContentType( "text/html" )
    .hasBody( "Expected body text" );

// Compare body against a classpath test resource (with optional substitutions)
assertMessage( message )
    .hasBody( MyTest.class, "expected-body.txt" )
    .hasBody( MyTest.class, "expected-body.txt", Map.of( "id", "o-1" ) );
```

### Factory methods

| Method | Description |
|---|---|
| `assertMessage(message)` | Assert on an existing `Message` object |
| `assertInboxMostRecentMessage(mail, password)` | Fetch the most recent message from a Gmail inbox and assert on it |

### Assertion methods

| Method | Description |
|---|---|
| `isFrom(email)` | Sender email matches |
| `isSentTo(emails...)` | `to` list contains all given emails |
| `isCopiedTo(emails...)` | `cc` list contains all given emails |
| `isBlindlyCopiedTo(emails...)` | `bcc` list contains all given emails |
| `hasSubject(subject)` | Subject matches exactly |
| `hasContentType(contentType)` | Content type matches |
| `hasBody(body)` | Body matches exactly |
| `hasBody(contextClass, resource)` | Body matches classpath test resource |
| `hasBody(contextClass, resource, substitutions)` | Body matches resource after variable substitution |

---

## `MessagesAssertion`

AssertJ-style fluent assertions for a collection of messages.

```java
import static oap.mail.test.MessagesAssertion.assertMessages;

assertMessages( transport.getSent() )
    .sentTo( "alice@example.com", msg ->
        assertMessage( msg ).hasSubject( "Welcome" ) )
    .bySubject( "Reset Password", msg ->
        assertMessage( msg ).isSentTo( "bob@example.com" ) );
```

### Factory methods

| Method | Description |
|---|---|
| `assertMessages(messages)` | Assert on an in-memory collection |
| `assertInbox(user, password)` | Fetch all unread messages from a Gmail inbox |

### Finder methods

Each finder locates the first matching message and passes it to the `Consumer<Message>` assertion; fails if no match is found.

| Method | Matches |
|---|---|
| `sentTo(email, assertion)` | First message whose `to` list contains `email` |
| `bySubject(subject, assertion)` | First message with the given subject |
| `by(predicate, assertion)` | First message matching the custom predicate |

---

## `MailBox`

Low-level IMAP reader for real-inbox integration tests (connects to Gmail over TLS).

```java
Folder inbox = MailBox.connectToInbox( "test@gmail.com", "app-password" );

// All unread messages, sorted by sent date
List<Message> messages = MailBox.getMessagesFromBox( inbox );

// The most recently delivered message
Message last = MailBox.getLastSentMessageFromTheBox( inbox );
```

Use `MessageAssertion.assertInboxMostRecentMessage` or `MessagesAssertion.assertInbox` for a one-liner that opens and closes the inbox automatically.
