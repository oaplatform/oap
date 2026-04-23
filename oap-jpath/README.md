# oap-jpath

JPath expression language for navigating Java objects and maps using reflection. Expressions are parsed by an ANTLR4 grammar and evaluated against a variable map, traversing public and private fields, calling methods, and indexing arrays or lists — all in a single `${…}` expression.

## Expression syntax

Every JPath expression is wrapped in `${…}`. The first segment names a variable from the provided map; subsequent segments are chained with `.`.

```
${variable}
${variable.field}
${variable.field.nestedField}
${variable.method()}
${variable.method("arg", 2)}
${variable.array[0]}
${variable.list[1].field}
```

### Path segment types

| Form | Example | Resolves via |
|---|---|---|
| `identifier` | `name` | Field access — public or private, via reflection |
| `name(args…)` | `getLabel("x", 2)` | Method call — public or private, via reflection |
| `name[n]` | `items[1]` | Array element or `List.get(n)` |

Segments can be chained freely:

```
${order.lines[0].product.getPrice("USD")}
```

### Method arguments

Methods accept string literals and decimal integer literals as arguments.

| Literal | Example | Parsed as |
|---|---|---|
| String | `"hello"` | `String` |
| Decimal integer | `42` | Parsed as `Long`, auto-coerced to the target parameter type (`int`, `long`, `float`, `double`, `short`, `byte`) |

## API

### `JPath.evaluate`

```java
StringBuilderJPathOutput output = new StringBuilderJPathOutput();

JPath.evaluate(
    "${user.address.city}",
    Map.of( "user", user ),
    output
);

String result = output.toString();
```

Static shorthand — builds a `JPath` instance and evaluates in one call. For repeated evaluation against the same variable set, construct a `JPath` instance directly:

```java
JPath jpath = new JPath( Map.of( "user", user ) );

jpath.evaluate( "${user.name}", output );
output.reset();
jpath.evaluate( "${user.email}", output );
```

### `StringBuilderJPathOutput`

Built-in `JPathOutput` implementation that collects results into a `StringBuilder`.

| Method | Description |
|---|---|
| `toString()` | Returns the accumulated string value |
| `reset()` | Clears the buffer for re-use |

## Examples

```java
// Simple variable lookup
JPath.evaluate( "${id}", Map.of( "id", 42 ), output );
// → "42"

// Nested field access (public field)
JPath.evaluate( "${order.status}", Map.of( "order", order ), output );

// Private field access
JPath.evaluate( "${bean.internalState}", Map.of( "bean", bean ), output );

// Private method call
JPath.evaluate( "${bean.computeScore()}", Map.of( "bean", bean ), output );

// Method with string argument
JPath.evaluate( "${bean.format(\"prefix\")}", Map.of( "bean", bean ), output );

// Method with multiple arguments (string + integer)
JPath.evaluate( "${bean.pad(\"x\", 5)}", Map.of( "bean", bean ), output );

// Array element access
JPath.evaluate( "${data.scores[2]}", Map.of( "data", data ), output );

// List element + field chain
JPath.evaluate( "${order.lines[0].productName}", Map.of( "order", order ), output );

// Chaining Java API calls
JPath.evaluate( "${map.keySet().stream().count()}", Map.of( "map", map ), output );
```

## Custom output

`JPathOutput` is a `@FunctionalInterface`. Implement it to collect typed values without converting to a string:

```java
List<Object> collected = new ArrayList<>();

JPathOutput collector = pointer -> collected.add( pointer.get() );

JPath.evaluate( "${item.price}", Map.of( "item", item ), collector );

BigDecimal price = (BigDecimal) collected.get( 0 );
```

The `Pointer` passed to `write` is one of:

| Implementation | `get()` returns |
|---|---|
| `ObjectPointer<T>` | The resolved object |
| `MapPointer` | The resolved `Map` |
| `NullPointer` | `null` |

## Errors

| Exception | Thrown when |
|---|---|
| `PathNotFoundException` | A field or method named in the expression does not exist on the target object |
| `ReflectionException` | Reflection access fails (e.g., module access denied) |
