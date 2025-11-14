# OAP StatsDB

High-performance in-memory statistics database with distributed master-node architecture for real-time metrics aggregation and storage.

## Overview

OAP StatsDB is a specialized database designed for collecting, aggregating, and querying statistics and metrics. It features:
- In-memory storage for sub-microsecond access latency
- Multi-level aggregation capabilities
- Distributed master-node architecture
- Configurable data types and aggregation functions
- Remote statistics query support
- Persistent storage options
- Time-series metrics support

The module consists of:
- **oap-statsdb-common** - Core data structures and interfaces
- **oap-statsdb-node** - Node implementation for data collection
- **oap-statsdb-master** - Master node for aggregation and coordination
- **oap-statsdb-test** - Testing utilities

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-statsdb-parent</artifactId>
    <version>${oap.version}</version>
    <type>pom</type>
</dependency>

<!-- Core StatsDB -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-statsdb-common</artifactId>
    <version>${oap.version}</version>
</dependency>

<!-- Node Implementation -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-statsdb-node</artifactId>
    <version>${oap.version}</version>
</dependency>

<!-- Master Aggregation -->
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-statsdb-master</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **In-Memory Storage** - Sub-microsecond access latency
- **Schema Definition** - Flexible field definitions with types
- **Aggregation Functions** - SUM, AVG, MIN, MAX, COUNT support
- **Hierarchical Data** - Support for nested data structures
- **Remote Queries** - Query distributed nodes from master
- **Node Architecture** - Each node maintains local statistics
- **Master Coordination** - Master aggregates data from nodes
- **Time Series** - Natural support for time-series metrics
- **Concurrent Access** - Thread-safe operations with minimal locking

## Key Classes

- `StatsDB` - Abstract base class for statistics database implementations
- `Node` - Represents a data node with fields and aggregates
- `NodeSchema` - Defines the structure of statistics data
- `IStatsDB` - Interface for StatsDB implementations
- `RemoteStatsDB` - Client for querying remote statistics
- `MessageType` - Message type definitions for protocol

## Data Model

StatsDB uses a hierarchical node-based model:
```
StatsDB (root)
  ├── Node (named data point)
  │   ├── Field (metric value)
  │   ├── Aggregate (computed value)
  │   └── Sub-nodes (child data)
  └── NodeSchema (structure definition)
```

## Quick Example

```java
import oap.statsdb.StatsDB;
import oap.statsdb.Node;
import oap.statsdb.NodeSchema;

// Define schema
NodeSchema schema = new NodeSchema()
    .addField("requests", Long.class)
    .addField("errors", Long.class)
    .addField("latency", Long.class)
    .addAggregate("errorRate", Double.class);

// Create StatsDB instance
StatsDB db = new InMemoryStatsDB(schema);

// Record statistics
Node node = db.getOrCreate("api.endpoint");
node.increment("requests", 1);
node.addValue("latency", 45);

// Retrieve statistics
Long requests = node.get("requests");
Double errorRate = node.get("errorRate");

// Query from master (if distributed)
RemoteStatsDB remote = new RemoteStatsDB("localhost", 9999);
Node remoteStats = remote.getStats("api.endpoint");
System.out.println("Total requests: " + remoteStats.get("requests"));
```

## Configuration

Enable OAP StatsDB in `oap-module.yaml`:

```yaml
dependsOn:
  - oap-statsdb
```

Configure in `application.conf`:

```hocon
oap-statsdb-node {
    schema {
        fields = [
            { name = "requests", type = "long" },
            { name = "errors", type = "long" },
            { name = "latency", type = "long" }
        ]
        aggregates = [
            { name = "errorRate", type = "double", function = "errors / requests" }
        ]
    }
}

oap-statsdb-master {
    port = 9999
    aggregationInterval = 60000
    retentionTime = 3600000
}
```

## Sub-Module Details

### oap-statsdb-common
Core interfaces and data structures:
- `StatsDB` - Abstract statistics database
- `Node` - Data node representation
- `NodeSchema` - Schema definitions
- `IStatsDB` - Interface contract

### oap-statsdb-node
Local node implementation for collecting statistics:
- In-memory storage with ConcurrentHashMap
- Automatic aggregation calculation
- Sub-node hierarchy support
- Configurable data types

### oap-statsdb-master
Master node coordination and aggregation:
- Central point for query aggregation
- Coordinates data from multiple nodes
- Time-based aggregation
- Remote query interface

### oap-statsdb-test
Testing utilities for StatsDB development.

## Node Schema

Define data structure for statistics:

```java
NodeSchema schema = new NodeSchema();
schema.addField("name", fieldType);
schema.addAggregate("name", resultType);
```

### Supported Field Types
- `Long` / `long` - 64-bit integer
- `Double` / `double` - Double precision float
- `Integer` / `int` - 32-bit integer
- `String` - Text data

### Aggregation Functions
- `SUM` - Sum of values
- `AVG` - Average of values
- `MIN` - Minimum value
- `MAX` - Maximum value
- `COUNT` - Count of entries

## Distributed Architecture

### Single Node Mode
```
Application
    ↓
  StatsDB (local)
    ↓
  [Data]
```

### Master-Node Mode
```
Application A        Application B        Application C
    ↓                    ↓                    ↓
Node StatsDB      Node StatsDB         Node StatsDB
    ↓                    ↓                    ↓
[Data]                [Data]               [Data]
    ↑                    ↑                    ↑
    └────────────────────┴────────────────────┘
           Master StatsDB
               ↓
          [Aggregated Data]
```

## Query Patterns

```java
// Direct node query
Node node = db.getNode("api.endpoint");
Long value = node.get("requests");

// Hierarchical query
Node parent = db.getNode("api");
Node[] children = parent.children();

// Remote query (from master)
RemoteStatsDB master = new RemoteStatsDB(host, port);
Collection<Node> stats = master.query("api.*");

// Time-series query
List<Node> timeSeries = db.getTimeSeriesData(start, end);
```

## Performance Considerations

- **Memory Usage** - Each node stores all field values in memory
- **Lock Contention** - Uses ConcurrentHashMap for minimal locking
- **GC Impact** - Design for low allocation rate
- **Query Cost** - Fast O(1) or O(n) node lookups
- **Network** - Remote queries add network latency

## Best Practices

1. **Schema Design** - Keep schema simple and focused
2. **Naming** - Use hierarchical names: `service.method.metric`
3. **Retention** - Configure appropriate cleanup policies
4. **Aggregation** - Use master node for cross-instance metrics
5. **Monitoring** - Monitor memory usage and query response times

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-message` - Protocol for node-master communication
- `oap-application` - Application framework integration
- `oap-storage` - Optional persistent storage

## Testing

See `oap-statsdb/oap-statsdb-test/` for test fixtures and benchmarks.
