OAP-messages Binary protocol encoding
===============================

This document describes the wire encoding for OAP-messages.

### UML diagram

![UML Diagram](oap-messages.png)

### Binary encoding
```
+--------+--------+--------+--------+---------+--------+----------------------+
|  mtype |  mver  |clientid|   md5  | reserve |  size  |     bytes            |
+--------+--------+--------+--------+---------+--------+----------------------+
```

Where:
* `mtype` is a single-byte message type. Set by the protocol client.
* `mver` is a 16-bit number that indicates the version of the data sent by the client.
* `clientid` is a 64-bit number that is a unique customer identifier.
* `md5` is a 16-byte md5
* `reserve` is a 8-byte reserve
* `size` is the size of the byte array, encoded as an int32, positive values only
* `bytes` are the bytes of the byte array.
