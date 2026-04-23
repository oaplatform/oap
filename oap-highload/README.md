# oap-highload

CPU affinity utility for the OAP platform. Pins the calling thread to a specific CPU core via `net.openhft.affinity`, reducing cross-core cache misses in high-throughput loops (network I/O, encoding, scheduling).

## `Affinity`

A plain utility class — not a managed OAP service. Instantiate it directly wherever you need to control thread placement.

### CPU set syntax

The constructor accepts a string that describes which CPU cores to use:

| Expression | Meaning | Example → CPUs |
|---|---|---|
| `*` | No affinity (disabled) | `*` → `[]` |
| `n` | Single core | `3` → `[3]` |
| `n-m` | Inclusive range | `1-3` → `[1, 2, 3]` |
| `n+` | Core `n` through the last available | `4+` on 8-core → `[4, 5, 6, 7]` |
| Comma-separated | Combine any of the above | `1-3, 8` → `[1, 2, 3, 8]` |

```java
Affinity affinity = new Affinity( "2-5" );   // cores 2, 3, 4, 5
Affinity affinity = new Affinity( "0+" );    // all cores from 0 upward
Affinity affinity = new Affinity( "*" );     // disabled — no pinning
Affinity affinity = Affinity.any();          // same as "*"
```

### API

| Method | Description |
|---|---|
| `set()` | Pin the calling thread to the next core in the set (round-robin); no-op when disabled |
| `isEnabled()` | `false` when constructed with `*`; `true` otherwise |
| `size()` | Number of CPU cores in the configured set |
| `getCpus()` | Raw `int[]` of configured core indices |

### Usage

Call `set()` once per thread at startup, or at the top of a processing loop when you want round-robin distribution across the configured cores:

```java
Affinity affinity = new Affinity( "4+" );   // dedicate upper cores to this pool

ExecutorService pool = Executors.newFixedThreadPool( affinity.size(), r -> {
    Thread t = new Thread( () -> {
        affinity.set();   // pin this thread before doing any work
        r.run();
    } );
    return t;
} );
```

When `isEnabled()` is `false` (e.g. `*` in config), `set()` is a no-op and the JVM scheduler assigns cores freely — no code path changes needed.
