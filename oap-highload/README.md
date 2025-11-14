# OAP Highload

Performance optimization utilities for high-load applications, including CPU affinity management for thread pinning and low-latency computing.

## Overview

OAP Highload provides specialized utilities designed for ultra-low-latency, high-throughput applications. It features:
- CPU affinity management for thread pinning
- Support for CPU range specifications
- OpenHFT affinity integration
- Thread-to-CPU binding utilities
- Performance tuning helpers
- Zero-allocation operations

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-highload</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **CPU Affinity** - Pin threads to specific CPU cores
- **Flexible CPU Selection** - Ranges, lists, and wildcard support
- **Runtime Adaptation** - Automatic detection of available cores
- **Zero-Allocation** - Optimized for minimal garbage collection
- **Thread Management** - Helper utilities for thread configuration
- **Performance Monitoring** - Integration with performance metrics

## Key Classes

- `Affinity` - CPU affinity specification and management
- `AffinityThread` - Thread bound to specific CPU core

## Quick Example

```java
import oap.highload.Affinity;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

// Create affinity configuration
Affinity affinity = new Affinity("0-3");  // Cores 0 to 3
int[] cpus = affinity.getCpus();  // [0, 1, 2, 3]

// Use for thread pool
ExecutorService executor = Executors.newFixedThreadPool(4);
for (int i = 0; i < 4; i++) {
    executor.submit(() -> {
        // Thread will be bound to specified CPU core
        performHighLoadWork();
    });
}

// Pin current thread to specific core
Affinity singleCore = new Affinity("7");
// Use API to bind thread...
```

## CPU Specification Syntax

The Affinity class supports flexible CPU specification:

### Single Core
```java
new Affinity("3")     // CPU 3 only
```

### Multiple Cores
```java
new Affinity("1, 3, 5")     // CPUs 1, 3, 5
```

### Core Range
```java
new Affinity("1-4")   // CPUs 1, 2, 3, 4 inclusive
```

### From N to End
```java
new Affinity("4+")    // CPUs 4 to last available core
```

### Mixed Syntax
```java
new Affinity("1-3, 8, 10+")  // CPUs 1, 2, 3, 8, and 10 onwards
```

### Disable Affinity
```java
new Affinity("*")     // No CPU binding (all cores available)
```

## Usage Examples

### Basic Thread Pinning
```java
// Bind current thread to CPU 0
Affinity affinity = new Affinity("0");
if (affinity.isEnabled()) {
    // Perform affinity binding
    // (actual binding done via JNI in production)
}
```

### CPU Selection Strategy
```java
// Reserve cores 0-3 for critical work
Affinity criticalPath = new Affinity("0-3");

// Use cores 4-7 for batch processing
Affinity batchPath = new Affinity("4-7");

// Leave cores 8+ for OS and background tasks
Affinity backgroundPath = new Affinity("8+");
```

### Executor Service with Affinity
```java
int[] cpus = new Affinity("0-3").getCpus();
ExecutorService executor = Executors.newFixedThreadPool(cpus.length);

// Tasks submitted will be distributed across pinned cores
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        // High-load work with CPU affinity
        process();
    });
}
```

## Configuration

Enable OAP Highload in `oap-module.yaml`:

```yaml
dependsOn:
  - oap-highload
```

Configure in `application.conf`:

```hocon
oap-highload {
    # CPU cores to use for critical operations
    criticalCores = "0-3"
    
    # Enable NUMA awareness if applicable
    numaAware = false
    
    # Enable CPU affinity enforcement
    affinityEnabled = true
}
```

## Performance Characteristics

### Benefits
- **Reduced Context Switching** - Thread stays on same CPU core
- **Better Cache Locality** - L1/L2/L3 cache affinity
- **Lower Latency** - Fewer NUMA penalties
- **Predictable Performance** - Reduced latency variance

### Trade-offs
- **Reduced Flexibility** - Fewer cores available per thread
- **Imbalanced Load** - May need manual balancing
- **System Coupling** - Depends on CPU topology

## Best Practices

1. **Reserve Cores** - Keep some cores free for OS and monitoring
2. **Test Topology** - Understand your system's CPU/NUMA layout
3. **Monitor Performance** - Use affinity only where it helps
4. **Graceful Degradation** - Work with or without affinity
5. **Documentation** - Document CPU allocation in your setup

## Example: Low-Latency Trading System

```java
public class LowLatencyTradingEngine {
    private final Affinity marketDataCores = new Affinity("0-1");
    private final Affinity orderProcessingCores = new Affinity("2-3");
    private final Affinity settlementCores = new Affinity("4+");
    
    public void start() {
        // Market data consumer on dedicated cores
        ExecutorService marketData = createAffinityExecutor(marketDataCores);
        marketData.submit(this::consumeMarketData);
        
        // Order processing on separate cores
        ExecutorService orderProc = createAffinityExecutor(orderProcessingCores);
        orderProc.submit(this::processOrders);
        
        // Settlement on remaining cores
        ExecutorService settlement = createAffinityExecutor(settlementCores);
        settlement.submit(this::settleTransactions);
    }
    
    private ExecutorService createAffinityExecutor(Affinity affinity) {
        return Executors.newFixedThreadPool(affinity.getCpus().length);
    }
}
```

## Dependencies

- **OpenHFT Affinity Library** - Native CPU affinity binding
  - Provides JNI bindings for OS-level CPU affinity
  - Supports Linux, Windows, and macOS
  - Gracefully degrades on unsupported systems

## Platform Support

- **Linux** - Full support with native affinity
- **Windows** - Limited support (no actual core binding)
- **macOS** - Limited support (no actual core binding)
- **Other Unix** - Varies by OS and kernel features

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-io` - I/O performance utilities
- `oap-storage` - Storage performance optimization

## Testing

See `oap-highload/src/test/java/oap/highload/AffinityTest.java` for examples and test cases.

## Troubleshooting

### Affinity Not Working
- Check if OpenHFT affinity library is available
- Verify CPU IDs are valid for your system
- Check system permissions for CPU binding

### Performance Not Improved
- Verify threads are actually pinned (use `taskset` on Linux)
- Check for other resource contention
- Ensure enough cores available for the workload
- Profile to identify actual bottleneck
