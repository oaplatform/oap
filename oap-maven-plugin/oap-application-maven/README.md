# oap-application-maven

Maven plugin goal that generates OS service scripts for OAP applications. Run during `prepare-package`; the generated files land inside `destinationDirectory` ready to be packaged into an RPM or tarball.

## Goal: `oap:startup-scripts`

Reads bundled script templates, substitutes Maven project properties, and writes the resulting files at their conventional OS paths.

### Parameters

| Parameter | Default | Description |
|---|---|---|
| `destinationDirectory` | `target/oap/scripts` | Root directory for all generated files |
| `failIfUnsupportedOperationException` | `false` | If `true`, fail the build when POSIX permissions cannot be set (e.g. on Windows); if `false`, log the error and continue |

### Project properties

| Property | Default | Description |
|---|---|---|
| `oap.service.name` | `oap-service` | Used as the filename stem for systemd/sysvinit scripts |
| `oap.service.home` | `/opt/oap-service` | Installation prefix used inside `functions.sh` and `oap.sh` |

Set these in the module's `<properties>` block or pass them with `-D` on the Maven command line.

### Generated files

| Template | Output path (relative to `destinationDirectory`) | Permissions |
|---|---|---|
| `functions.sh` | `<oap.service.home>/bin/functions.sh` | — |
| `oap.sh` | `<oap.service.home>/bin/<oap.service.name>.sh` | `rwxr-xr-x` |
| `service.systemd` | `usr/lib/systemd/system/<oap.service.name>.service` | — |
| `service.sysvinit` | `etc/init.d/<oap.service.name>` | `rwxr-xr-x` |

### `pom.xml` wiring

```xml
<plugin>
    <groupId>oap</groupId>
    <artifactId>oap-application-maven</artifactId>
    <version>${oap.version}</version>
    <executions>
        <execution>
            <goals><goal>startup-scripts</goal></goals>
        </execution>
    </executions>
</plugin>
```

```xml
<properties>
    <oap.service.name>my-service</oap.service.name>
    <oap.service.home>/opt/my-service</oap.service.home>
</properties>
```
