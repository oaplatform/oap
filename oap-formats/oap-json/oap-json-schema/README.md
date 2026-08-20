# oap-json-schema

Data-driven JSON schema validation for the OAP platform. Schemas are HOCON or JSON documents; validation returns a `List<String>` of error messages — empty means valid. No annotations, no code generation.

## Schema types

Schemas are written in HOCON (or JSON). Every schema node has a `type` field.

| Type | Description |
|---|---|
| `boolean` | `true` or `false` |
| `string` / `text` | UTF-8 string |
| `integer` | 32-bit integer |
| `long` | 64-bit integer |
| `double` | 64-bit floating point |
| `date` | ISO 8601 datetime string (`2024-06-01T00:00:00.000Z`) |
| `object` | JSON object with named `properties` |
| `array` | JSON array with typed `items` |
| `dictionary` | Key-value map |
| `any` | Accepts any JSON value including `null` |

## Common keywords

These keywords apply to most types:

| Keyword | Description |
|---|---|
| `required: true` | **Deprecated** — per-field marker meaning the field must be present and non-null. Prefer the object-level `required: [name, ...]` array (draft 2020-12 style, see `object` keywords below) |
| `default: <value>` | Default value applied when the field is null (returned from validate) |
| `enum: [val1, val2]` | Static allowed-values constraint |
| `enum: {json-path: fieldName}` | Dynamic enum — allowed values taken from another field in the same object |
| `enum: {json-path: fieldName, ne: excluded}` | Dynamic enum with one value excluded |
| `enabled: {json-path: fieldName, eq: value}` | **Deprecated** — field is only validated when another field equals a specific value. Prefer object-level `if`/`then`/`else` (draft 2020-12 style, see `object` keywords below) |

## Type-specific keywords

### `string` / `text`

| Keyword | Description |
|---|---|
| `minLength: N` | Minimum string length |
| `maxLength: N` | Maximum string length |
| `pattern: "regex"` | Must match the full regex |

### `integer` / `long` / `double`

| Keyword | Description |
|---|---|
| `minimum: N` | Minimum value (inclusive unless `exclusiveMinimum: true`) |
| `maximum: N` | Maximum value (inclusive unless `exclusiveMaximum: true`) |
| `exclusiveMinimum: true` | Makes `minimum` exclusive |
| `exclusiveMaximum: true` | Makes `maximum` exclusive |

### `object`

| Keyword | Description |
|---|---|
| `properties: { name: {…} }` | Named child schemas |
| `additionalProperties: false` | Reject properties not listed in `properties` |
| `extends: "path/to/schema"` | Merge properties from another schema file |
| `required: [name, ...]` | Instance must contain every listed property name (draft 2020-12 style; distinct from per-field `required: true`) |
| `if: {…}`, `then: {…}`, `else: {…}` | Conditional composition (draft 2020-12): when the instance validates against `if`, it must also validate against `then`; otherwise it must validate against `else`. Either branch may be omitted, in which case it imposes no constraint |

> `required: [name, ...]` is the preferred form. Per-field `required: true` (see common keywords above) is deprecated.

> `if`/`then`/`else` subschemas must declare an explicit `type` like any other schema node in this module (draft JSON Schema examples often omit `type` — that's not valid here).

```hocon
// select postal_code format based on country, US vs Canada
{
  type = object
  properties {
    country     { type = string }
    postal_code { type = string }
  }
  if   { type = object, properties { country { type = string, enum = [ "United States of America" ] } }, required = [ country ] }
  then { type = object, properties { postal_code { type = string, pattern = "[0-9]{5}(-[0-9]{4})?" } } }
  else { type = object, properties { postal_code { type = string, pattern = "[A-Z][0-9][A-Z] [0-9][A-Z][0-9]" } } }
}
```

### `array`

| Keyword | Description |
|---|---|
| `items: {…}` | Schema applied to every element |
| `minItems: N` | Minimum number of elements |
| `maxItems: N` | Maximum number of elements |
| `idField: "fieldName"` | Identity field for object-array diff (used by `JsonDiff`) |
| `idField: "{index}"` | Use element position as identity for diff |

## Schema composition

### `extends`

Merges properties from a referenced schema. Nested objects are merged recursively; local definitions take precedence.

```hocon
{
  type = object
  extends = "base/product"
  properties {
    discount { type = double }
  }
}
```

### `"$ref"`

Uses another schema as the type for a field or array items.

```hocon
{
  type = object
  properties {
    address { "$ref" = "/schema/address" }
    tags {
      type = array
      items { "$ref" = "/schema/tag" }
    }
  }
}
```

## Schema format examples

```hocon
// Primitive field
{ type = string, required = true, minLength = 1, maxLength = 255 }

// Object with fields
{
  type = object
  additionalProperties = false
  properties {
    name   { type = string, required = true }
    age    { type = integer, minimum = 0 }
    active { type = boolean, default = true }
  }
}

// Object with draft 2020-12 style required properties
{
  type = object
  properties {
    name { type = string }
    age  { type = integer, minimum = 0 }
  }
  required = [name]
}

// Array of objects
{
  type = array
  minItems = 1
  items {
    type = object
    properties {
      id   { type = string, required = true }
      qty  { type = integer, minimum = 1 }
    }
  }
}

// Static enum
{ type = string, enum = [PENDING, ACTIVE, CLOSED] }

// Dynamic enum — value of 'b' must equal the value of 'a'
{
  type = object
  properties {
    a { type = string }
    b { type = string, enum { json-path = a } }
  }
}
```

## `JsonSchema` API

### Load and validate

```java
// Load from classpath resource (cached across calls)
JsonSchema schema = JsonSchema.schema( "schemas/product" );

// Parse inline (not cached)
JsonSchema schema = JsonSchema.schemaFromString( """
    { type = object, properties { name { type = string, required = true } } }
    """ );

// Validate — returns error messages; empty list means valid
Object json = Binder.json.unmarshal( Object.class, jsonString );
List<String> errors = schema.validate( json, false );
if( !errors.isEmpty() ) {
    // handle errors
}
```

`validate` parameters:

| Parameter | Description |
|---|---|
| `json` | Pre-parsed JSON object (`Map`, `List`, scalar, or `null`) |
| `ignoreRequiredDefault` | When `true`, `required` constraints are not enforced (useful for partial saves) |
| `forceIgnoreAdditionalProperties` | When `true`, `additionalProperties: false` is not enforced |

### Partial validation

Validate only a sub-path of the schema against a value, given the full root document for dynamic constraint evaluation (e.g., `enabled`, dynamic `enum`).

```java
// Validate only the fields inside array items at path "lines.items"
List<String> errors = schema.partialValidate( rootJson, partialJson, "lines.items", false );
```

### Custom `SchemaStorage`

`SchemaStorage` is a `@FunctionalInterface` — implement it to load schema files from any source (database, network, test fixture):

```java
SchemaStorage storage = name -> switch( name ) {
    case "schemas/address" -> addressSchemaJson;
    case "schemas/tag"     -> tagSchemaJson;
    default -> throw new JsonSchemaException( "unknown schema: " + name );
};

JsonSchema schema = JsonSchema.schemaFromString( rootSchemaJson, storage );
```

`ResourceSchemaStorage.INSTANCE` (the default) resolves schemas from the classpath. It tries `name.conf`, `name.yaml`, and `name.json` in order, and also merges any override files found in a sibling directory (`name/filename.conf`).

## `JsonDiff`

Computes a structural diff between two JSON strings guided by a parsed schema. Useful for audit logs and change tracking.

```java
JsonSchema schema = JsonSchema.schemaFromString( schemaJson );
JsonDiff diff = JsonDiff.diff( oldJson, newJson, schema.schema );

for( JsonDiff.Line line : diff.getDiff() ) {
    System.out.printf( "%s: %s → %s%n",
        line.path,
        line.oldValue.orElse( "∅" ),
        line.newValue.orElse( "∅" ) );
}
```

### `JsonDiff.Line`

| Field | Type | Description |
|---|---|---|
| `path` | `String` | Dot-path to the changed field (e.g., `address.city`, `lines[0].qty`) |
| `lineType` | `LineType` | `FIELD`, `ARRAY`, or `OBJECT` |
| `oldValue` | `Optional<String>` | JSON-serialized previous value (`Optional.empty()` if was absent) |
| `newValue` | `Optional<String>` | JSON-serialized new value (`Optional.empty()` if now absent) |

Array elements in object-typed arrays are identified by the field named in `idField`. Arrays with `idField = "{index}"` are compared positionally.

## Errors

| Exception | Thrown when |
|---|---|
| `JsonSchemaException` | A classpath schema resource cannot be found |
| `ValidationSyntaxException` | The schema itself references an unknown type |
| `UnknownTypeValidationSyntaxException` | Subtype of `ValidationSyntaxException` for unregistered type names |
