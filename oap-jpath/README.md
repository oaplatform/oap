# OAP JPath

JSON Path expression evaluator for accessing nested object properties and invoking methods on Java objects.

## Overview

OAP JPath is a powerful path expression language for extracting and accessing nested data in Java objects using a JSON-like syntax. It supports:
- Variable substitution with `${variable}` syntax
- Nested property access (dot notation)
- Array and list indexing
- Method invocation with arguments
- Private field and method access
- Stream operations and functional programming
- ANTLR-based parsing with full expression language support

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-jpath</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **Expression Syntax** - `${variable.property.method()[index].field}` style path expressions
- **Type Flexibility** - Works with Maps, JavaBeans, Lists, Arrays, and raw objects
- **Method Invocation** - Call methods with arguments: `${obj.method(arg1, arg2)}`
- **Reflective Access** - Access private fields and methods
- **Collection Support** - Index into arrays, lists, and maps
- **Streaming** - Call stream operations: `${map.keySet().stream().count()}`
- **Flexible Output** - Multiple output implementations for different use cases

## Key Classes

- `JPath` - Main entry point for evaluating path expressions
- `Expression` - Base interface for evaluable expressions
- `Pointer` - Base for references to values in objects
- `MapPointer` - Access to map entries
- `ObjectPointer` - Access to object fields and methods
- `PathExpression` - Complex expressions with method calls
- `JPathOutput` - Interface for output implementations
- `StringBuilderJPathOutput` - Outputs to StringBuilder

## Quick Example

```java
import oap.jpath.JPath;
import oap.jpath.StringBuilderJPathOutput;
import java.util.Map;

// Simple variable substitution
var output = new StringBuilderJPathOutput();
JPath.evaluate("${name}", Map.of("name", "John"), output);
System.out.println(output); // Output: John

// Nested property access
var user = Map.of(
    "name", "Alice",
    "address", Map.of("city", "NYC")
);
output = new StringBuilderJPathOutput();
JPath.evaluate("${user.address.city}", Map.of("user", user), output);
System.out.println(output); // Output: NYC

// Array indexing
var numbers = new int[]{10, 20, 30};
output = new StringBuilderJPathOutput();
JPath.evaluate("${arr[1]}", Map.of("arr", numbers), output);
System.out.println(output); // Output: 20

// Method invocation
output = new StringBuilderJPathOutput();
JPath.evaluate("${map.keySet().stream().count()}", 
    Map.of("map", Map.of("a", 1, "b", 2)), output);
System.out.println(output); // Output: 2

// List access
var items = List.of(
    new Item("Apple", 1.5),
    new Item("Banana", 0.8)
);
output = new StringBuilderJPathOutput();
JPath.evaluate("${items[0].name}", Map.of("items", items), output);
System.out.println(output); // Output: Apple
```

## Expression Language

### Basic Syntax
- `${variable}` - Access a variable
- `${obj.property}` - Access nested property
- `${obj.method()}` - Call method with no args
- `${obj.method(arg1, arg2)}` - Call method with arguments
- `${array[0]}` - Array/list index access
- `${map["key"]}` - Map key access

### Supported Operations
- Property/field access through reflection
- Method calls with type coercion
- Array and collection indexing
- Streaming and functional operations
- Chained expressions: `${a.b().c.d}`

## Supported Data Types

- JavaBeans with getters/setters or public fields
- Maps (HashMap, LinkedHashMap, etc.)
- Lists and Collections
- Arrays (primitives and objects)
- Any object with accessible methods/fields

## Advanced Features

### Private Member Access
JPath uses reflection to access private fields and methods:
```java
// Access private field
JPath.evaluate("${obj.privateField}", variables, output);

// Call private method
JPath.evaluate("${obj.getPrivate()}", variables, output);
```

### Type Coercion
Arguments to methods are automatically type-coerced to match method signatures.

### Null Handling
Accessing null references returns empty string by default, no exceptions thrown.

## Performance Considerations

- JPath parses expressions at evaluation time using ANTLR parser
- Cache compiled expressions for repeated use
- Reflection overhead for field/method access is typical
- Suitable for template processing, config, and moderate-frequency use
- Not optimized for extremely high-frequency evaluations

## Related Modules

- `oap-stdlib` - Core utilities
- `oap-template` - Template processing using JPath
- `oap-application` - Application framework that uses JPath for config

## Testing

See `oap-jpath/src/test/java/oap/jpath/JPathTest.java` for comprehensive examples.

## Implementation Details

JPath uses ANTLR 4 parser with a custom grammar for path expressions. The grammar
is defined in the ANTLR profile and auto-generated during build with:
```
mvn -Dantlr clean compile
```
