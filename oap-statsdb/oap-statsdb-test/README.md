# oap-statsdb-test

Test utilities for `oap-statsdb`. Provides an in-memory transport that wires a `StatsDBNode` directly to a `StatsDBMaster` without HTTP or message-broker overhead.

Depends on: `oap-statsdb-master`, `oap-statsdb-node`

## `StatsDBTransportMock`

Implements `StatsDBTransport`. On `sendAsync`, it immediately forwards the sync batch to the master and records it in the `syncs` list.

```java
StatsDBMaster master = new StatsDBMaster( schema, StatsDBStorage.NULL );
StatsDBTransportMock transport = new StatsDBTransportMock( master );
StatsDBNode node = new StatsDBNode( schema, transport );

node.<Counters>update( "api", "2024-06-01", v -> v.requests += 5 );
node.sync();

assertThat( master.<Counters>get( "api", "2024-06-01" ).requests ).isEqualTo( 5 );
assertThat( transport.syncs ).hasSize( 1 );
```

### Exception injection

Test failure handling by injecting a transport exception before syncing:

```java
transport.syncWithException( sync -> new RuntimeException( "network down" ) );
node.<Counters>update( "api", "2024-06-01", v -> v.requests++ );
node.sync();
assertThat( node.lastSyncSuccess ).isFalse();

transport.syncWithoutException();
node.<Counters>update( "api", "2024-06-01", v -> v.requests++ );
node.sync();
assertThat( node.lastSyncSuccess ).isTrue();
```

### Without a master

Construct without a master to record syncs without forwarding them:

```java
StatsDBTransportMock transport = new StatsDBTransportMock();
StatsDBNode node = new StatsDBNode( schema, transport );

node.<Counters>update( "api", "k2", v -> v.requests += 10 );
node.sync();

assertThat( transport.syncs ).hasSize( 1 );
assertThat( transport.syncs.get( 0 ).data ).hasSize( 1 );
```

### API summary

| Method / Field | Description |
|---|---|
| `syncs` | `ArrayList<RemoteStatsDB.Sync>` — all batches delivered so far |
| `syncWithException( Function<Sync, RuntimeException> )` | Make the next `sendAsync` call throw the given exception |
| `syncWithoutException()` | Clear the injected exception — return to normal behaviour |
| `reset()` | Clear `syncs` and remove any injected exception |
