# oap-ws-openapi-maven-plugin

Maven plugin that generates an OpenAPI 3.x specification (`swagger.json` or YAML) from OAP web service annotations at build time. The output is written to `target/classes` so it is bundled into the application JAR.

## Build-time generation

Add the plugin to `pom.xml`:

```xml
<plugin>
  <groupId>oap</groupId>
  <artifactId>oap-ws-openapi-maven-plugin</artifactId>
  <version>${project.parent.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>openapi</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Then run:

```bash
mvn clean install
```

The generated file appears at `target/classes/swagger.json` (default).

## Configuration

All settings go inside `<configuration>`:

```xml
<configuration>
  <outputPath>swagger</outputPath>          <!-- output directory inside target/classes -->
  <outputType>JSON_OPENAPI</outputType>     <!-- JSON_OPENAPI or YAML_OPENAPI -->
  <excludeModules>                          <!-- comma-separated module names to skip -->
    oap-ws,oap-ws-admin-ws,oap-ws-openapi-ws
  </excludeModules>
</configuration>
```

| Parameter | Default | Description |
|---|---|---|
| `outputPath` | `""` (target/classes root) | Subdirectory under `target/classes` for the output file |
| `outputType` | `JSON_OPENAPI` | Output format: `JSON_OPENAPI` or `YAML_OPENAPI` |
| `excludeModules` | — | Comma-separated list of OAP module names whose endpoints are omitted from the spec |

## Manual invocation

Run the goal without a full build:

```bash
mvn oap:oap-ws-openapi-maven-plugin:<version>:openapi
```
