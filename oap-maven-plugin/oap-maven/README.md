# oap-maven

Maven plugin goal that copies file sets into an output directory during `prepare-package`, with optional Maven property substitution in file content.

## Goal: `oap:copy`

### Parameters

| Parameter | Required | Description |
|---|---|---|
| `outputDirectory` | yes | Destination directory for all copied files |
| `fileSets` | yes | List of `FileSet` elements describing what to copy |

### `FileSet` fields

| Field | Description |
|---|---|
| `directory` | Source directory (resolved to absolute path) |
| `includes` | List of Ant-style glob patterns to include (empty = all files) |
| `excludes` | List of Ant-style glob patterns to exclude |
| `filtering` | If `true`, replace `${property}` tokens in file content using Maven project properties |

Directories that do not exist are silently skipped.

### `pom.xml` wiring

```xml
<plugin>
    <groupId>oap</groupId>
    <artifactId>oap-maven</artifactId>
    <version>${oap.version}</version>
    <executions>
        <execution>
            <goals><goal>copy</goal></goals>
            <configuration>
                <outputDirectory>${project.build.directory}/package</outputDirectory>
                <fileSets>
                    <fileSet>
                        <directory>src/main/conf</directory>
                        <includes>
                            <include>**/*.conf</include>
                        </includes>
                        <filtering>true</filtering>
                    </fileSet>
                    <fileSet>
                        <directory>src/main/scripts</directory>
                        <excludes>
                            <exclude>**/*.bak</exclude>
                        </excludes>
                        <filtering>false</filtering>
                    </fileSet>
                </fileSets>
            </configuration>
        </execution>
    </executions>
</plugin>
```
