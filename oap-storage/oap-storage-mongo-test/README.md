# oap-storage-mongo-test

Test utilities for `oap-storage-mongo`. Provides an in-memory MongoDB server so tests run without a real MongoDB instance.

Depends on: `oap-storage-mongo`

## `MongoFixture`

Starts a `de.bwaldvogel` in-memory MongoDB server (no Docker) and exposes a connected `MongoClient`. Implements `AbstractFixture` — wire it with TestNG `@Listeners(Fixtures.class)`.

```java
@Listeners( Fixtures.class )
public class OrderRepositoryTest {

    @Fixture
    MongoFixture mongo = new MongoFixture( "orders_db" );

    @Test
    public void storesOrder() {
        MongoClient client = mongo.client();

        MemoryStorage<String, Order> storage = new MemoryStorage<>( orderId, Lock.SERIALIZED, 1000 );
        MongoPersistence<String, Order> persistence = new MongoPersistence<>(
            client, "orders", 100, storage
        );
        persistence.preStart();

        storage.store( new Order( "o-1", "item-A" ), "test" );
        persistence.fsync();

        assertThat( storage.size() ).isEqualTo( 1 );
    }
}
```

### Fixture variables

These are available as template variables in `application.conf` and `oap-module.oap` files loaded by `KernelFixture`:

| Variable | Value |
|---|---|
| `PORT` | Randomly assigned port |
| `HOST` | `localhost` |
| `DATABASE` | Database name passed to constructor |

Default database name is `"test"` when using the no-arg constructor.

### API

| Method | Description |
|---|---|
| `client()` | Returns the connected `MongoClient` for the fixture database |
| `createMongoClient()` | Create a new `MongoClient` connected to the fixture database |
| `createMongoClient(migrationPackage, ...)` | Create a `MongoClient` that runs Mongock migrations from the given packages on `preStart()` |
| `getConnectionString()` | `mongodb://localhost:<PORT>/<DATABASE>` |
| `getConnectionString(database)` | Connection string for an alternate database |
| `insertDocument(contextClass, collection, resourceName)` | Insert a JSON document loaded from a test resource file |
| `initializeVersion(version)` | Write a `Version` record into the `version` collection |
| `dropDatabase(database)` | Drop an alternate database by name |

### Multi-database tests

```java
MongoFixture mongo = new MongoFixture( "primary" );

// Connect to a second database on the same in-memory server
MongoClient secondary = mongo.createMongoClient();
// or with a specific database name:
String connStr = mongo.getConnectionString( "secondary_db" );
```

### Seeding from JSON files

```java
// Inserts the content of src/test/resources/oap/storage/mongo/order.json
mongo.insertDocument( OrderRepositoryTest.class, "orders", "order.json" );
```
