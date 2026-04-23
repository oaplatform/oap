# oap-http-prometheus

Prometheus metrics exporters for the OAP platform. Provides a scrape endpoint, JVM instrumentation, and application uptime tracking — all wired through `oap-module.oap` with zero boilerplate.

Depends on: `oap-http`

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-http-prometheus]
```

All three exporters start automatically. Override parameters in `application.conf` as needed.

## Services

### `PrometheusExporter`

Exposes a Prometheus scrape endpoint at `GET /metrics` on the `httpprivate` port (8081 by default). The endpoint returns metrics in the Prometheus text format 0.0.4.

```bash
curl http://localhost:8081/metrics
```

To serve metrics on the public port instead:

```hocon
services {
  oap-http-prometheus.oap-prometheus-metrics.parameters.port = ""
}
```

Internally uses Micrometer's `PrometheusMeterRegistry`, which is registered as the global `Metrics` registry so all `Metrics.counter(...)` / `Metrics.gauge(...)` calls are collected automatically.

### `PrometheusJvmExporter`

Registers Micrometer JVM binders that produce standard JVM metrics. All binders are enabled by default.

| Parameter | Default | Metrics produced |
|---|---|---|
| `enableClassLoaderMetrics` | `true` | `jvm_classes_*` |
| `enableJvmMemoryMetrics` | `true` | `jvm_memory_*` |
| `enableJvmGcMetrics` | `true` | `jvm_gc_*` |
| `enableLogbackMetrics` | `true` | `logback_events_total` |
| `enableJvmThreadMetrics` | `true` | `jvm_threads_*` |
| `enableJvmProcessorMetrics` | `true` | `system_cpu_*`, `process_cpu_*` |
| `enableFileDescriptorMetrics` | `true` | `process_files_*` |
| `enableJvmHeapPressureMetrics` | `true` | `jvm_memory_pool_allocated_bytes_total` |

Disable individual binders in `application.conf`:

```hocon
services {
  oap-http-prometheus.prometheus-jvm-exporter.parameters {
    enableLogbackMetrics = false
    enableFileDescriptorMetrics = false
  }
}
```

### `PrometheusApplicationInfoExporter`

Publishes a single `uptime_seconds` gauge that measures elapsed time since the service started.

```
# HELP uptime_seconds
# TYPE uptime_seconds gauge
uptime_seconds 3724.0
```

## Custom metrics

Use Micrometer's `Metrics` facade anywhere in the application — all metrics automatically flow to the Prometheus registry:

```java
Counter requests = Metrics.counter( "api_requests_total", "endpoint", "/items" );
requests.increment();

Metrics.gauge( "queue_depth", queue, Queue::size );
```
