# oap-maven-plugin

Build-time code generation and packaging utilities for OAP projects. All goals share the prefix `oap`.

## Goals

| Goal | Module | Phase | Description |
|---|---|---|---|
| [`oap:generate`](oap-dictionary-maven/README.md) | `oap-dictionary-maven` | `generate-sources` | Generate Java enums from dictionary JSON/HOCON files |
| [`oap:startup-scripts`](oap-application-maven/README.md) | `oap-application-maven` | `prepare-package` | Generate OS service scripts (systemd, sysvinit, shell) |
| [`oap:copy`](oap-maven/README.md) | `oap-maven` | `prepare-package` | Copy file sets into a directory with optional property filtering |
