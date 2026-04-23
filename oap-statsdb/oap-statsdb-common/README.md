# oap-statsdb-common

Core interfaces and base classes for `oap-statsdb`. See the [parent README](../README.md) for the full data model, `NodeSchema` declaration, and `StatsDB` API reference.

## Contents

| Class / Interface | Description |
|---|---|
| `Node.Value<T>` | Leaf or intermediate value — must implement `merge(T)` and `Serializable` |
| `Node.Container<T, TChild>` | Intermediate value with `aggregate(List<TChild>)` rollup |
| `NodeSchema` | Ordered list of `(levelKey, valueClass)` pairs, one per tree level |
| `StatsDB` | Abstract base implementing `update`, `get`, `children`, `select2`–`select5` |
| `IStatsDB` | Thinner abstract base used by `StatsDBNode` (no select methods) |
| `Node` | Internal tree node: holds a `Value`, child map, create-time (`ct`), modify-time (`mt`) |
| `NodeId` | Immutable ordered list of string keys identifying a node path |
| `RemoteStatsDB.Sync` | Serializable batch of `NodeIdNode` records sent from node to master |
