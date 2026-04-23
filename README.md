# <img src="oap_logo.png" width="165" height="90"> Open Application Platform

A light-weight application framework to build high performant and distributed java applications.

## Modules

| Module | Description                                                                                                 |
|---|-------------------------------------------------------------------------------------------------------------|
| [oap-application](oap-application/README.md) | IoC/DI Kernel — discovers services from HOCON descriptors, wires dependencies, manages start/stop lifecycle |
| [oap-stdlib](oap-stdlib/README.md) | Core utilities — `Binder` (JSON/HOCON/YAML/XML), `Files`, `IoStreams`, `Cuid`, `Dates`, `Stream`, `Result`  |
| [oap-stdlib-test](oap-stdlib-test/README.md) | Test infrastructure — fixture lifecycle, `TestDirectoryFixture`, `Asserts`, `Ports`, `Benchmark`            |
| [oap-http](oap-http/README.md) | Undertow-based HTTP server with named ports, PNIO high-performance pipeline, and HTTP client                |
| [oap-ws](oap-ws/README.md) | Annotation-driven web services (`@WsMethod`, `@WsParam`) with session and interceptor support               |
| [oap-jpath](oap-jpath/README.md) | JPath expression language for navigating object graphs: `${var.field.method().array[n]}`                    |
| [oap-formats](oap-formats/README.md) | Template engine, TSV/CSV parsing, JSON schema validation, and log streaming                                 |
| [oap-statsdb](oap-statsdb/README.md) | Distributed in-memory statistics tree with hierarchical rollup and MongoDB persistence                      |
| [oap-message](oap-message/README.md) | Reliable binary message delivery with disk spill, retry, and MD5-based deduplication                        |
| [oap-storage](oap-storage/README.md) | In-memory object store (`MemoryStorage`) with MongoDB sync and cloud object storage                         |
| [oap-highload](oap-highload/README.md) | CPU affinity utility for pinning threads to specific CPU cores                                              |
| [oap-mail](oap-mail/README.md) | Email sending via SMTP and SendGrid with a persistent delivery queue                                        |
| [oap-maven-plugin](oap-maven-plugin/README.md) | Build-time code generation: startup scripts and dictionary enum source files                                |
