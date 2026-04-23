# oap-mail

Email delivery for the OAP platform. Provides a persistent delivery queue, Velocity-based message templates, and swappable transports (SMTP, SendGrid).

## Architecture

```
Template → Message → Mailman → MailQueue → Transport
                                    ↕
                            MailQueuePersistence
                         (file / memory / MongoDB)
```

`Mailman` runs as a supervised background thread that drains `MailQueue`. Failed messages are retried on a configurable schedule; messages that remain broken past `brokenMessageTTL` are dropped.

## Sub-modules

| Module | Description |
|---|---|
| [oap-mail](oap-mail/README.md) | Core: `Message`, `Mailman`, `MailQueue`, `SmtpTransport`, `Template` |
| [oap-mail-sendgrid](oap-mail-sendgrid/README.md) | SendGrid REST API transport |
| [oap-mail-mongo](oap-mail-mongo/README.md) | MongoDB-backed queue persistence |
| [oap-mail-test](oap-mail-test/README.md) | `TransportMock`, `MessageAssertion`, `MessagesAssertion`, `MailBox` |
