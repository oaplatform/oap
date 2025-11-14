# OAP JSON

Extended JSON support with powerful schema validation, transformation, and comparison capabilities for the Open Application Platform (OAP).

## Overview

The `oap-json` module provides a comprehensive framework for validating, transforming, and analyzing JSON documents using declarative schemas. It enables developers to define complex validation rules, apply transformations, and compare JSON structures programmatically.

## Features

### Core Capabilities

1. **Schema-Based Validation**
   - Define validation rules in JSON or DSL format
   - Type checking for all JSON types
   - Required field validation
   - Default value support
   - Custom validators

2. **Type Support**
   - Primitives: boolean, string, number (integer, long, double), date
   - Collections: arrays, objects
   - Complex structures: dictionaries, nested objects
   - Custom types via extension

3. **Template Integration**
   - Embed template expressions in schema definitions
   - Dynamic schema generation
   - Schema inheritance and composition
   - Schema references and includes

4. **JSON Path Operations**
   - Navigate nested structures
   - Query and filter JSON
   - Extract values from complex paths
   - Path-based validation

5. **JSON Comparison**
   - Compare two JSON documents
   - Generate difference reports
   - Identify structural changes
   - Useful for testing and synchronization

## Module Structure

```
oap-json/
├── pom.xml
└── oap-json-schema/
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   └── java/oap/json/schema/
    │   │       ├── JsonSchema.java           # Core schema validator
    │   │       ├── JsonPath.java             # Path navigation
    │   │       ├── JsonDiff.java             # JSON comparison
    │   │       ├── SchemaStorage.java        # Schema loading
    │   │       ├── JsonSchemaException.java  # Error handling
    │   │       └── validator/                # Type-specific validators
    │   │           ├── AbstractJsonSchemaValidator.java
    │   │           ├── BooleanJsonValidator.java
    │   │           ├── DateJsonValidator.java
    │   │           ├── string/StringJsonValidator.java
    │   │           ├── number/               # Integer, Long, Double validators
    │   │           ├── array/ArrayJsonValidator.java
    │   │           ├── object/ObjectJsonValidator.java
    │   │           └── dictionary/DictionaryJsonValidator.java
    │   └── test/
    │       └── java/oap/json/schema/
    │           ├── SchemaTest.java           # Core validation tests
    │           ├── JsonPathTest.java
    │           ├── JsonDiffTest.java
    │           └── ... (other test classes)
```

## Quick Start

### Basic Schema Validation

```java
import oap.json.schema.JsonSchema;
import oap.json.schema.NodeResponse;

// Create a schema from JSON definition
String schemaJson = "{" +
    "type: object," +
    "properties: {" +
    "  name: {type: string, required: true}," +
    "  age: {type: integer, required: false}" +
    "}" +
"}";

JsonSchema schema = JsonSchema.schemaFromString(schemaJson);

// Validate data
String jsonData = "{\"name\": \"John\", \"age\": 30}";
NodeResponse response = schema.validate(jsonData);

if (response.hasErrors()) {
    response.errors().forEach(error -> System.out.println(error));
} else {
    System.out.println("Validation successful");
}
```

### Type Validators

```java
// Boolean validation
String boolSchema = "{type: boolean, required: true}";
JsonSchema schema = JsonSchema.schemaFromString(boolSchema);
schema.validate("true");  // OK
schema.validate("null");  // Error: required

// Number validation
String numberSchema = "{" +
    "type: integer," +
    "required: true" +
"}";
schema = JsonSchema.schemaFromString(numberSchema);
schema.validate("42");        // OK
schema.validate("42.5");      // Error: expected integer
schema.validate("\"text\"");  // Error: type mismatch
```

### Array Validation

```java
String arraySchema = "{" +
    "type: array," +
    "items: {type: string}" +
"}";

JsonSchema schema = JsonSchema.schemaFromString(arraySchema);
schema.validate("[\"a\", \"b\", \"c\"]");        // OK
schema.validate("[\"a\", 123]");                 // Error: item type mismatch
schema.validate("[[\"nested\", \"arrays\"]]");   // Error: item type mismatch
```

### Object and Dictionary Validation

```java
// Strict object with defined properties
String objectSchema = "{" +
    "type: object," +
    "properties: {" +
    "  id: {type: integer, required: true}," +
    "  name: {type: string, required: true}" +
    "}" +
"}";

// Dictionary with dynamic keys and typed values
String dictionarySchema = "{" +
    "type: dictionary," +
    "values: {type: string}" +
"}";

JsonSchema schema = JsonSchema.schemaFromString(dictionarySchema);
schema.validate("{\"key1\": \"value1\", \"key2\": \"value2\"}");  // OK
```

### Default Values and Optional Fields

```java
String schemaWithDefaults = "{" +
    "type: object," +
    "properties: {" +
    "  status: {type: string, default: \"active\"}," +
    "  count: {type: integer, default: 0}," +
    "  tags: {type: array, items: {type: string}}" +
    "}" +
"}";

JsonSchema schema = JsonSchema.schemaFromString(schemaWithDefaults);

// Missing fields with defaults will be filled
schema.validate("{}");  // Valid, defaults applied
```

### Using JSON Paths

```java
import oap.json.schema.JsonPath;

String json = "{" +
    "user: {" +
    "  name: \"John\"," +
    "  email: \"john@example.com\"," +
    "  address: {" +
    "    street: \"123 Main St\"," +
    "    city: \"Springfield\"" +
    "  }" +
    "}" +
"}";

JsonPath path = new JsonPath("user.address.city");
Object city = path.extract(json);  // "Springfield"
```

### Comparing JSON Documents

```java
import oap.json.schema.JsonDiff;

String json1 = "{\"a\": 1, \"b\": 2}";
String json2 = "{\"a\": 1, \"b\": 3, \"c\": 4}";

JsonDiff diff = new JsonDiff(json1, json2);
// Compare documents and get differences
// - b: changed from 2 to 3
// - c: added with value 4
```

### Schema References and Templates

```java
// Reference external schemas
String schemaWithTemplate = "{" +
    "template: \"schema/user.json\"," +
    "properties: {" +
    "  profile: {type: object, required: true}" +
    "}" +
"}";

JsonSchema schema = JsonSchema.schemaFromString(schemaWithTemplate);
```

## Advanced Features

### Custom Validators

Extend `AbstractJsonSchemaValidator` to create custom validators:

```java
public class CustomValidator extends AbstractJsonSchemaValidator<CustomType> {
    public CustomValidator() {
        super("customtype");
    }

    @Override
    public NodeResponse validate(Object value, SchemaPath path, JsonSchemaParserContext context) {
        // Custom validation logic
        return new NodeResponse(/* ... */);
    }
}

// Register custom validator
JsonSchema.add(new CustomValidator());
```

### Dynamic Boolean References

Use `DynamicBooleanReference` for context-aware validation:

```java
// Validation rules that depend on other properties
String conditionalSchema = "{" +
    "type: object," +
    "properties: {" +
    "  type: {type: string}," +
    "  value: {type: any}" +
    "}" +
"}";
```

### Schema Storage

Load schemas from resources:

```java
import oap.json.schema.ResourceSchemaStorage;

// Load from classpath resources
JsonSchema schema = JsonSchema.schema("schemas/user.json");

// Or use custom storage
JsonSchema schema = JsonSchema.schemaFromString(
    schemaJson,
    customStorageInstance
);
```

## Testing Examples

### From SchemaTest.java

```java
@Test
public void requiredProperty() {
    String schema = "{type: object, properties: {a: {type: boolean, required: true}}}";
    assertFailure(schema, "{}", "/a: required property is missing");
}

@Test
public void defaultValue() {
    String schema = "{type: boolean, default: true}";
    assertOk(schema, "null");  // Default applied to null
}

@Test
public void templateSchema() {
    String schema = "{" +
        "template: \"schema/test.json\"," +
        "properties: {" +
        "  a: {}," +
        "  b: {type: string, required: true}" +
        "}" +
    "}";
    assertOk(schema, "{'a': 'test', 'b': 'test'}");
}
```

## Error Handling

Schema validation returns error information:

```java
NodeResponse response = schema.validate(data);

if (response.hasErrors()) {
    // Get detailed error information
    response.errors().forEach(error -> {
        System.out.println("Path: " + error.path());
        System.out.println("Message: " + error.message());
        System.out.println("Type: " + error.type());
    });
}
```

Common error messages:
- `required property is missing` - Required field not present
- `instance type is X, but allowed type is Y` - Type mismatch
- `invalid format` - Format validation failed
- `value is not in enum` - Value not in allowed set

## Performance Considerations

1. **Schema Caching**: Schemas are automatically cached after first parse
2. **Lazy Evaluation**: Validation stops at first error in strict mode
3. **Streaming**: Use paths to extract specific values without full parsing
4. **Type-Specific Validators**: Each type has optimized validation logic

## Integration with Other OAP Modules

### With oap-template

Schemas can use template expressions:

```java
String templateSchema = "{" +
    "type: object," +
    "properties: {" +
    "  name: {type: string, template: \"{{user.name}}\"}" +
    "}" +
"}";
```

### With oap-logstream

Use JSON schemas to validate log data structures.

## Supported JSON Schemas

The module supports a custom JSON schema DSL:

```
{
  type: <string|integer|long|double|boolean|date|object|array|dictionary|any>,
  required: <boolean>,
  default: <value>,
  properties: { <object properties> },
  items: { <array items> },
  values: { <dictionary values> },
  enum: [ <allowed values> ],
  template: <string>
}
```

## Building

```bash
mvn clean install
```

Build only oap-json:

```bash
mvn -pl oap-formats/oap-json clean install
```

## Testing

```bash
mvn test
```

Run specific test class:

```bash
mvn -pl oap-formats/oap-json test -Dtest=SchemaTest
```

## Related Classes

- `JsonSchemaParserContext` - Parser state and context
- `SchemaId` - Identifier for schema instances
- `SchemaPath` - Path to property in schema
- `JsonValidatorProperties` - Configuration for validators
- `ValidationSyntaxException` - Schema syntax errors
- `JsonSchemaException` - Validation errors

## License

MIT License - See LICENSE file for details

## Further Reading

- [JSON Schema Specification](https://json-schema.org/)
- [OAP Framework Documentation](https://github.com/oaplatform/oap)
- [Related oap-template Module](../oap-template/README.md)
