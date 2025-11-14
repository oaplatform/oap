# OAP Configuration Reference

This guide provides comprehensive documentation for configuring OAP applications, modules, and services using HOCON (Human-Optimized Config Object Notation).

## Table of Contents

1. [Configuration Basics](#configuration-basics)
2. [Module Configuration](#module-configuration)
3. [Application Configuration](#application-configuration)
4. [Service Configuration](#service-configuration)
5. [Dependency Injection](#dependency-injection)
6. [Service Lifecycle (Supervision)](#service-lifecycle-supervision)
7. [Module Extensions](#module-extensions)
8. [Configuration Overrides](#configuration-overrides)
9. [Environment Variables](#environment-variables)
10. [Advanced Patterns](#advanced-patterns)

---

## Configuration Basics

### HOCON Syntax

OAP uses HOCON, a JSON superset that provides:

- **Comments**: Use `//` for single-line or `/* */` for multi-line comments
- **Unquoted keys**: Keys don't require quotes (e.g., `name = value`)
- **Substitutions**: Reference other values using `${path.to.value}`
- **Durations**: Use time units like `5s`, `10m`, `24h`
- **Lists**: Use `[]` for arrays
- **Objects**: Use `{}` for nested structures

**Example:**
```hocon
// Simple HOCON example
services {
  http-server {
    implementation = com.example.HttpServer
    parameters {
      port = 8080
      timeout = 30s
      hosts = ["localhost", "0.0.0.0"]
    }
  }
}
```

### File Locations

**Module Configuration** (`oap-module.conf`):
- Location: `src/main/resources/META-INF/oap-module.conf`
- Purpose: Defines services, dependencies, and module metadata
- Loaded automatically from classpath

**Application Configuration** (`application.conf`):
- Location: Application root directory or specified path
- Purpose: Overrides module defaults, customizes service parameters
- Loaded at application startup

**Module Extensions** (`oap-module-ext.conf`):
- Location: `src/main/resources/META-INF/oap-module-ext.conf`
- Purpose: Extends or modifies existing services in other modules
- Allows customization without modifying original module

**Configuration Directory** (`conf.d/`):
- Location: Application root or specified directory
- Purpose: Multiple configuration files (`.conf`, `.yaml`) merged together
- Useful for environment-specific overrides

---

## Module Configuration

Module configuration files define reusable services that can be shared across applications.

### Module Structure

**File:** `src/main/resources/META-INF/oap-module.conf`

```hocon
name = module-name
dependsOn = [other-module-1, other-module-2]
enabled = true

services {
  service-name {
    implementation = fully.qualified.ClassName
    parameters {
      param1 = value1
      param2 = value2
    }
    supervision {
      supervise = true
    }
    dependsOn = [other-service]
    enabled = true
  }
}

configurations = [
  {
    loader = oap.json.TypeIdFactory
    config = {
      type-alias = fully.qualified.ClassName
    }
  }
]
```

### Module Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Unique module identifier |
| `dependsOn` | List[String] | No | Modules this module depends on |
| `enabled` | Boolean | No | Enable/disable entire module (default: true) |
| `services` | Object | Yes | Service definitions (see [Service Configuration](#service-configuration)) |
| `configurations` | List | No | Type registrations for JSON serialization |

### Example: HTTP Module

```hocon
name = oap-http
services {
  nio-compression-handler {
    implementation = oap.http.server.nio.handlers.CompressionNioHandler
  }

  blocking-read-timeout-handler {
    implementation = oap.http.server.nio.handlers.BlockingReadTimeoutHandler
    parameters {
      readTimeout = 60s
    }
  }

  oap-http-server {
    implementation = oap.http.server.nio.NioHttpServer
    parameters {
      defaultPort {
        httpPort = 8080
//        httpsPort = 8443
//        keyStore = /path/to/keystore
//        password = secret
      }
      additionalHttpPorts {
        httpprivate = 8081
      }
      backlog = -1
      idleTimeout = -1
      tcpNodelay = true
      ioThreads = -1
      workerThreads = -1

      handlers = [
        <modules.this.nio-compression-handler>
        <modules.this.blocking-read-timeout-handler>
      ]
    }
    supervision.supervise = true
  }
}
```

---

## Application Configuration

Application configuration customizes modules and services for specific deployments.

### Application Structure

**File:** `application.conf`

```hocon
boot {
  main = [module1, module2]
  allowActiveByDefault = false
}

shutdown {
  serviceTimeout = 5s
  serviceAsyncShutdownAfterTimeout = false
}

services {
  module-name.service-name.parameters.param1 = override-value
  module-name.service-name.enabled = false
}
```

### Boot Configuration

Controls which modules and services are started:

```hocon
boot {
  // Main modules to start (others can be auto-loaded via dependencies)
  main = [oap-http, oap-ws, my-app]

  // If true, modules with activation.activeByDefault = true are started
  allowActiveByDefault = false
}
```

### Shutdown Configuration

Controls graceful shutdown behavior:

```hocon
shutdown {
  // Timeout for each service to stop gracefully (default: 5s)
  serviceTimeout = 10s
  serviceTimeout = ${?SHUTDOWN_SERVICE_TIMEOUT}

  // Force shutdown after timeout (default: false)
  serviceAsyncShutdownAfterTimeout = true
}
```

### Service Overrides

Override parameters from module configuration:

```hocon
services {
  // Override HTTP server port
  oap-http.oap-http-server.parameters.defaultPort.httpPort = 9090

  // Override session expiration
  oap-ws.session-manager.parameters.expirationTime = 12h

  // Disable a service
  oap-http.oap-http-zero-png-handler.enabled = false

  // Add custom parameter
  my-module.my-service.parameters.customConfig = "production"
}
```

---

## Service Configuration

Services are the core building blocks of OAP applications.

### Service Definition

```hocon
service-name {
  implementation = fully.qualified.ClassName
  default = <modules.other-module.default-service>
  abstract = false
  enabled = true

  parameters {
    param1 = value1
    param2 = <modules.other-module.dependency>
  }

  supervision {
    supervise = true
    thread = false
    schedule = false
    preStartWith = ["preStart"]
    startWith = ["start"]
    preStopWith = ["preStop"]
    stopWith = ["stop", "close"]
    delay = 0
    cron = "0 */5 * * * ?"
  }

  dependsOn = [other-service]

  listen {
    event-type = listener-method-name
  }

  link {
    field-name = interface-name
  }
}
```

### Service Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `implementation` | String | Yes* | Fully qualified class name (*optional if abstract) |
| `default` | Reference | No | Default implementation for abstract service |
| `abstract` | Boolean | No | Mark as abstract service (requires implementation in application.conf) |
| `enabled` | Boolean | No | Enable/disable service (default: true) |
| `parameters` | Object | No | Constructor/setter parameters |
| `supervision` | Object | No | Lifecycle management (see [Supervision](#service-lifecycle-supervision)) |
| `dependsOn` | List[String] | No | Services that must start before this one |
| `listen` | Object | No | Event listeners to register |
| `link` | Object | No | Link all implementations of an interface |

---

## Dependency Injection

OAP uses constructor and field injection for service dependencies.

### Service References

Reference other services using angle bracket syntax:

```hocon
services {
  database {
    implementation = com.example.Database
    parameters {
      url = "jdbc:postgresql://localhost/mydb"
    }
  }

  user-service {
    implementation = com.example.UserService
    parameters {
      // Reference service in same module
      database = <modules.this.database>

      // Reference service in other module
      httpServer = <modules.oap-http.oap-http-server>

      // Reference kernel itself
      kernel = <kernel.self>
    }
  }
}
```

### Reference Types

| Reference | Description | Example |
|-----------|-------------|---------|
| `<modules.this.service-name>` | Service in current module | `<modules.this.database>` |
| `<modules.module-name.service-name>` | Service in another module | `<modules.oap-http.oap-http-server>` |
| `<kernel.self>` | The Kernel instance itself | `<kernel.self>` |

### Complex Parameters

```hocon
services {
  email-service {
    implementation = com.example.EmailService
    parameters {
      // Simple values
      fromAddress = "noreply@example.com"
      retryCount = 3
      timeout = 30s

      // Nested objects
      smtp {
        host = "smtp.gmail.com"
        port = 587
        useTLS = true
      }

      // Lists
      allowedDomains = ["example.com", "example.org"]

      // Service references in lists
      handlers = [
        <modules.this.handler1>
        <modules.this.handler2>
      ]

      // Environment variable substitution
      apiKey = ${EMAIL_API_KEY}
      apiKey = ${?EMAIL_API_KEY}  // Optional (won't fail if missing)
    }
  }
}
```

---

## Service Lifecycle (Supervision)

Supervision controls how services are started, stopped, and managed.

### Supervision Configuration

```hocon
supervision {
  // Enable supervision (service will be started/stopped with kernel)
  supervise = true

  // Run service in separate thread
  thread = false

  // Schedule service to run periodically
  schedule = false

  // Method names to call during lifecycle
  preStartWith = ["preStart", "init"]
  startWith = ["start", "run"]
  preStopWith = ["preStop"]
  stopWith = ["stop", "close", "shutdown"]

  // For scheduled services: delay before first execution (ms)
  delay = 5000

  // For scheduled services: Quartz cron expression
  cron = "0 */5 * * * ?"  // Every 5 minutes
}
```

### Lifecycle Methods

Services can implement lifecycle methods that are called automatically:

```java
public class MyService {
    public void preStart() {
        // Called before service starts
        // Initialize resources, validate configuration
    }

    public void start() {
        // Called to start the service
        // Open connections, start listening
    }

    public void preStop() {
        // Called before service stops
        // Prepare for shutdown, finish in-flight requests
    }

    public void stop() {
        // Called to stop the service
        // Close connections, release resources
    }
}
```

### Lifecycle Examples

**Always-Running Service:**
```hocon
http-server {
  implementation = com.example.HttpServer
  supervision {
    supervise = true
    startWith = ["start"]
    stopWith = ["stop"]
  }
}
```

**Background Thread:**
```hocon
cache-cleanup {
  implementation = com.example.CacheCleanup
  supervision {
    supervise = true
    thread = true
    startWith = ["run"]  // Runs continuously in thread
  }
}
```

**Scheduled Task:**
```hocon
report-generator {
  implementation = com.example.ReportGenerator
  supervision {
    supervise = true
    schedule = true
    delay = 60000  // Wait 1 minute before first run
    cron = "0 0 2 * * ?"  // Run daily at 2 AM
    startWith = ["generateReports"]
  }
}
```

---

## Module Extensions

Module extensions allow you to modify services from other modules without changing the original module.

### Extension File Structure

**File:** `src/main/resources/META-INF/oap-module-ext.conf`

```hocon
// Extend or override service implementation
services.module-name.service-name.implementation = com.example.CustomImplementation

// Add parameters
services.module-name.service-name.parameters.newParam = value

// Modify existing parameters
services.module-name.service-name.parameters.existingParam = newValue
```

### Extension Examples

**Override Remote Service Implementation:**
```hocon
// File: oap-module-ext.conf in your module
services.remote.implementation = com.mycompany.CustomRemoteLocation
```

**Add Health Check Provider:**
```hocon
// Extend health handler with custom provider
services.oap-http.oap-http-health-handler.parameters.providers = [
  <modules.this.custom-health-provider>
]
```

**Customize Session Manager:**
```hocon
// Override session expiration
services.oap-ws.session-manager.parameters.expirationTime = 48h
services.oap-ws.session-manager.parameters.cookiePath = "/api"
```

---

## Configuration Overrides

Configuration can be overridden in multiple ways with the following precedence (highest to lowest):

1. **Environment variables** (prefixed with `CONFIG.`)
2. **Application configuration** (`application.conf`)
3. **Configuration directory files** (`conf.d/*.conf`, `conf.d/*.yaml`)
4. **Module extensions** (`oap-module-ext.conf`)
5. **Module defaults** (`oap-module.conf`)

### Override Example

**Module default** (in `oap-module.conf`):
```hocon
name = oap-http
services {
  oap-http-server {
    implementation = oap.http.server.nio.NioHttpServer
    parameters {
      defaultPort.httpPort = 8080
      workerThreads = -1
    }
  }
}
```

**Application override** (in `application.conf`):
```hocon
services {
  oap-http.oap-http-server.parameters {
    defaultPort.httpPort = 9090
    workerThreads = 100
  }
}
```

**Environment variable override:**
```bash
export CONFIG.services.oap-http.oap-http-server.parameters.defaultPort.httpPort=9999
```

Final effective configuration: `httpPort = 9999`, `workerThreads = 100`

---

## Environment Variables

### Standard Environment Variables

**Direct substitution:**
```hocon
services {
  database {
    parameters {
      url = ${DATABASE_URL}
      password = ${DATABASE_PASSWORD}
    }
  }
}
```

**Optional substitution** (doesn't fail if variable missing):
```hocon
services {
  database {
    parameters {
      url = "jdbc:postgresql://localhost/mydb"
      url = ${?DATABASE_URL}  // Override if set
    }
  }
}
```

### CONFIG Prefix

Any environment variable prefixed with `CONFIG.` automatically overrides configuration:

```bash
# Set HTTP port via environment
export CONFIG.services.oap-http.oap-http-server.parameters.defaultPort.httpPort=8888

# Disable a service
export CONFIG.services.oap-http.oap-http-zero-png-handler.enabled=false

# Override module dependency
export CONFIG.boot.main="[my-module]"
```

---

## Advanced Patterns

### Abstract Services with Defaults

Define pluggable services with default implementations:

**Module definition:**
```hocon
name = oap-mail
services {
  mail-queue-persistence {
    abstract = true
    implementation = oap.mail.MailQueuePersistence
    default = <modules.this.mail-queue-persistence-file>
  }

  mail-queue-persistence-file {
    implementation = oap.mail.FileMailQueuePersistence
    parameters {
      path = /var/lib/mail-queue
    }
  }

  mail-queue-persistence-mongodb {
    enabled = false  // Available but not used by default
    implementation = oap.mail.MongoMailQueuePersistence
    parameters {
      database = <modules.oap-storage.mongo-database>
    }
  }

  mail-sender {
    implementation = oap.mail.MailSender
    parameters {
      // Will use default (file-based) unless overridden
      persistence = <modules.this.mail-queue-persistence>
    }
  }
}
```

**Application override to use MongoDB:**
```hocon
services {
  // Enable MongoDB persistence
  oap-mail.mail-queue-persistence-mongodb.enabled = true

  // Override abstract service to use MongoDB
  oap-mail.mail-queue-persistence.implementation = <modules.oap-mail.mail-queue-persistence-mongodb>
}
```

### Dynamic Service Lists

Build service lists dynamically:

```hocon
services {
  request-logger {
    implementation = com.example.RequestLogger
  }

  auth-interceptor {
    implementation = com.example.AuthInterceptor
  }

  rate-limiter {
    implementation = com.example.RateLimiter
  }

  http-server {
    implementation = com.example.HttpServer
    parameters {
      // List of interceptors - add/remove as needed
      interceptors = [
        <modules.this.request-logger>
        <modules.this.auth-interceptor>
        <modules.this.rate-limiter>
      ]
    }
  }
}
```

### Configuration Loading from Files

Load configuration from external files:

```hocon
services {
  json-service {
    implementation = com.example.JsonService
    parameters {
      // Load from classpath
      config = <classpath:config/service-config.json>

      // Load from file path
      schema = <path:/etc/myapp/schema.json>
    }
  }
}
```

### Multi-Environment Configuration

**Base configuration** (`application.conf`):
```hocon
include "application-${ENV}.conf"

services {
  database {
    parameters {
      pool.maxSize = 20
      pool.minIdle = 5
    }
  }
}
```

**Development** (`application-dev.conf`):
```hocon
services {
  database.parameters {
    url = "jdbc:postgresql://localhost/myapp_dev"
    pool.maxSize = 5
  }
}
```

**Production** (`application-prod.conf`):
```hocon
services {
  database.parameters {
    url = ${DATABASE_URL}
    pool.maxSize = 100
    pool.minIdle = 20
  }
}
```

Run with: `ENV=prod java -jar app.jar`

### Type Registration

Register type aliases for JSON serialization:

```hocon
configurations = [
  {
    loader = oap.json.TypeIdFactory
    config = {
      // Map type ID to class
      my-type = com.example.MyType
      pair = oap.util.Pair

      // Useful for polymorphic deserialization
      user-event = com.example.events.UserEvent
      order-event = com.example.events.OrderEvent
    }
  }
]
```

### Conditional Configuration

```hocon
services {
  feature-x {
    // Enable feature based on environment variable
    enabled = false
    enabled = ${?FEATURE_X_ENABLED}

    implementation = com.example.FeatureX
  }

  cache {
    implementation = com.example.CacheService
    parameters {
      // Use Redis if configured, otherwise in-memory
      backend = "memory"
      backend = ${?CACHE_BACKEND}

      redisUrl = "redis://localhost:6379"
      redisUrl = ${?REDIS_URL}
    }
  }
}
```

---

## Configuration Best Practices

### 1. **Use Modules for Reusability**
Define services in modules that can be reused across applications:
```hocon
// oap-module.conf
name = my-company-auth
services {
  auth-service {
    implementation = com.mycompany.AuthService
    parameters {
      // Sensible defaults
      sessionTimeout = 24h
      bcryptRounds = 12
    }
  }
}
```

### 2. **Override in Applications**
Keep application configuration minimal:
```hocon
// application.conf
boot.main = [my-company-auth, my-app]

services {
  // Only override what's different from defaults
  my-company-auth.auth-service.parameters.sessionTimeout = 12h
}
```

### 3. **Use Environment Variables for Secrets**
Never commit secrets to configuration files:
```hocon
services {
  database.parameters {
    username = ${DB_USERNAME}
    password = ${DB_PASSWORD}
    url = ${DB_URL}
  }
}
```

### 4. **Document Custom Parameters**
Add comments to explain non-obvious configuration:
```hocon
services {
  cache {
    parameters {
      // TTL in seconds. Must be > 0. Recommended: 300-3600 for prod
      ttl = 600

      // Max entries before eviction starts. Memory usage ~= maxEntries * avgEntrySize
      maxEntries = 10000
    }
  }
}
```

### 5. **Use Supervision Wisely**
Only supervise services that need lifecycle management:
```hocon
// Database pool needs lifecycle
database-pool {
  supervision.supervise = true
}

// Stateless utility doesn't need supervision
string-utils {
  supervision.supervise = false
}
```

### 6. **Organize Large Configurations**
Split configuration into multiple files in `conf.d/`:
```
conf.d/
  ├── 01-database.conf
  ├── 02-cache.conf
  ├── 03-http.conf
  └── 99-overrides.conf
```

Files are loaded in alphabetical order, use prefixes to control precedence.

---

## Troubleshooting

### Common Configuration Errors

**Service not found:**
```
Error: Service 'my-module.my-service' not found
```
- Check service name spelling
- Ensure module is in `boot.main` or loaded via dependency
- Verify module's `enabled = true`

**Circular dependency:**
```
Error: Circular dependency detected: service-a -> service-b -> service-a
```
- Refactor services to break the cycle
- Consider using lazy initialization or event listeners

**Missing required parameter:**
```
Error: Required parameter 'database' not provided for service 'user-service'
```
- Add parameter to service configuration
- Check parameter name matches constructor/setter

**Type mismatch:**
```
Error: Cannot convert value '8080' to type java.lang.Boolean
```
- Check parameter type in Java class
- Ensure configuration value matches expected type

**Reference resolution failed:**
```
Error: Cannot resolve reference <modules.other-module.service>
```
- Ensure referenced module is loaded
- Check service name spelling
- Verify service is enabled

### Debug Configuration

Enable configuration logging:
```bash
java -Dlogger.oap.application=TRACE -jar app.jar
```

Print effective configuration:
```java
// In your application
kernel.serviceOfClass(Kernel.class).ifPresent(k -> {
    System.out.println("Loaded modules: " + k.modules.keySet());
    System.out.println("Loaded services: " + k.services.keySet());
});
```

---

## See Also

- [Getting Started Guide](getting-started.md)
- [Developer Guide](developer-guide.md)
- [Module READMEs](../README.md#modules-overview)
- [HOCON Documentation](https://github.com/lightbend/config/blob/main/HOCON.md)
