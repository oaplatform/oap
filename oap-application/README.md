# OAP Application

## Overview

The OAP Application module provides the core application framework for building modular, lifecycle-managed Java applications. It implements a lightweight dependency injection container with supervision capabilities inspired by Erlang/OTP.

The framework manages:
- **Module loading and configuration** via HOCON files
- **Service lifecycle** (instantiation, linking, starting, stopping)
- **Dependency injection** through configuration
- **Supervision** (automatic restart, scheduling, threading)
- **Service references** and inter-module dependencies

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-application</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Sub-modules

- **oap-application**: Core framework (Kernel, Module, Service)
- **oap-application-cli**: Command-line interface support
- **oap-application-test**: Testing utilities for OAP applications

## Dependencies

Required OAP modules:
- `oap-stdlib` - Core utilities and libraries
- `oap-http` - HTTP server support
- `oap-remote` - Remote communication support
- `oap-application-cli` - CLI support

External dependencies:
- TypeSafe Config (HOCON configuration)
- Jackson (JSON/configuration binding)
- SLF4J (logging)
- Lombok (code generation)

## Core Concepts

### 1. Kernel

The `Kernel` is the main application container that manages the complete lifecycle of modules and services.

```java
// Create kernel with module configurations
Kernel kernel = new Kernel(List.of(
    moduleConfigUrl1,
    moduleConfigUrl2
));

// Start with application configuration
kernel.start(Paths.get("application.conf"));

// Access services
Optional<MyService> service = kernel.service("my-module", "my-service");

// Stop kernel
kernel.stop();
```

**Key responsibilities:**
- Load modules from `oap-module.conf` files
- Parse application configuration (`application.conf`)
- Instantiate services with dependency injection
- Link services together
- Start services with supervision
- Stop services gracefully

### 2. Module

A `Module` represents a group of related services with dependencies. Modules are defined in `META-INF/oap-module.conf` files.

**Module structure:**
```hocon
name = my-module

# Optional: module dependencies
dependsOn = [other-module]

# Module activation
activation {
  activeByDefault = false
}

services {
  my-service {
    implementation = com.example.MyService
    # ... service configuration ...
  }
}
```

**Module properties:**
- `name`: Unique module identifier
- `dependsOn`: List of required modules
- `activation.activeByDefault`: Whether module loads automatically
- `services`: Map of service definitions
- `enabled`: Enable/disable entire module

### 3. Service

A `Service` represents a configured instance of a class with lifecycle management.

**Service configuration:**
```hocon
services {
  my-service {
    # Required: fully qualified class name
    implementation = com.example.MyServiceImpl

    # Optional: enable/disable service
    enabled = true

    # Dependency injection parameters
    parameters {
      configValue = "some-value"
      timeout = 30s
      # Service reference
      dependency = <modules.other-module.other-service>
    }

    # Lifecycle supervision
    supervision {
      supervise = true    # Call preStart/start/preStop/stop
      thread = false      # Run as background thread
      schedule = false    # Run on schedule
      delay = 0           # Fixed delay scheduling (milliseconds)
      cron = null         # Cron expression

      # Lifecycle method names (optional)
      preStartWith = preStart
      startWith = start
      preStopWith = preStop
      stopWith = stop
    }

    # Service dependencies (must start before this)
    dependsOn = [other-service]

    # Listen to events from other services
    listen {
      event = <modules.other-module.event-source>
    }

    # Link this service to other services
    link {
      observer = <modules.other-module.observable>
    }
  }
}
```

### 4. Service References

Services reference each other using the format: `<modules.[module].[service]>`

**Reference examples:**
```hocon
# Reference service in another module
dependency = <modules.my-module.my-service>

# Reference service in same module
dependency = <modules.this.my-service>

# Reference from any module (searches all modules)
dependency = <modules.*.my-service>
```

### 5. Abstract Services

Abstract services define interfaces that can have different implementations.

**Module configuration:**
```hocon
services {
  # Define abstract service
  storage {
    abstract = true
    implementation = com.example.Storage
    # Optional default implementation
    default = <modules.this.file-storage>
  }

  # Provide implementations
  file-storage {
    implementation = com.example.FileStorage
  }

  memory-storage {
    enabled = false
    implementation = com.example.MemoryStorage
  }

  # Use abstract service
  app {
    implementation = com.example.App
    parameters {
      storage = <modules.this.storage>
    }
  }
}
```

**Application configuration (select implementation):**
```hocon
boot.main = [my-module]

services {
  my-module.storage = <modules.my-module.memory-storage>
}
```

For detailed abstract service documentation, see [README-abstract-service.md](README-abstract-service.md).

### 6. Application Configuration

The `application.conf` file configures the application at runtime.

**Structure:**
```hocon
boot {
  # List of modules to load and start
  main = [
    module1
    module2
  ]

  # Allow modules with activeByDefault = true to load
  allowActiveByDefault = true
}

shutdown {
  # Timeout for service shutdown warning
  serviceTimeout = 5s

  # Proceed to next service if shutdown times out
  serviceAsyncShutdownAfterTimeout = false
}

services {
  # Configure specific services
  module-name {
    service-name {
      # Override parameters
      timeout = 60s
      enabled = true
    }

    # Override abstract service implementation
    abstract-service = <modules.module-name.concrete-service>
  }
}
```

For more details, see [README-application-configuration.md](README-application-configuration.md).

## Getting Started

### 1. Create a Module Configuration

Create `src/main/resources/META-INF/oap-module.conf`:

```hocon
name = hello-world

services {
  greeter {
    implementation = com.example.Greeter
    parameters {
      greeting = "Hello"
    }
    supervision {
      supervise = true
    }
  }

  app {
    implementation = com.example.App
    parameters {
      greeter = <modules.this.greeter>
    }
    supervision {
      supervise = true
      thread = true
    }
  }
}
```

### 2. Implement Services

```java
package com.example;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Greeter {
    private final String greeting;

    public Greeter(String greeting) {
        this.greeting = greeting;
    }

    public void greet(String name) {
        log.info("{}, {}!", greeting, name);
    }

    // Lifecycle methods
    public void preStart() {
        log.info("Greeter pre-starting...");
    }

    public void start() {
        log.info("Greeter started");
    }

    public void preStop() {
        log.info("Greeter pre-stopping...");
    }

    public void stop() {
        log.info("Greeter stopped");
    }
}

@Slf4j
public class App implements Runnable {
    private final Greeter greeter;
    private volatile boolean running = true;

    public App(Greeter greeter) {
        this.greeter = greeter;
    }

    @Override
    public void run() {
        log.info("App started");
        while (running) {
            greeter.greet("World");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void preStop() {
        running = false;
    }
}
```

### 3. Create Application Configuration

Create `application.conf`:

```hocon
boot {
  main = [hello-world]
}
```

### 4. Start the Application

```java
import oap.application.Kernel;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        URL moduleConfig = Main.class.getClassLoader()
            .getResource("META-INF/oap-module.conf");

        Kernel kernel = new Kernel(List.of(moduleConfig));
        kernel.start(Paths.get("application.conf"));

        // Application runs until interrupted
        Runtime.getRuntime().addShutdownHook(new Thread(kernel::stop));

        Thread.currentThread().join();
    }
}
```

## Supervision

The supervision system manages service lifecycle automatically.

### Lifecycle Methods

Services can implement these optional methods:

```java
public class MyService {
    public void preStart() { }  // Called before start()
    public void start() { }     // Called to start service
    public void preStop() { }   // Called before stop()
    public void stop() { }      // Called to stop service
}
```

### Supervision Modes

**1. Supervised Service** (`supervision.supervise = true`)
```hocon
supervision {
  supervise = true
  preStartWith = preStart  # Optional: custom method name
  startWith = start
  preStopWith = preStop
  stopWith = stop
}
```

Calls lifecycle methods in order:
1. `preStart()` on startup
2. `start()` on startup
3. `preStop()` on shutdown
4. `stop()` on shutdown

**2. Thread Service** (`supervision.thread = true`)
```hocon
supervision {
  supervise = true
  thread = true
}
```

Runs the service's `run()` method in a background thread. The service must implement `Runnable`.

**3. Scheduled Service** (fixed delay)
```hocon
supervision {
  supervise = true
  schedule = true
  delay = 5000  # milliseconds
}
```

Runs the service's `run()` method repeatedly with fixed delay between executions.

**4. Cron Scheduled Service**
```hocon
supervision {
  supervise = true
  schedule = true
  cron = "0 0 * * * ?"  # Every hour
}
```

Runs the service's `run()` method on a cron schedule.

## Service Linking

### Parameters Linking

Inject service references via constructor or field parameters:

```hocon
services {
  database {
    implementation = com.example.Database
  }

  repository {
    implementation = com.example.Repository
    parameters {
      # Constructor parameter
      database = <modules.this.database>
    }
  }

  service {
    implementation = com.example.Service
    parameters {
      # Field parameter
      repository = <modules.this.repository>

      # List parameter
      handlers = [
        <modules.this.handler1>
        <modules.this.handler2>
      ]
    }
  }
}
```

### Link Configuration

The `link` property links this service to other services by calling setter methods or adding to collections:

```hocon
services {
  observer {
    implementation = com.example.Observer
    link {
      # Calls observable.addObserver(this) or observable.setObserver(this)
      observer = <modules.this.observable>
    }
  }
}
```

The framework looks for these methods in order:
1. `add[FieldName](Object o)` - e.g., `addObserver(Observer o)`
2. `set[FieldName](Object o)` - e.g., `setObserver(Observer o)`
3. `add[FieldName]Listener(Object o)` - e.g., `addObserverListener(Observer o)`
4. Field access - e.g., `public Collection<Observer> observer`

### Listen Configuration

The `listen` property registers this service as a listener on other services:

```hocon
services {
  listener {
    implementation = com.example.EventListener
    listen {
      # Calls eventSource.addEventListener(this)
      event = <modules.this.event-source>
    }
  }
}
```

Looks for method: `add[ListenerName]Listener(Object listener)`

## API Reference

### Kernel

Main application container class: `oap.application.Kernel`

**Constructor:**
```java
Kernel(String name, List<URL> moduleConfigurations)
Kernel(List<URL> moduleConfigurations)
```

**Start methods:**
```java
void start()  // Load from classpath
void start(Path appConfigPath)
void start(Path appConfigPath, Path confd)
void start(ApplicationConfiguration config)
void start(Map<String, Object> properties)
```

**Service access:**
```java
<T> Optional<T> service(String moduleName, String serviceName)
<T> Optional<T> service(String reference)  // e.g., "module.service"
<T> List<T> services(String moduleName, String serviceName)
<T> Optional<T> serviceOfClass(Class<T> clazz)
<T> Optional<T> serviceOfClass(String moduleName, Class<T> clazz)
<T> List<T> ofClass(Class<T> clazz)
<T> List<T> ofClass(String moduleName, Class<T> clazz)
```

**Lifecycle:**
```java
void stop()
void close()  // Same as stop()
```

### Module

Module definition class: `oap.application.module.Module`

**Fields:**
```java
String name
LinkedHashSet<String> dependsOn
LinkedHashMap<String, Service> services
ModuleActivation activation
boolean enabled
```

### Service

Service definition class: `oap.application.module.Service`

**Fields:**
```java
String implementation
LinkedHashMap<String, Object> parameters
Supervision supervision
LinkedHashSet<String> dependsOn
LinkedHashMap<String, String> listen
LinkedHashMap<String, String> link
boolean enabled
boolean abstract
String defaultImplementation
```

### ApplicationConfiguration

Application configuration class: `oap.application.ApplicationConfiguration`

**Static methods:**
```java
static ApplicationConfiguration load()
static ApplicationConfiguration load(URL configURL, String confd)
static ApplicationConfiguration load(Path configPath, Path confd)
static ApplicationConfiguration load(Map<String, Object> properties)
```

## Configuration File Locations

**Module configuration:**
- `src/main/resources/META-INF/oap-module.conf`
- Packaged in JAR at `META-INF/oap-module.conf`
- Loaded from classpath

**Application configuration:**
- Default: `application.conf` in working directory
- Can be specified via path parameter to `kernel.start()`
- Supports `conf.d/` directory for configuration fragments
- Supports system property substitution

## Advanced Topics

### Dynamic Configuration

Use `DynamicConfig` for runtime-configurable values:

```java
import oap.application.DynamicConfig;

public class MyService {
    private final DynamicConfig<Integer> timeout;

    public MyService(int timeout) {
        this.timeout = new DynamicConfig<>(timeout);
    }

    public void setTimeout(int timeout) {
        this.timeout.set(timeout);
    }

    public int getTimeout() {
        return timeout.get();
    }
}
```

### Service Extensions

Modules and services support custom extensions via the `ext` map:

```hocon
services {
  my-service {
    implementation = com.example.MyService

    # Custom extension data
    my-extension {
      setting1 = value1
      setting2 = value2
    }
  }
}
```

Access extensions:
```java
List<ServiceExt<MyExtConfig>> services =
    kernel.servicesByExt("my-extension");

for (ServiceExt<MyExtConfig> serviceExt : services) {
    String serviceName = serviceExt.name;
    MyExtConfig config = serviceExt.ext;
    Object instance = serviceExt.serviceItem.instance;
}
```

### Kernel Commands

The framework supports custom kernel commands for configuration value processing. See `AbstractKernelCommand` and implementations:
- `ServiceKernelCommand` - Processes `<modules.*>` references
- `LocationKernelCommand` - Processes `<location.*>` references
- `KernelKernelCommand` - Processes `<kernel.*>` references

## Testing

Use `oap-application-test` for testing OAP applications. See that module's documentation for details.

## Best Practices

1. **Module naming**: Use lowercase-with-hyphens (e.g., `my-app`)
2. **Service naming**: Use lowercase-with-hyphens (e.g., `my-service`)
3. **Service references**: Prefer `<modules.this.*>` for same-module references
4. **Abstract services**: Define clear interfaces and provide sensible defaults
5. **Lifecycle methods**: Keep `preStart/start` fast, defer initialization to background threads
6. **Supervision**: Use `thread = true` for long-running services, `schedule` for periodic tasks
7. **Configuration**: Keep module configuration minimal, override in application.conf
8. **Dependencies**: Declare `dependsOn` explicitly for startup ordering

## See Also

- [oap-stdlib](../oap-stdlib/README.md) - Core utilities
- [oap-application-test](oap-application-test/README.md) - Testing support
- [Abstract Services Guide](README-abstract-service.md)
- [Application Configuration Guide](README-application-configuration.md)
