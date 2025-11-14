# OAP Storage Module

A comprehensive data storage abstraction framework for the Open Application Platform (OAP), providing unified CRUD operations, metadata tracking, persistence, replication, and cloud storage support.

## Table of Contents

- [Overview](#overview)
- [Maven Coordinates](#maven-coordinates)
- [Sub-modules](#sub-modules)
- [Core Concepts](#core-concepts)
- [Getting Started](#getting-started)
  - [In-Memory Storage](#in-memory-storage)
  - [MongoDB Persistence](#mongodb-persistence)
  - [Cloud Storage](#cloud-storage)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
  - [Storage Interface](#storage-interface)
  - [CRUD Operations](#crud-operations)
  - [Metadata Management](#metadata-management)
  - [Replication](#replication)
  - [Data Listeners](#data-listeners)

## Overview

The OAP Storage module provides a flexible, extensible data storage abstraction layer that supports:

- **Multiple Storage Backends**: In-memory, MongoDB, AWS S3, and other cloud providers
- **CRUD Operations**: Unified Create, Read, Update, Delete interface
- **Metadata Tracking**: Automatic tracking of creation/modification timestamps and authors
- **Persistence**: Async synchronization to MongoDB with crash dump support
- **Replication**: Master-slave replication with change propagation
- **Cloud Storage**: Abstract file system interface for cloud blob storage (S3, etc.)
- **Type Safety**: Generic type support for any serializable data objects
- **ID Management**: Pluggable identifier strategies with auto-generation

## Maven Coordinates

### Core Module

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-storage</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### MongoDB Support

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-storage-mongo</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### Cloud Storage (Base)

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-storage-cloud</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### Cloud Storage - AWS S3

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-storage-cloud-aws-s3</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Sub-modules

### 1. **oap-storage** - Core Module
The foundational storage abstraction providing:
- `Storage<Id, Data>` - Main interface for all storage operations
- `MemoryStorage<Id, Data>` - In-memory implementation
- `Metadata<T>` - Object metadata with timestamps and authors
- `Identifier<Id, Data>` - Pluggable ID management
- `RemoteStorage<Id, Data>` - Interface for remote storage with hash-based consistency
- `Replicator<I, T>` - Master-slave replication support

**Key Classes**:
- `Storage.java` - Core interface
- `MemoryStorage.java` - In-memory implementation with ConcurrentHashMap
- `Metadata.java` - Metadata wrapper with creation/modification tracking
- `AbstractPersistance.java` - Base class for persistence layers

### 2. **oap-storage-mongo** - MongoDB Persistence
Provides MongoDB-backed storage with automatic synchronization:
- `MongoPersistence<I, T>` - MongoDB persistence layer
- `MongoClient` - Connection management with migration support (Mongock)
- `MongoIndex` - Index management for MongoDB collections
- Custom codecs for BSON serialization

**Key Features**:
- Async write batching with configurable delay
- Automatic crash dump on persistence errors
- Change stream support for watch operations
- MongoDB migration framework integration
- JodaTime codec support

### 3. **oap-storage-cloud** - Cloud Storage Abstraction
Abstract file system interface for cloud blob storage:
- `FileSystem` - Main interface for cloud operations
- `FileSystemCloudApi` - SPI for cloud providers
- `CloudURI` - URI parsing for cloud resources
- `BlobData` - Blob metadata and operations

**Supported Operations**:
- Async input/output streams
- List operations with pagination
- Blob upload/download
- Metadata querying

### 4. **oap-storage-cloud-aws-s3** - AWS S3 Implementation
AWS S3-specific cloud storage implementation:
- `FileSystemCloudApiS3` - S3 cloud API implementation
- AWS SDK integration with S3 Transfer Manager
- Virtual hosting and endpoint configuration
- IAM credential support

### 5. **oap-storage-mongo-test** - MongoDB Test Support
Testing utilities and fixtures for MongoDB-based tests.

### 6. **oap-storage-cloud-test** - Cloud Storage Test Support
Testing utilities and fixtures for cloud storage tests.

## Core Concepts

### 1. Storage Interface

The `Storage<Id, Data>` interface provides a generic, type-safe API for data operations:

```java
public interface Storage<Id, Data> extends Iterable<Data> {
    // Query operations
    Stream<Data> select();
    Stream<Metadata<Data>> selectMetadata();
    List<Data> list();
    List<Metadata<Data>> listMetadata();
    Optional<Data> get(Id id);
    long size();
    
    // CRUD operations
    Data store(Data object, String modifiedBy);
    void store(Collection<Data> objects, String modifiedBy);
    Optional<Data> update(Id id, Function<Data, Data> update, String modifiedBy);
    Data update(Id id, Function<Data, Data> update, Supplier<Data> init, String modifiedBy);
    boolean tryUpdate(Id id, Function<Data, Data> tryUpdate, String modifiedBy);
    Optional<Data> delete(Id id, String modifiedBy);
    Optional<Data> permanentlyDelete(Id id);
    
    // Deletion
    void deleteAll();
    void permanentlyDelete();
    
    // Listeners & management
    void addDataListener(DataListener<Id, Data> listener);
    void removeDataListener(DataListener<Id, Data> listener);
    Identifier<Id, Data> identifier();
}
```

### 2. Metadata Tracking

Every stored object is wrapped with `Metadata<T>` that tracks:

```java
public class Metadata<T> {
    public long modified;        // Last modification timestamp (ms)
    public long created;         // Creation timestamp (ms)
    public String createdBy;     // User/system who created it
    public String modifiedBy;    // User/system who last modified it
    public long hash;            // Object hash for consistency checks
    public T object;             // The actual data object
    private boolean deleted;     // Soft delete flag
}
```

### 3. CRUD Operations

#### Create/Store
```java
// Create a new object
Data stored = storage.store(new DataObject("id", "value"), "user1");

// Batch create
List<DataObject> batch = List.of(
    new DataObject("id1", "value1"),
    new DataObject("id2", "value2")
);
storage.store(batch, "user1");

// Create with auto-generated ID
Data stored = storage.store(dataObject, "user1");
// ID is generated based on Identifier strategy
```

#### Read
```java
// Get single object
Optional<Data> obj = storage.get("id1");

// Get with metadata
Optional<Metadata<Data>> meta = storage.getMetadata("id1");

// List all objects
List<Data> all = storage.list();
List<Metadata<Data>> allMeta = storage.listMetadata();

// Stream-based iteration
storage.select().forEach(obj -> { /* process */ });
storage.selectMetadata().forEach(meta -> { /* process */ });
```

#### Update
```java
// Update an existing object
Optional<Data> updated = storage.update(
    "id1",
    obj -> {
        obj.setValue("newValue");
        return obj;
    },
    "user1"
);

// Update with fallback creation
Data result = storage.update(
    "id1",
    obj -> { obj.setValue("newValue"); return obj; },
    () -> new DataObject("id1", "default"),
    "user1"
);

// Conditional update (fails if update function returns null)
boolean updated = storage.tryUpdate(
    "id1",
    obj -> {
        if (obj.isValid()) {
            obj.setValue("newValue");
            return obj;
        }
        return null; // Update fails
    },
    "user1"
);
```

#### Delete
```java
// Soft delete (marks as deleted, keeps in storage)
Optional<Data> deleted = storage.delete("id1", "user1");

// Soft delete all
storage.deleteAll();

// Permanent delete (removes completely)
Optional<Data> removed = storage.permanentlyDelete("id1");
storage.permanentlyDelete(); // Clear all
```

### 4. Identifier Strategy

The `Identifier<Id, Data>` manages ID generation and extraction:

```java
// Create identifier that uses object field
Identifier<String, DataObject> id = Identifier
    .<DataObject>forId(
        obj -> obj.id,           // getter
        (obj, id) -> obj.id = id // setter
    )
    .suggestion(obj -> obj.name) // suggestion for auto-generated ID
    .build();

MemoryStorage<String, DataObject> storage = 
    new MemoryStorage<>(id, Storage.Lock.SERIALIZED);
```

### 5. Replication

Support for master-slave replication with automatic change propagation:

```java
// Create master storage (source of truth)
MemoryStorage<String, DataObject> master = 
    new MemoryStorage<>(identifier, Storage.Lock.SERIALIZED);

// Create slave storage (mirror)
MemoryStorage<String, DataObject> slave = 
    new MemoryStorage<>(identifier, Storage.Lock.SERIALIZED);

// Setup replication (polls master every 1000ms)
Replicator<String, DataObject> replicator = 
    new Replicator<>(slave, master, 1000);

// Changes to master are replicated to slave
master.store(obj, "user1");
// After 1000ms, obj appears in slave
```

### 6. Data Listeners

Listen for storage change events:

```java
storage.addDataListener(new Storage.DataListener<String, DataObject>() {
    @Override
    public void added(List<IdObject<String, DataObject>> objects) {
        // Called when objects are added (batched per replication period)
        objects.forEach(io -> log.info("Added: {}", io.id));
    }
    
    @Override
    public void updated(List<IdObject<String, DataObject>> objects) {
        // Called when objects are updated
        objects.forEach(io -> log.info("Updated: {}", io.id));
    }
    
    @Override
    public void deleted(List<IdObject<String, DataObject>> objects) {
        // Called when objects are deleted
        objects.forEach(io -> log.info("Deleted: {}", io.id));
    }
    
    @Override
    public void permanentlyDeleted(IdObject<String, DataObject> object) {
        // Called when object is permanently removed
        log.info("Permanently deleted: {}", object.id);
    }
    
    @Override
    public void changed(List<IdObject<String, DataObject>> added,
                       List<IdObject<String, DataObject>> updated,
                       List<IdObject<String, DataObject>> deleted) {
        // Called with all changes in one batch
        log.info("Changes - Added: {}, Updated: {}, Deleted: {}",
                added.size(), updated.size(), deleted.size());
    }
});
```

## Getting Started

### In-Memory Storage

```java
import oap.storage.*;
import oap.id.Identifier;

public class QuickStart {
    
    // Sample data class
    public static class User {
        public String id;
        public String name;
        public String email;
        
        public User() {}
        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
    
    public static void main(String[] args) {
        // Create identifier strategy
        Identifier<String, User> identifier = Identifier
            .<User>forId(
                u -> u.id,
                (u, id) -> u.id = id
            )
            .suggestion(u -> u.name)
            .build();
        
        // Create in-memory storage
        MemoryStorage<String, User> storage = 
            new MemoryStorage<>(identifier, Storage.Lock.SERIALIZED);
        
        // Store objects
        User user1 = new User("USR1", "Alice", "alice@example.com");
        storage.store(user1, "admin");
        
        User user2 = new User("USR2", "Bob", "bob@example.com");
        storage.store(user2, "admin");
        
        // Retrieve
        Optional<User> retrieved = storage.get("USR1");
        System.out.println("Found: " + retrieved.get().name);
        
        // Get metadata
        Optional<Metadata<User>> metadata = storage.getMetadata("USR1");
        metadata.ifPresent(m -> {
            System.out.println("Created: " + new Date(m.created));
            System.out.println("Modified: " + new Date(m.modified));
            System.out.println("Created by: " + m.createdBy);
        });
        
        // Update
        storage.update("USR1", user -> {
            user.email = "alice.new@example.com";
            return user;
        }, "admin");
        
        // List all
        storage.list().forEach(u -> 
            System.out.println(u.id + ": " + u.name)
        );
        
        // Delete
        storage.delete("USR2", "admin");
        
        // List remaining
        System.out.println("Remaining: " + storage.size());
        
        // Stream API
        storage.select()
            .filter(u -> u.email.endsWith("@example.com"))
            .forEach(u -> System.out.println(u.name));
    }
}
```

### MongoDB Persistence

```java
import oap.storage.*;
import oap.storage.mongo.MongoClient;

public class MongoQuickStart {
    
    public static void main(String[] args) throws Exception {
        // Create MongoDB client
        try (MongoClient mongoClient = new MongoClient(
            "mongodb://localhost:27017/mydb",
            "com.myapp.migrations" // migration package
        )) {
            mongoClient.preStart();
            
            // Create identifier
            Identifier<String, User> identifier = Identifier
                .<User>forId(u -> u.id, (u, id) -> u.id = id)
                .suggestion(u -> u.name)
                .build();
            
            // Create in-memory storage
            MemoryStorage<String, User> storage = 
                new MemoryStorage<>(identifier, Storage.Lock.SERIALIZED);
            
            // Create MongoDB persistence (5-second sync delay)
            try (MongoPersistence<String, User> persistence = 
                    new MongoPersistence<>(mongoClient, "users", 5000, storage)) {
                persistence.preStart();
                
                // All operations on storage are automatically persisted to MongoDB
                User user = new User("USR1", "Alice", "alice@example.com");
                storage.store(user, "admin");
                
                User updated = storage.store(
                    new User("USR1", "Alice Smith", "alice@example.com"), 
                    "admin"
                );
                
                // After 5 seconds, changes are fsynced to MongoDB
                Thread.sleep(6000);
                
                // Verify in MongoDB
                long count = persistence.collection.countDocuments();
                System.out.println("Documents in MongoDB: " + count);
            }
        }
    }
}
```

**Key Features:**
- Asynchronous batch writing to MongoDB
- Automatic crash dump to `/tmp/mongo-persistance-crash-dump` on errors
- Change stream support for watch operations
- Mongock migration framework integration
- Configurable write delay and batch size

### Cloud Storage

```java
import oap.storage.cloud.*;

public class CloudStorageQuickStart {
    
    public static void main(String[] args) throws Exception {
        // Configure cloud file system
        FileSystemConfiguration config = new FileSystemConfiguration();
        config.configuration.put("fs.default.clouds.scheme", "s3");
        config.configuration.put("fs.default.clouds.container", "my-bucket");
        config.configuration.put("fs.s3.clouds.identity", "ACCESS_KEY");
        config.configuration.put("fs.s3.clouds.credential", "SECRET_KEY");
        
        // Create file system
        try (FileSystem fileSystem = new FileSystem(config)) {
            // Async read from S3
            CloudURI uri = new CloudURI("s3://my-bucket/data/file.json");
            CompletableFuture<? extends InputStream> futureStream = 
                fileSystem.getInputStreamAsync(uri);
            
            try (InputStream is = futureStream.get()) {
                // Process stream
                byte[] data = is.readAllBytes();
                System.out.println("Read " + data.length + " bytes from S3");
            }
            
            // Async write to S3
            CloudURI outputUri = new CloudURI("s3://my-bucket/output/result.json");
            try (OutputStream os = fileSystem.getOutputStream(outputUri)) {
                os.write("Hello, Cloud!".getBytes());
            }
            
            // List operations
            ListOptions options = new ListOptions();
            PageSet<BlobData> blobs = fileSystem.list(
                new CloudURI("s3://my-bucket/data/"), 
                options
            );
            blobs.getItems().forEach(blob ->
                System.out.println("Blob: " + blob.getName())
            );
        }
    }
}
```

## Configuration Reference

### MongoDB Configuration

Configure in `oap-module.conf`:

```conf
name = oap-storage-mongo
services {
  mongo-client {
    implementation = oap.storage.mongo.MongoClient
    parameters {
      connectionString = "mongodb://host:port/database"
      throwIfMigrationFailed = true
      # Optional: migration package for DB schema migrations
      # migrationPackage = "com.myapp.migrations"
    }
    supervision.supervise = true
  }
}
```

**Connection String Formats:**
```
# Basic
mongodb://localhost:27017/mydb

# With authentication
mongodb://username:password@host:port/database

# Replica set
mongodb://host1:27017,host2:27017,host3:27017/database?replicaSet=rs0

# With options
mongodb://localhost:27017/mydb?maxPoolSize=10&serverSelectionTimeoutMS=5000
```

### Cloud Storage Configuration

Configure in `oap-module.conf`:

```conf
name = oap-storage-cloud
services {
  oap-cloud-configuration {
    implementation = oap.storage.cloud.FileSystemConfiguration
    parameters {
      configuration = {
        # AWS S3 Configuration
        fs.s3.clouds.identity = "YOUR_ACCESS_KEY"
        fs.s3.clouds.credential = "YOUR_SECRET_KEY"
        # fs.s3.clouds.endpoint = "http://localhost:9000"  # For MinIO
        fs.s3.clouds.virtual-host-buckets = false
        
        # Default scheme and bucket
        fs.default.clouds.scheme = s3
        fs.default.clouds.container = my-bucket
      }
    }
  }
}
```

### Persistence Configuration

```java
// Customize MongoPersistence
MongoPersistence<String, User> persistence = 
    new MongoPersistence<>(
        mongoClient,
        "users",           // Collection name
        5000,              // Sync delay in milliseconds
        storage,
        Path.of("/var/crash-dumps")  // Custom crash dump path
    );

persistence.batchSize = 100;  // Default batch size for writes
persistence.watch = true;     // Enable change stream watching
```

## API Reference

### Storage Interface

#### Query Operations

```java
// Get stream of all live objects
Stream<Data> select();

// Get stream with metadata
Stream<Metadata<Data>> selectMetadata();

// Get list of all objects
List<Data> list();

// Get list with metadata
List<Metadata<Data>> listMetadata();

// Get single object by ID
Optional<Data> get(Id id);

// Get or create
Data get(Id id, Supplier<Data> init, String modifiedBy);

// Get total count
long size();

// Iterate
void forEach(Consumer<? super Data> action);
```

#### Store Operations

```java
// Store single object
Data store(Data object, String modifiedBy);

// Store multiple objects
void store(Collection<Data> objects, String modifiedBy);

// Store with hash-based consistency (for RemoteStorage)
Data store(Data object, long hash);
```

#### Update Operations

```java
// Update if exists
Optional<Data> update(Id id, Function<Data, Data> update, String modifiedBy);

// Update or create
Data update(Id id, Function<Data, Data> update, 
           Supplier<Data> init, String modifiedBy);

// Conditional update (fails if update returns null)
boolean tryUpdate(Id id, Function<Data, Data> tryUpdate, String modifiedBy);
```

#### Delete Operations

```java
// Soft delete (marks as deleted)
Optional<Data> delete(Id id, String modifiedBy);

// Soft delete with metadata
Optional<Metadata<Data>> deleteMetadata(Id id, String modifiedBy);

// Hard delete
Optional<Data> permanentlyDelete(Id id);

// Delete all
void deleteAll();

// Permanently delete all
void permanentlyDelete();
```

### RemoteStorage Interface

For distributed storage with hash-based optimistic concurrency:

```java
public interface RemoteStorage<Id, Data> extends ReplicationMaster<Id, Data> {
    Optional<Data> get(Id id);
    Optional<Metadata<Data>> getMetadata(Id id);
    long size();
    
    // Store with hash-based CAS (Compare-And-Swap)
    @Nullable
    Data store(Data object, long hash);
    
    // Convenience method for find-and-modify pattern
    default Data findAndModify(Id id, Function<Data, Data> func, 
                               int retry) throws StorageException {
        // Atomically read, modify, and write with retry on conflict
    }
}
```

Example:

```java
// Read current value and hash
Metadata<Data> metadata = remoteStorage.getMetadata(id).orElse(null);
long currentHash = metadata != null ? metadata.hash : 0;

// Modify
Data modified = modifyFunction.apply(metadata != null ? metadata.object : null);

// Try to store with hash check (fails if hash changed)
Data stored = remoteStorage.store(modified, currentHash);
if (stored == null) {
    // Hash mismatch - someone else modified it, retry
}
```

### Metadata Class

```java
public class Metadata<T> {
    public long modified;           // Last modification timestamp (ms)
    public long created;            // Creation timestamp (ms)
    public String createdBy;        // Creator identifier
    public String modifiedBy;       // Last modifier identifier
    public long hash;               // Object hash for consistency
    public T object;                // The actual object
    
    public boolean isDeleted();     // Check if soft deleted
    public void delete(String by);  // Soft delete with modifier
    
    // Check if looks unmodified
    public boolean looksUnmodified(Metadata<T> other);
    
    // Refresh metadata (recalculate hash and timestamp)
    public void refresh();
}
```

### DataListener Interface

For listening to storage changes:

```java
public interface DataListener<DI, D> {
    // Called when objects are added
    default void added(List<IdObject<DI, D>> objects) {}
    
    // Called when objects are updated
    default void updated(List<IdObject<DI, D>> objects) {}
    
    // Called when objects are deleted
    default void deleted(List<IdObject<DI, D>> objects) {}
    
    // Called when object is permanently deleted
    default void permanentlyDeleted(IdObject<DI, D> object) {}
    
    // Called with all changes in one batch
    default void changed(List<IdObject<DI, D>> added,
                        List<IdObject<DI, D>> updated,
                        List<IdObject<DI, D>> deleted) {}
    
    // Wrapper class
    class IdObject<DI, D> {
        public final DI id;
        public final Metadata<D> metadata;
    }
}
```

### Replicator

For master-slave replication:

```java
// Create replicator (polls master every interval ms)
Replicator<String, DataObject> replicator = 
    new Replicator<>(slave, master, 1000);

// Get replication statistics
long stored = Replicator.stored.get();
long deleted = Replicator.deleted.get();

// Reset statistics
Replicator.reset();

// Cleanup
replicator.close();
```

## Examples from Tests

### MemoryStorageTest - Basic CRUD

```java
// Create storage
MemoryStorage<String, Bean> storage = new MemoryStorage<>(
    Identifier.<Bean>forId(b -> b.id, (b, id) -> b.id = id)
        .suggestion(b -> b.s)
        .build(),
    SERIALIZED);

// Add data listener
List<String> addedIds = new ArrayList<>();
storage.addDataListener(new Storage.DataListener<>() {
    @Override
    public void added(List<IdObject<String, Bean>> objects) {
        objects.forEach(io -> addedIds.add(io.id));
    }
});

// Store
Bean bean1 = new Bean("id1");
storage.store(bean1, Storage.MODIFIED_BY_SYSTEM);

// Verify listener was called
assertThat(addedIds).contains("id1");

// Update
storage.update("id1", bean -> {
    bean.value = "newValue";
    return bean;
}, "user1");

// Delete
storage.delete("id1", "user1");

// List
List<Bean> remaining = storage.list();
```

### MongoPersistenceTest - Persistence

```java
// Create MongoDB-backed storage
MemoryStorage<String, Bean> storage = 
    new MemoryStorage<>(beanIdentifier, SERIALIZED);

try (MongoClient mongoClient = mongoFixture
        .createMongoClient("my.migration.package");
     MongoPersistence<String, Bean> persistence = 
        new MongoPersistence<>(mongoClient, "test", 5000, storage)) {
    
    mongoClient.preStart();
    persistence.preStart();
    
    // Store objects - automatically persisted
    Bean bean1 = storage.store(new Bean("test1"), 
        Storage.MODIFIED_BY_SYSTEM);
    Bean bean2 = storage.store(new Bean("test2"), 
        Storage.MODIFIED_BY_SYSTEM);
    
    // Update
    storage.store(new Bean(bean2.id, "test3"), 
        Storage.MODIFIED_BY_SYSTEM);
    
    // Wait for fsync
    Thread.sleep(6000);
    
    // Verify in MongoDB
    assertThat(persistence.collection.countDocuments()).isEqualTo(2);
}

// Create new storage and reload from MongoDB
MemoryStorage<String, Bean> storage2 = 
    new MemoryStorage<>(beanIdentifier, SERIALIZED);

try (MongoClient mongoClient = mongoFixture
        .createMongoClient("my.migration.package");
     MongoPersistence<String, Bean> persistence = 
        new MongoPersistence<>(mongoClient, "test", 5000, storage2)) {
    
    mongoClient.preStart();
    persistence.preStart();
    
    // Data is automatically loaded from MongoDB
    assertThat(storage2.select()).containsOnly(
        new Bean("TST1", "test1"),
        new Bean("TST2", "test3")
    );
}
```

### Tracking Metadata

```java
// Create storage
MemoryStorage<String, Bean> storage = new MemoryStorage<>(
    Identifier.<Bean>forId(b -> b.id, (b, id) -> b.id = id)
        .suggestion(b -> b.s)
        .build(),
    SERIALIZED);

DateTime created = new DateTime(2024, 1, 14, 15, 13, 14, UTC);
Dates.setTimeFixed(created.getMillis());

// Store
storage.store(new Bean("id1"), Storage.MODIFIED_BY_SYSTEM);

// Check metadata
Metadata<Bean> metadata = storage.getMetadata("id1").orElseThrow();
assertThat(new DateTime(metadata.created, UTC)).isEqualTo(created);
assertThat(new DateTime(metadata.modified, UTC)).isEqualTo(created);
assertThat(metadata.createdBy).isEqualTo(Storage.MODIFIED_BY_SYSTEM);

// Update after delay
Dates.incFixed(Dates.m(3)); // Add 3 minutes
storage.store(new Bean("id1", "v"), Storage.MODIFIED_BY_SYSTEM);

metadata = storage.getMetadata("id1").orElseThrow();
assertThat(new DateTime(metadata.created, UTC)).isEqualTo(created);
assertThat(new DateTime(metadata.modified, UTC))
    .isEqualTo(created.plusMinutes(3));
```

## Threading and Concurrency

The storage framework provides two locking strategies:

```java
// Concurrent locking (no synchronization)
Storage.Lock lock = Storage.Lock.CONCURRENT;
MemoryStorage<String, Data> storage = 
    new MemoryStorage<>(identifier, lock);

// Serialized locking (synchronized on object ID)
Storage.Lock lock = Storage.Lock.SERIALIZED;
MemoryStorage<String, Data> storage = 
    new MemoryStorage<>(identifier, lock);
```

**Important**: Use `SERIALIZED` for correct concurrent updates. The storage framework automatically handles per-ID synchronization to prevent race conditions.

## Performance Considerations

1. **Batch Operations**: Use batch store for multiple objects
   ```java
   storage.store(largeCollection, "user");
   ```

2. **Streaming**: Use streams instead of `list()` for large datasets
   ```java
   storage.select()
       .filter(predicate)
       .limit(1000)
       .forEach(processor);
   ```

3. **MongoDB Sync Delay**: Higher values reduce disk I/O, lower values reduce data loss risk
   ```java
   // Higher delay = better performance but higher risk
   new MongoPersistence(client, "collection", 10000, storage);
   ```

4. **Batch Size**: Tune for your data and hardware
   ```java
   persistence.batchSize = 500;  // Default is 100
   ```

5. **Replication**: Only use when needed, it adds overhead
   ```java
   Replicator<String, Data> replicator = 
       new Replicator<>(slave, master, 5000); // 5-second poll
   ```

## Error Handling

```java
// StorageException for consistency failures
try {
    Data result = remoteStorage.findAndModify(id, 
        data -> { /* modify */ },
        3  // retry 3 times
    );
} catch (StorageException e) {
    // Failed after retries
    log.error("Failed to modify object", e);
}

// MongoDB persistence uses crash dumps
try (MongoPersistence<String, Data> persistence = 
        new MongoPersistence<>(client, "collection", 5000, storage,
            Path.of("/var/crash-dumps"))) {
    // On error, objects are dumped to /var/crash-dumps/collection
}
```

## License

MIT License - See LICENSE file in repository

## Contributing

Contributions are welcome! Please follow the OAP contribution guidelines.

## Support

- Check test files in `oap-storage-*-test` modules for usage examples
- Review configuration examples in `oap-module.conf` files
- See OAP documentation for integration patterns

