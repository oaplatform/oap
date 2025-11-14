# Getting Started with OAP

This guide walks you through creating your first OAP application from scratch. By the end, you'll have a working web service with storage, configuration, and lifecycle management.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Basic understanding of Java and Maven

## What You'll Build

A simple REST API for managing tasks (a todo list) with:
- HTTP endpoints for CRUD operations
- In-memory storage with persistence
- Configuration-based setup
- Lifecycle management

## Step 1: Create a Maven Project

Create a new Maven project:

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=todo-app \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false

cd todo-app
```

## Step 2: Configure Dependencies

Edit `pom.xml` and add OAP dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>todo-app</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <oap.version>24.3.2</oap.version>
    </properties>

    <repositories>
        <repository>
            <id>oap</id>
            <url>https://artifacts.oaplatform.org/repository/oap-maven/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- OAP Application Framework -->
        <dependency>
            <groupId>oap</groupId>
            <artifactId>oap-application</artifactId>
            <version>${oap.version}</version>
        </dependency>

        <!-- OAP Web Services -->
        <dependency>
            <groupId>oap</groupId>
            <artifactId>oap-ws</artifactId>
            <version>${oap.version}</version>
        </dependency>

        <!-- OAP Storage -->
        <dependency>
            <groupId>oap</groupId>
            <artifactId>oap-storage</artifactId>
            <version>${oap.version}</version>
        </dependency>

        <!-- Lombok (optional, but recommended) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>

        <!-- SLF4J for logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.11</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 3: Create the Domain Model

Create `src/main/java/com/example/Task.java`:

```java
package com.example;

import lombok.Data;
import oap.storage.Id;

@Data
public class Task {
    @Id
    public String id;
    public String title;
    public String description;
    public boolean completed = false;
    public long createdAt;

    public Task() {
        this.createdAt = System.currentTimeMillis();
    }

    public Task(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }
}
```

**Key points:**
- `@Id` annotation marks the identifier field
- `@Data` from Lombok generates getters/setters/toString/equals/hashCode
- Fields are public for JSON serialization (OAP convention)

## Step 4: Create the Web Service

Create `src/main/java/com/example/TaskService.java`:

```java
package com.example;

import lombok.extern.slf4j.Slf4j;
import oap.storage.MemoryStorage;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static oap.http.Http.StatusCode.*;
import static oap.ws.WsParam.From.*;

@Slf4j
public class TaskService {
    private final MemoryStorage<Task> storage;

    public TaskService(MemoryStorage<Task> storage) {
        this.storage = storage;
        log.info("TaskService initialized");
    }

    @WsMethod(path = "/tasks", method = "GET", produces = "application/json")
    public List<Task> listTasks() {
        log.debug("Listing all tasks");
        return storage.list();
    }

    @WsMethod(path = "/tasks/{id}", method = "GET", produces = "application/json")
    public Optional<Task> getTask(@WsParam(from = PATH) String id) {
        log.debug("Getting task: {}", id);
        return storage.get(id);
    }

    @WsMethod(path = "/tasks", method = "POST", produces = "application/json")
    public Task createTask(@WsParam(from = BODY) Task task) {
        if (task.id == null || task.id.isEmpty()) {
            task.id = UUID.randomUUID().toString();
        }
        log.info("Creating task: {}", task.id);
        storage.store(task);
        return task;
    }

    @WsMethod(path = "/tasks/{id}", method = "PUT", produces = "application/json")
    public Optional<Task> updateTask(
            @WsParam(from = PATH) String id,
            @WsParam(from = BODY) Task task) {
        log.info("Updating task: {}", id);
        task.id = id;
        storage.store(task);
        return Optional.of(task);
    }

    @WsMethod(path = "/tasks/{id}", method = "DELETE")
    public void deleteTask(@WsParam(from = PATH) String id) {
        log.info("Deleting task: {}", id);
        storage.delete(id);
    }

    // Lifecycle methods
    public void start() {
        log.info("TaskService started with {} tasks", storage.size());
    }

    public void stop() {
        log.info("TaskService stopped");
    }
}
```

**Key annotations:**
- `@WsMethod` - Defines an HTTP endpoint
- `@WsParam` - Binds method parameters to HTTP request data
- `path` - URL pattern (supports {variables})
- `method` - HTTP method (GET, POST, PUT, DELETE)
- `produces` - Response content type

## Step 5: Create Module Configuration

Create `src/main/resources/META-INF/oap-module.conf`:

```hocon
name = todo-app

services {
  # Storage for tasks
  task-storage {
    implementation = oap.storage.MemoryStorage
    parameters {
      # Directory for persistence
      fsDirectory = ${TASK_STORAGE_PATH}
      # Lock policy: SERIALIZED, CONCURRENT, UNLOCKED
      lock = SERIALIZED
    }
    supervision {
      supervise = true
    }
  }

  # Task web service
  task-service {
    implementation = com.example.TaskService
    parameters {
      storage = <modules.this.task-storage>
    }
    supervision {
      supervise = true
    }
  }

  # Web service registration
  task-ws {
    implementation = oap.ws.WebServices
    parameters {
      services = [<modules.this.task-service>]
    }
  }
}
```

**Configuration explained:**
- `name` - Unique module identifier
- `services` - Map of service definitions
- `implementation` - Fully qualified class name
- `parameters` - Constructor/field injection values
- `<modules.this.*>` - Service references within same module
- `supervision.supervise = true` - Enable lifecycle methods (start/stop)

## Step 6: Create Application Configuration

Create `application.conf` in your project root:

```hocon
boot {
  # Modules to load on startup
  main = [
    oap-http
    oap-ws
    todo-app
  ]
}

# Service configuration overrides
services {
  oap-http {
    oap-http-server {
      parameters {
        defaultPort {
          httpPort = 8080
        }
      }
    }
  }

  todo-app {
    task-storage {
      parameters {
        fsDirectory = "./data/tasks"
      }
    }
  }
}
```

**Configuration sections:**
- `boot.main` - List of modules to start
- `services.[module].[service].parameters` - Override module configuration

## Step 7: Create the Main Application

Create `src/main/java/com/example/TodoApp.java`:

```java
package com.example;

import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TodoApp {
    public static void main(String[] args) throws Exception {
        log.info("Starting Todo Application...");

        // Find module configurations on classpath
        List<URL> moduleConfigs = new ArrayList<>();

        // OAP modules
        addModuleConfig(moduleConfigs, "oap-http");
        addModuleConfig(moduleConfigs, "oap-ws");

        // Our application module
        addModuleConfig(moduleConfigs, "META-INF/oap-module.conf");

        // Create and start kernel
        Kernel kernel = new Kernel("todo-app", moduleConfigs);
        kernel.start(Paths.get("application.conf"));

        log.info("Todo Application started successfully!");
        log.info("API available at: http://localhost:8080/tasks");

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            kernel.stop();
            log.info("Shutdown complete");
        }));

        // Keep running
        Thread.currentThread().join();
    }

    private static void addModuleConfig(List<URL> configs, String name) {
        URL url = TodoApp.class.getClassLoader().getResource(
            "META-INF/" + name + "/oap-module.conf"
        );
        if (url == null) {
            url = TodoApp.class.getClassLoader().getResource(name);
        }
        if (url != null) {
            configs.add(url);
        } else {
            log.warn("Module configuration not found: {}", name);
        }
    }
}
```

## Step 8: Create Logging Configuration (Optional)

Create `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- Your application logging -->
    <logger name="com.example" level="DEBUG" />

    <!-- OAP framework logging -->
    <logger name="oap" level="INFO" />
</configuration>
```

## Step 9: Build and Run

Build the application:

```bash
mvn clean package
```

Run the application:

```bash
mvn exec:java -Dexec.mainClass="com.example.TodoApp"
```

You should see output like:

```
12:34:56.789 [main] INFO  com.example.TodoApp - Starting Todo Application...
12:34:57.123 [main] INFO  oap.application.Kernel - initializing application kernel todo-app...
12:34:57.456 [main] INFO  com.example.TaskService - TaskService initialized
12:34:57.789 [main] INFO  com.example.TaskService - TaskService started with 0 tasks
12:34:58.012 [main] INFO  com.example.TodoApp - Todo Application started successfully!
12:34:58.013 [main] INFO  com.example.TodoApp - API available at: http://localhost:8080/tasks
```

## Step 10: Test Your API

Use curl to test the endpoints:

**Create a task:**
```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learn OAP",
    "description": "Complete the getting started guide",
    "completed": false
  }'
```

**List all tasks:**
```bash
curl http://localhost:8080/tasks
```

**Get a specific task:**
```bash
curl http://localhost:8080/tasks/{task-id}
```

**Update a task:**
```bash
curl -X PUT http://localhost:8080/tasks/{task-id} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learn OAP",
    "description": "Complete the getting started guide",
    "completed": true
  }'
```

**Delete a task:**
```bash
curl -X DELETE http://localhost:8080/tasks/{task-id}
```

## Project Structure

Your final project structure:

```
todo-app/
├── pom.xml
├── application.conf
├── data/
│   └── tasks/                          # Storage directory (created automatically)
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           ├── Task.java       # Domain model
        │           ├── TaskService.java # Web service
        │           └── TodoApp.java    # Main application
        └── resources/
            ├── logback.xml             # Logging configuration
            └── META-INF/
                └── oap-module.conf     # Module configuration
```

## What You've Learned

1. **OAP Application Structure** - Module and service configuration
2. **Dependency Injection** - Service references and wiring
3. **Web Services** - REST API with `@WsMethod` and `@WsParam`
4. **Storage** - In-memory storage with file persistence
5. **Lifecycle Management** - Automatic start/stop handling
6. **Configuration** - HOCON-based configuration system

## Next Steps

### Add Validation

Add input validation to your web service:

```java
import oap.ws.validate.WsValidate;

@WsMethod(path = "/tasks", method = "POST")
@WsValidate({"required:title", "required:description"})
public Task createTask(@WsParam(from = BODY) Task task) {
    // ...
}
```

### Add Persistence

Replace `MemoryStorage` with MongoDB:

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-storage-mongo</artifactId>
    <version>${oap.version}</version>
</dependency>
```

Update `oap-module.conf`:

```hocon
task-storage {
    implementation = oap.storage.mongo.MongoPersistence
    parameters {
        database = "tododb"
        collection = "tasks"
        mongoClient = <modules.oap-storage-mongo.oap-mongo-client>
    }
}
```

### Add Authentication

Integrate with oap-ws-sso for session management:

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-ws-sso</artifactId>
    <version>${oap.version}</version>
</dependency>
```

### Add Metrics

Monitor your application with Prometheus:

```hocon
boot.main = [
    oap-http
    oap-http-prometheus
    oap-ws
    todo-app
]
```

Access metrics at: `http://localhost:8081/metrics`

### Add Health Checks

Implement custom health checks:

```java
import oap.http.server.nio.health.HealthProvider;

public class TaskHealthProvider implements HealthProvider {
    private final MemoryStorage<Task> storage;

    @Override
    public String name() {
        return "tasks";
    }

    @Override
    public boolean isAlive() {
        return storage != null && storage.size() >= 0;
    }
}
```

### Add Testing

Create tests using oap-application-test:

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-application-test</artifactId>
    <version>${oap.version}</version>
    <scope>test</scope>
</dependency>
```

```java
import oap.application.testng.KernelFixture;
import org.testng.annotations.Test;

public class TaskServiceTest extends KernelFixture {
    @Test
    public void testCreateTask() {
        TaskService service = kernel.service("todo-app", "task-service").get();

        Task task = new Task("Test", "Description");
        Task created = service.createTask(task);

        assertNotNull(created.id);
        assertEquals("Test", created.title);
    }
}
```

## Common Issues

### Port Already in Use

If port 8080 is busy, change it in `application.conf`:

```hocon
services.oap-http.oap-http-server.parameters.defaultPort.httpPort = 9090
```

### Module Not Found

Ensure your `oap-module.conf` is in the correct location:
- Must be in `src/main/resources/META-INF/oap-module.conf`
- Will be packaged at `META-INF/oap-module.conf` in JAR

### Service Not Starting

Check logs for:
- Circular dependencies
- Missing service references
- Invalid configuration syntax

Enable debug logging:

```xml
<logger name="oap.application" level="DEBUG" />
```

## Resources

- [OAP Application Documentation](../oap-application/README.md)
- [OAP Web Services Documentation](../oap-ws/README.md)
- [OAP Storage Documentation](../oap-storage/README.md)
- [Developer Guide](developer-guide.md)
- [Configuration Reference](configuration-reference.md)

## Summary

You've successfully created a complete OAP application with:
- ✅ REST API endpoints
- ✅ Data persistence
- ✅ Configuration management
- ✅ Lifecycle management
- ✅ Dependency injection

The OAP framework handled:
- HTTP server setup
- Request routing and parsing
- JSON serialization
- Service lifecycle
- Configuration loading

Continue to the [Developer Guide](developer-guide.md) for advanced topics and best practices.
