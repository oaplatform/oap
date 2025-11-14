# OAP Template

An ANTLR-based dynamic template engine for the Open Application Platform (OAP) that enables runtime generation of Java objects and transformation of data using declarative template syntax.

## Overview

The `oap-template` module provides a powerful template processing engine based on ANTLR (Another Tool for Language Recognition). It allows developers to dynamically generate Java objects from templates, evaluate expressions, access object fields and methods, and perform data transformations at runtime without code generation.

## Key Features

1. **Dynamic Java Object Generation**
   - Generate Java objects from template configurations
   - Type-safe template rendering
   - Support for complex object hierarchies

2. **Expression Evaluation**
   - Rich expression language
   - Field access and method invocation
   - Optional/nullable handling with graceful null handling
   - Optional chaining for safe navigation

3. **Data Transformation**
   - Transform maps to objects
   - Template-based data mapping
   - Type conversion
   - Default value support

4. **Built-in Functions**
   - String functions: `urlencode()`, `toUpperCase()`, `toLowerCase()`, etc.
   - Math operations: sum, concatenation
   - Filtering and conditional logic

5. **Multiple Output Formats**
   - String output
   - Binary serialization
   - Object accumulation
   - Custom accumulators

6. **Error Handling**
   - Graceful null handling
   - Optional field resolution
   - Error recovery strategies

## Module Structure

```
oap-template/
├── pom.xml
└── src/
    ├── main/
    │   ├── antlr4/oap/template/
    │   │   ├── TemplateGrammar.g4      # Main grammar definition
    │   │   └── TemplateGrammarExpression.g4
    │   ├── java-antlr-generated/       # ANTLR-generated classes
    │   │   ├── TemplateGrammar.java
    │   │   ├── TemplateLexer.java
    │   │   ├── TemplateGrammarExpression.java
    │   │   └── ... (other generated classes)
    │   └── java/oap/template/
    │       ├── TemplateEngine.java     # Core template engine
    │       ├── Template.java           # Base template interface
    │       ├── JavaTemplate.java       # Java object template
    │       ├── DataModelTemplate.java  # Data model template
    │       ├── DictionaryTemplate.java # Dictionary template
    │       ├── TemplateMacros.java     # Macro definitions
    │       ├── Types.java              # Type definitions
    │       ├── TemplateAccumulator*.java # Output accumulators
    │       ├── render/                 # AST rendering classes
    │       │   ├── Render.java
    │       │   ├── AstRender.java
    │       │   ├── AstRenderField.java
    │       │   ├── AstRenderFunction.java
    │       │   ├── AstRenderMethod.java
    │       │   ├── AstRenderOptional.java
    │       │   ├── AstRenderNullable.java
    │       │   ├── AstRenderExpression.java
    │       │   └── ... (other render classes)
    │       ├── tree/                   # AST tree classes
    │       │   ├── Element.java
    │       │   ├── ExpressionElement.java
    │       │   ├── Expression.java
    │       │   ├── Expr.java
    │       │   └── ... (other tree classes)
    │       └── BinaryInputStream.java  # Binary I/O utilities
    └── test/
        └── java/oap/template/
            └── TemplateEngineTest.java # Comprehensive tests
```

## Quick Start

### Basic String Template

```java
import oap.template.TemplateEngine;
import oap.template.TemplateAccumulators;
import oap.reflect.TypeRef;
import java.util.Map;

// Create template engine
TemplateEngine engine = new TemplateEngine();

// Get a string template
var template = engine.getTemplate(
    "greeting",
    new TypeRef<Map<String, String>>() {},
    "Hello {{name}}!",
    TemplateAccumulators.STRING,
    null
);

// Render template
Map<String, String> data = Map.of("name", "World");
String result = template.render(data).get();
System.out.println(result);  // Output: "Hello World!"
```

### Object Template

```java
import oap.template.Template;
import java.util.Map;

// Template for mapping data to objects
var template = engine.getTemplate(
    "user",
    new TypeRef<Map<String, Object>>() {},
    "{{name}} ({{age}} years old)",
    TemplateAccumulators.STRING,
    null
);

Map<String, Object> user = Map.of(
    "name", "John",
    "age", 30
);
String result = template.render(user).get();
System.out.println(result);  // Output: "John (30 years old)"
```

### Accessing Object Fields

```java
public class Person {
    public String firstName;
    public String lastName;
    public int age;
}

// Template accessing object fields
var template = engine.getTemplate(
    "person",
    new TypeRef<Person>() {},
    "{{firstName}} {{lastName}}, age {{age}}",
    TemplateAccumulators.STRING,
    null
);

Person person = new Person();
person.firstName = "John";
person.lastName = "Doe";
person.age = 30;

String result = template.render(person).get();
System.out.println(result);  // Output: "John Doe, age 30"
```

### Method Invocation

```java
public class User {
    private String name;
    
    public User(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getNameUpperCase() {
        return name.toUpperCase();
    }
}

// Template with method invocation
var template = engine.getTemplate(
    "user",
    new TypeRef<User>() {},
    "Name: {{name}}, Uppercase: {{nameUpperCase}}",
    TemplateAccumulators.STRING,
    null
);

User user = new User("john");
String result = template.render(user).get();
System.out.println(result);  // Output: "Name: john, Uppercase: JOHN"
```

### Optional Handling

```java
public class Address {
    public String street;
    public String city;
}

public class Person {
    public String name;
    public Address address;
}

// Template with optional handling
var template = engine.getTemplate(
    "person",
    new TypeRef<Person>() {},
    "{{name}}, City: {{address?.city}}",
    TemplateAccumulators.STRING,
    null
);

// When address is null, the field safely returns null
Person person = new Person();
person.name = "John";
person.address = null;

String result = template.render(person).get();
System.out.println(result);  // Output: "John, City: null"
```

### Built-in Functions

```java
// String transformation functions
var template = engine.getTemplate(
    "transform",
    new TypeRef<Map<String, String>>() {},
    "Lowercase: {{name.toLowerCase()}}, " +
    "Uppercase: {{name.toUpperCase()}}, " +
    "URL Encoded: {{name.urlencode()}}",
    TemplateAccumulators.STRING,
    null
);

Map<String, String> data = Map.of("name", "hello world");
String result = template.render(data).get();
// Output: "Lowercase: hello world, Uppercase: HELLO WORLD, URL Encoded: hello%20world"
```

### Collection Handling

```java
public class Order {
    public String orderId;
    public List<String> items;
}

// Template with collection iteration
var template = engine.getTemplate(
    "order",
    new TypeRef<Order>() {},
    "Order {{orderId}}: {{items}}",
    TemplateAccumulators.STRING,
    null
);

Order order = new Order();
order.orderId = "ORD-123";
order.items = List.of("Item1", "Item2", "Item3");

String result = template.render(order).get();
// Output: "Order ORD-123: [Item1, Item2, Item3]"
```

### Binary Output

```java
import java.io.IOException;

// Template with binary accumulator
var template = engine.getTemplate(
    "binary",
    new TypeRef<Map<String, String>>() {},
    "data",
    TemplateAccumulators.BINARY,
    null
);

Map<String, String> data = Map.of();
byte[] result = template.render(data).get();

// Process binary data
BinaryInputStream bis = new BinaryInputStream(result);
List<List<String>> rows = BinaryUtils.read(bis);
```

### Error Handling

```java
import static oap.template.ErrorStrategy.*;

// Ignore errors and continue
var template = engine.getTemplate(
    "safe",
    new TypeRef<Map<String, Object>>() {},
    "Value: {{missingField}}",
    TemplateAccumulators.STRING,
    IGNORE  // Error strategy
);

// Throw exception on error
var strictTemplate = engine.getTemplate(
    "strict",
    new TypeRef<Map<String, Object>>() {},
    "Value: {{requiredField}}",
    TemplateAccumulators.STRING,
    ERROR  // Error strategy
);
```

## Advanced Features

### Custom Macros

```java
import oap.template.TemplateMacros;

// Define custom macro
TemplateMacros.register("custom", context -> {
    // Custom macro implementation
    return "custom value";
});

// Use in template
var template = engine.getTemplate(
    "custom",
    new TypeRef<Map<String, String>>() {},
    "{{custom()}}",
    TemplateAccumulators.STRING,
    null
);
```

### Template Composition

```java
// Create reusable template components
var headerTemplate = engine.getTemplate(
    "header",
    new TypeRef<Map<String, String>>() {},
    "=== {{title}} ===",
    TemplateAccumulators.STRING,
    null
);

var contentTemplate = engine.getTemplate(
    "content",
    new TypeRef<Map<String, String>>() {},
    "{{body}}",
    TemplateAccumulators.STRING,
    null
);

// Compose templates
String header = headerTemplate.render(Map.of("title", "Report")).get();
String content = contentTemplate.render(Map.of("body", "Content here")).get();
String result = header + "\n" + content;
```

### Type System Integration

```java
import oap.template.Types;

// Work with OAP type system
var types = new Types();

// Template accessing type information
var template = engine.getTemplate(
    "typed",
    new TypeRef<Map<String, Object>>() {},
    "Value: {{value}} (Type: {{value.class.simpleName}})",
    TemplateAccumulators.STRING,
    null
);
```

## Testing Examples

### From TemplateEngineTest.java

```java
@Test
public void testRenderStringText() {
    Template<Map<String, String>> template = engine.getTemplate(
        "test",
        new TypeRef<Map<String, String>>() {},
        "sdkjf hdkfgj d$...{}",
        STRING,
        null
    );
    
    assertThat(template.render(null).get())
        .isEqualTo("sdkjf hdkfgj d$...{}");
}

@Test
public void testEscapeVariables() {
    TestTemplateClass c = new TestTemplateClass();
    c.field = "1";
    
    Template<TestTemplateClass> template = engine.getTemplate(
        "test",
        new TypeRef<TestTemplateClass>() {},
        "${field}",
        STRING,
        null
    );
    
    assertThat(template.render(c).get())
        .isEqualTo("${field}");  // No variable substitution with $
}
```

## Integration with Other Modules

### With oap-json

```java
import oap.json.schema.JsonSchema;

// Template can be used within JSON schemas
JsonSchema schema = JsonSchema.schemaFromString(
    "{template: \"schema/user.json\", properties: {...}}"
);
```

### With oap-logstream

```java
import oap.logstream.TemplateLogger;

// Template-based log ID generation
TemplateLogger logger = new TemplateLogger(backend, template);
```

## Architecture Patterns

### ANTLR Grammar
- Uses ANTLR4 for parsing template syntax
- Separate grammars for basic and expression parsing
- Lexer and parser generation

### Visitor Pattern
- AST nodes implement rendering logic
- Custom render classes for each node type
- Visitor traversal through template tree

### Accumulator Pattern
- Different output types use specialized accumulators
- String, Binary, Object accumulators
- Pluggable accumulator system

### Error Recovery
- Graceful null handling
- Optional field resolution
- Error strategy configuration

## Performance Considerations

1. **Template Caching**: Templates are automatically cached after first compilation
2. **Lazy Evaluation**: Expressions evaluated only when rendered
3. **Type Safety**: Type checking at compile time reduces runtime errors
4. **Streaming**: Binary output supports streaming for large data

## Building

```bash
mvn clean install
```

Build only oap-template:

```bash
mvn -pl oap-formats/oap-template clean install
```

## Testing

```bash
mvn test
```

Run specific test class:

```bash
mvn -pl oap-formats/oap-template test -Dtest=TemplateEngineTest
```

## Related Classes

- `TemplateEngine` - Main engine for creating templates
- `Template<T>` - Generic template interface
- `JavaTemplate` - Template for Java objects
- `DataModelTemplate` - Template for data models
- `DictionaryTemplate` - Template for dictionaries
- `TemplateAccumulator` - Output accumulation
- `ErrorStrategy` - Error handling configuration

## Troubleshooting

### Field Not Found

Ensure field names match the object structure:

```java
// Check field name case sensitivity
var template = engine.getTemplate(
    "test",
    new TypeRef<Person>() {},
    "{{firstName}}",  // Must match exact case
    STRING,
    null
);
```

### Null Pointer Exception

Use optional chaining for nullable fields:

```java
// Unsafe
var template = engine.getTemplate(
    "test",
    new TypeRef<Person>() {},
    "{{address.city}}",  // May throw if address is null
    STRING,
    null
);

// Safe
var template = engine.getTemplate(
    "test",
    new TypeRef<Person>() {},
    "{{address?.city}}",  // Returns null if address is null
    STRING,
    null
);
```

### Method Not Found

Ensure method exists and is public:

```java
public class Person {
    public String getName() { return name; }  // OK
    private String getPrivate() { ... }       // Not accessible
}
```

## ANTLR Documentation

For more information on ANTLR:
- [ANTLR 4 Documentation](https://github.com/antlr/antlr4/blob/master/doc/index.md)
- [ANTLR4 Getting Started](https://www.antlr.org/getting-started)

## License

MIT License - See LICENSE file for details

## Further Reading

- [OAP Framework Documentation](https://github.com/oaplatform/oap)
- [Related oap-json Module](../oap-json/README.md)
- [Related oap-logstream Module](../oap-logstream/README.md)
