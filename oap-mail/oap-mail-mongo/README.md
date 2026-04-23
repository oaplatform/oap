# oap-mail-mongo

MongoDB-backed queue persistence for oap-mail. Stores queued messages in a MongoDB collection so the queue survives restarts without a local filesystem.

Depends on: `oap-mail`, `oap-storage-mongo`

## `MailQueuePersistenceMongo`

```java
MailQueuePersistenceMongo persistence = new MailQueuePersistenceMongo( mongoClient, "mails" );
```

Each queued message is stored as a document with a Cuid `_id`. On dequeue (successful delivery or TTL expiry) the document is deleted.

| Parameter | Default | Description |
|---|---|---|
| `mongoClient` | (required) | `MongoClient` from `oap-storage-mongo` |
| `collectionName` | `mails` | MongoDB collection name |

Kernel service name: `mail-queue-persistence-mongo`.

## Wiring

Enable MongoDB persistence in `application.conf`:

```hocon
boot.main = [oap-mail, oap-mail-mongo]

services {
  oap-mail.mail-queue-persistence.default = <modules.oap-mail-mongo.mail-queue-persistence-mongo>
}
```
