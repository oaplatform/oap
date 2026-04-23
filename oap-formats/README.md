# oap-formats

Format processing modules for the OAP platform: template engine, TSV/CSV, JSON schema validation, and log streaming.

## Sub-modules

| Module | Description |
|---|---|
| [oap-template](oap-template/README.md) | Compile-time template engine — parses once, compiles to Java, renders at near-native speed |
| [oap-template-test](oap-template-test/README.md) | `TemplateEngineFixture` — TestNG fixture for template engine tests |
| [oap-json](oap-json/README.md) | JSON schema validation (HOCON format) and structural diff |
| [oap-tsv](oap-tsv/README.md) | TSV/CSV parsing, streaming, and printing |
| [oap-logstream](oap-logstream/README.md) | High-throughput transactional log streaming to time-bucketed gzip files |
