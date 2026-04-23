# oap-statsdb

Distributed, in-memory statistics database for the OAP platform. Data is organized as a typed key hierarchy — each level of the tree holds a `Node.Value` that knows how to merge itself with another value of the same type. Parent nodes optionally aggregate over their children after each update.

## Architecture

```
StatsDBNode (process A)          StatsDBNode (process B)
  update("k1","k2", v -> v.n++)    update("k1","k3", v -> v.n++)
  sync() ─────────────────────┐    sync() ────────────────────┐
                               ▼                              ▼
                        StatsDBMaster (in-memory tree)
                          k1 → MockChild (aggregate)
                            k2 → MockValue
                            k3 → MockValue
                          ▼ (periodically)
                        StatsDBStorage (MongoDB / NULL)
```

In a single-process deployment, use `StatsDBMaster` directly without a `StatsDBNode`.

## Sub-modules

| Module | Description | Depends on |
|---|---|---|
| [oap-statsdb-common](oap-statsdb-common/README.md) | Core: `Node.Value`, `Node.Container`, `NodeSchema`, `StatsDB` API | — |
| [oap-statsdb-master](oap-statsdb-master/README.md) | `StatsDBMaster`, `StatsDBStorage`, MongoDB persistence, message listener | `oap-statsdb-common` |
| [oap-statsdb-node](oap-statsdb-node/README.md) | `StatsDBNode`, `StatsDBTransport`, message-based sync transport | `oap-statsdb-common` |
| [oap-statsdb-test](oap-statsdb-test/README.md) | `StatsDBTransportMock` for integration tests | `oap-statsdb-master`, `oap-statsdb-node` |

---

## Data model

### `Node.Value<T>`

The value stored at each tree node. Must implement `merge(T other)` — called when a sync from a remote node arrives — and `Serializable`.

```java
public class Counters implements Node.Value<Counters> {
    public long requests;
    public long errors;

    @Override
    public Counters merge( Counters other ) {
        requests += other.requests;
        errors   += other.errors;
        return this;
    }
}
```

### `Node.Container<T, TChild>`

A value at an intermediate tree level that rolls up metrics from its children. `aggregate(List<TChild>)` is called automatically after every update on any descendant.

```java
public class RollupCounters implements Node.Container<RollupCounters, Counters> {
    public long totalRequests;

    @Override
    public RollupCounters merge( RollupCounters other ) {
        // merge is additive — called when syncing from remote nodes
        return this;
    }

    @Override
    public RollupCounters aggregate( List<Counters> children ) {
        totalRequests = children.stream().mapToLong( c -> c.requests ).sum();
        return this;
    }
}
```

Mark computed fields `@JsonIgnore` if they should not be persisted (they are re-derived from children on load).

### `NodeSchema`

Declares the `Node.Value` class at each key level, ordered from root to leaf.

```java
NodeSchema schema = new NodeSchema(
    nc( "endpoint",  RollupCounters.class ),  // level 0 — root
    nc( "date",      Counters.class )          // level 1 — leaf
);
```

`nc(String key, Class<T>)` is a static factory on `NodeSchema`.

Register value classes in `oap-module.oap` so the JSON binder can deserialize them:

```hocon
configurations = [
  {
    loader = oap.json.TypeIdFactory
    config {
      counters         = com.example.Counters
      rollup-counters  = com.example.RollupCounters
    }
  }
]
```

---

## `StatsDB` API

All update and query methods are available on both `StatsDBMaster` and `StatsDBNode`.

### Writing

```java
// 1-key update (leaf at level 0)
db.<Counters>update( "endpoint-a", v -> v.requests++ );

// 2-key update (leaf at level 1)
db.<Counters>update( "endpoint-a", "2024-06-01", v -> {
    v.requests++;
    v.errors++;
} );

// Up to 5 keys supported
db.<Counters>update( k1, k2, k3, k4, k5, v -> v.requests++ );
```

### Reading

```java
// Get value at a path (returns null if not present)
Counters c = db.get( "endpoint-a", "2024-06-01" );

// Get all child values under a prefix
Stream<Counters> daily = db.children( "endpoint-a" );
```

### Typed select streams

Use `select2()` … `select5()` to stream over the full tree with typed key-value tuples:

```java
// 2-level tree: (id1, v1) → (id2, v2)
db.<RollupCounters, Counters>select2().forEach( row -> {
    System.out.println( row.id1 + " " + row.id2 + " requests=" + row.v2.requests );
} );

// 3-level tree
db.<T1, T2, T3>select3().forEach( row -> { … } );
// also select4(), select5()
```

| Method | Fields |
|---|---|
| `select2()` | `id1, v1, id2, v2` |
| `select3()` | `id1, v1, id2, v2, id3, v3` |
| `select4()` | `id1, v1, id2, v2, id3, v3, id4, v4` |
| `select5()` | `id1, v1, id2, v2, id3, v3, id4, v4, id5, v5` |

### Clearing

```java
db.removeAll();  // clears in-memory state only
```

---

## Quick start — single process

```java
NodeSchema schema = new NodeSchema(
    nc( "endpoint", RollupCounters.class ),
    nc( "date",     Counters.class )
);

try( StatsDBMaster master = new StatsDBMaster( schema, StatsDBStorage.NULL ) ) {
    master.<Counters>update( "search", "2024-06-01", v -> v.requests += 5 );
    master.<Counters>update( "search", "2024-06-02", v -> v.requests += 3 );

    // Roll-up is automatic
    assertThat( master.<RollupCounters>get( "search" ).totalRequests ).isEqualTo( 8 );
}
```
