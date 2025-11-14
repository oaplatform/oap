# OAP Maven Plugin

Collection of Maven plugins for building and processing OAP applications, providing build-time code generation, application packaging, and configuration management.

## Overview

OAP Maven Plugin provides a suite of Maven plugins to streamline OAP application development and deployment:
- Application packaging and deployment
- Dictionary compilation and validation
- Code generation from configuration
- Resource management
- Build-time optimizations
- Maven integration for OAP framework

The module consists of:
- **oap-maven** - Core Maven plugin base
- **oap-application-maven** - Application packaging and deployment
- **oap-dictionary-maven** - Dictionary compilation and management

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-maven-plugin</artifactId>
    <version>${oap.version}</version>
    <type>pom</type>
</dependency>
```

### Usage in POM

```xml
<plugins>
    <plugin>
        <groupId>oap</groupId>
        <artifactId>oap-maven-plugin</artifactId>
        <version>${oap.version}</version>
        <executions>
            <execution>
                <phase>process-resources</phase>
                <goals>
                    <goal>process-application</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
```

## Key Features

- **Application Packaging** - Package OAP applications with all dependencies
- **Dictionary Processing** - Compile and validate dictionary definitions
- **Code Generation** - Generate Java code from configuration
- **Resource Optimization** - Optimize resource bundling
- **Build-time Validation** - Validate configuration at build time
- **Deployment Preparation** - Prepare applications for deployment
- **Incremental Builds** - Support for incremental compilation

## Key Plugins

### oap-application-maven
Handles application packaging and preparation for deployment.

**Goals:**
- `process-application` - Process application configuration
- `package-application` - Package application with dependencies
- `validate-application` - Validate application configuration

**Configuration:**
```xml
<configuration>
    <applicationName>myapp</applicationName>
    <outputDirectory>${project.build.directory}</outputDirectory>
    <includeResources>true</includeResources>
</configuration>
```

### oap-dictionary-maven
Compiles and validates dictionary definitions for data models.

**Goals:**
- `compile-dictionary` - Compile dictionary definitions
- `validate-dictionary` - Validate dictionary structure
- `generate-dictionary-docs` - Generate dictionary documentation

**Configuration:**
```xml
<configuration>
    <dictionarySource>src/main/dictionary</dictionarySource>
    <outputDirectory>${project.build.directory}/generated-sources</outputDirectory>
    <generateJava>true</generateJava>
</configuration>
```

### oap-maven
Base Maven plugin infrastructure and utilities.

**Features:**
- Common Mojo base classes
- Logging utilities
- File processing utilities
- Parameter validation

## Quick Example

### Minimal Application POM

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-oap-app</artifactId>
    <version>1.0.0</version>
    
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>oap</groupId>
            <artifactId>oap-application</artifactId>
            <version>${oap.version}</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>oap</groupId>
                <artifactId>oap-maven-plugin</artifactId>
                <version>${oap.version}</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>process-application</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Dictionary Compilation Example

```xml
<plugin>
    <groupId>oap</groupId>
    <artifactId>oap-maven-plugin</artifactId>
    <version>${oap.version}</version>
    <executions>
        <execution>
            <id>compile-dictionary</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>compile-dictionary</goal>
            </goals>
            <configuration>
                <dictionarySource>src/main/dictionary</dictionarySource>
                <outputDirectory>
                    ${project.build.directory}/generated-sources/dictionary
                </outputDirectory>
                <packageName>com.example.generated</packageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Configuration Options

### Application Plugin Configuration

```hocon
oap-application-maven {
    # Application name
    applicationName = "myapp"
    
    # Output directory for packaged application
    outputDirectory = "target"
    
    # Include all resources
    includeResources = true
    
    # Include source code
    includeSources = false
    
    # Generate deployment descriptors
    generateDeployment = true
    
    # Validation level: STRICT, WARN, OFF
    validationLevel = "STRICT"
}
```

### Dictionary Plugin Configuration

```hocon
oap-dictionary-maven {
    # Source directory for dictionary definitions
    dictionarySource = "src/main/dictionary"
    
    # Output directory for generated code
    outputDirectory = "target/generated-sources"
    
    # Package for generated classes
    packageName = "com.example.generated"
    
    # Generate Java classes
    generateJava = true
    
    # Generate documentation
    generateDocs = false
    
    # Validation level
    validationLevel = "STRICT"
}
```

## Build Lifecycle

The OAP Maven plugins integrate into standard Maven lifecycle:

```
validate
    ↓
generate-sources
    ↓ (compile-dictionary)
process-sources
    ↓
generate-resources
    ↓
process-resources
    ↓ (process-application)
compile
    ↓
process-classes
    ↓
generate-test-sources
    ↓
process-test-sources
    ↓
generate-test-resources
    ↓
process-test-resources
    ↓
test-compile
    ↓
process-test-classes
    ↓
test
    ↓
package
    ↓ (package-application)
integration-test
    ↓
verify
    ↓
install
    ↓
deploy
```

## Common Goals

### oap:process-application
Process application configuration and resources.

```bash
mvn oap:process-application
```

### oap:compile-dictionary
Compile dictionary definitions to Java code.

```bash
mvn oap:compile-dictionary
```

### oap:validate-application
Validate application configuration without building.

```bash
mvn oap:validate-application
```

### oap:package-application
Package application with all dependencies.

```bash
mvn oap:package-application
```

## Generated Artifacts

After running the plugins, you get:

```
target/
  ├── generated-sources/
  │   ├── dictionary/
  │   │   └── com/example/generated/
  │   │       ├── Dictionary.java
  │   │       ├── Entity.java
  │   │       └── ...
  │   └── application/
  │       └── Application.java
  ├── classes/
  ├── my-oap-app-1.0.0.jar
  └── my-oap-app-1.0.0-deployment
```

## Error Handling

The plugins include validation and error handling:

```
[ERROR] Dictionary compilation failed:
  [ERROR] Dictionary field 'entityId' not found in Entity 'User'
  [ERROR] at src/main/dictionary/user.dict:5
  [ERROR] Validation level: STRICT - build failed
```

## Performance Tuning

### Skip Plugin Execution

```bash
mvn clean install -Doap.maven.skip=true
```

### Incremental Compilation

The plugins support incremental builds when dependencies haven't changed.

### Parallel Builds

```bash
mvn clean install -T 1C  # 1 thread per core
```

## Sub-Module Details

### oap-maven
Base framework for OAP Maven plugins.

**Features:**
- Abstract Mojo base classes
- Parameter validation
- Logging utilities
- File system utilities

### oap-application-maven
Handles complete application lifecycle.

**Features:**
- Application validation
- Dependency resolution
- Resource bundling
- Deployment descriptor generation

### oap-dictionary-maven
Compiles dictionary definitions to executable code.

**Features:**
- Dictionary parsing
- Java code generation
- Documentation generation
- Validation and error reporting

## Integration with IDE

### Eclipse/STS
- Right-click project → Configure → Convert to Maven project
- Maven → Update project (clean imports)

### IntelliJ IDEA
- File → Import or Open pom.xml
- Automatically recognizes OAP Maven plugins
- Run configurations available in Maven tool window

### Visual Studio Code
- Install Maven for Java extension
- Opens pom.xml and recognizes plugins
- Run goals from command palette

## Troubleshooting

### Plugin Not Found
```
[ERROR] Plugin not found: oap:oap-maven-plugin
```
Solution: Ensure plugin is in active Maven repository

### Dictionary Compilation Error
```
[ERROR] Field 'id' not found in class User
```
Solution: Check dictionary definition matches Java classes

### Resource Processing Issues
```
[ERROR] Cannot find resource: src/main/resources
```
Solution: Verify resource directory exists and is configured

## Related Modules

- `oap-application` - Application framework
- `oap-stdlib` - Core utilities
- `oap-module` - Module system

## Best Practices

1. **Separate Concerns** - Use one plugin per responsibility
2. **Configuration** - Keep plugin configuration minimal
3. **Validation** - Enable STRICT validation in development
4. **Documentation** - Generate and include plugin documentation
5. **Testing** - Test plugin execution in CI/CD pipeline

## Further Reading

- See individual plugin README files for detailed documentation
- Check pom.xml examples in test modules
- Review Maven plugin development guide: https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
