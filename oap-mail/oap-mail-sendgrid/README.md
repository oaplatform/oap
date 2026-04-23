# oap-mail-sendgrid

SendGrid transport for oap-mail. Delivers messages via the SendGrid REST API (`mail/send`).

Depends on: `oap-mail`

## `SendGridTransport`

```java
SendGridTransport transport = new SendGridTransport( "SG.your-api-key" );
```

One API call is made per recipient in `message.to`. `message.body` is sent as `text/html` regardless of `message.contentType`. Attachments are forwarded as-is.

Kernel service name: `oap-mail-sendgrid-transport`.

## Wiring

Wire `SendGridTransport` as the active `oap-mail-transport` in `application.conf`:

```hocon
boot.main = [oap-mail, oap-mail-sendgrid]

services {
  oap-mail.oap-mail-transport.default = <modules.oap-mail-sendgrid.oap-mail-sendgrid-transport>
  oap-mail-sendgrid.oap-mail-sendgrid-transport.parameters.sendGridKey = ${?SENDGRID_API_KEY}
}
```
