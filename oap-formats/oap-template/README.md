# oap-template

A compile-time template engine for the OAP framework. Each unique template string is parsed once, compiled into a real Java class at first use, and cached — subsequent renders invoke the compiled class directly with no re-parsing overhead.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Template Syntax](#template-syntax)
  - [Delimiters](#delimiters)
  - [Whitespace trimming](#whitespace-trimming)
  - [Field access](#field-access)
  - [Null safety](#null-safety)
  - [Default values (`??`)](#default-values-)
  - [Fallback chains (`| default`)](#fallback-chains--default)
  - [Concatenation](#concatenation)
  - [Math](#math)
  - [If / then / else (inline)](#if--then--else-inline)
    - [Compound conditions (inline)](#compound-conditions-inline)
  - [If / else / end (block)](#if--else--end-block)
    - [Compound conditions](#compound-conditions)
    - [Truthiness semantics](#truthiness-semantics)
  - [With scope (inline)](#with-scope-inline)
  - [With scope (block)](#with-scope-block)
  - [Pipe-to-function](#pipe-to-function)
  - [Cast types](#cast-types)
  - [Block comments](#block-comments)
- [Built-in Functions](#built-in-functions)
- [Custom Functions](#custom-functions)
- [Output Accumulators](#output-accumulators)
- [Using the Engine in Java](#using-the-engine-in-java)
- [Runtime Interpreter Mode](#runtime-interpreter-mode)
- [OAP Module Integration](#oap-module-integration)
- [Aliases](#aliases)
- [Error Handling](#error-handling)
- [Disk Cache](#disk-cache)

---

## Overview

`oap-template` turns user-supplied template strings into efficient Java code. It is designed for high-throughput scenarios such as:

- Serializing data rows to TSV / log formats driven by configuration
- Generating HTTP response bodies (bid responses, URL macros) from per-advertiser rules
- Extracting single fields from objects without reflection at render time

The engine is output-agnostic: a pluggable `TemplateAccumulator` decides how each Java type is serialised.

---

## Architecture

```
Template string + TypeRef + Accumulator
        │
        ▼
  TemplateLexer / TemplateGrammar   ← tokenises text vs ${ } / {{ }} blocks
        │
        ▼
  TemplateGrammarExpression         ← parses expression blocks into an AST
        │
        ▼
  TemplateAstUtils.toAst()          ← walks the type hierarchy via reflection,
                                       resolves fields/methods, builds AstRender nodes
        │
        ├─── getTemplate() ──────────────────────────────────────────────────────┐
        │                                                                        │
        ▼                                                                        │
  Render (code generation)          ← emits a Java source file                  │
        │                                                                        │
        ▼                                                                        │
  MemoryClassLoaderJava             ← compiles and loads the generated class     │
        │                                                                        │
        ▼                                                                        │
  Guava Cache  ←→  optional disk cache                                           │
        │                                                                        │
        ▼                                                                        │
  template.render(obj)              ← calls compiled TriConsumer directly        │
                                                                                 │
        └─── getRuntimeTemplate() ───────────────────────────────────────────────┘
        │
        ▼
  AstRender tree interpreted via reflection (RuntimeContext)
  No compilation, no cache — each render walks the AST directly
```

Field resolution honours `@JsonProperty` and `@JsonAlias` annotations as alternate names. `@Nullable` / `@Nonnull` control null-check code generation.

---

## Template Syntax

### Delimiters

| Syntax | Meaning |
|---|---|
| `${ expr }` | Expression block |
| `{{ expr }}` | Alternate expression block (identical semantics) |
| `$${ expr }` | Escape — renders the literal text `${expr}` without evaluation |
| `{{- expr }}` | Expression block; trim trailing whitespace from preceding text |
| `{{ expr -}}` | Expression block; trim leading whitespace from following text |
| `{{- expr -}}` | Expression block; trim whitespace on both sides |
| `{{%- if … }}` | Block-if; trim trailing whitespace from preceding text |

Everything outside a delimiter is emitted verbatim.

```
"Hello, {{ name }}!"         → "Hello, world!"
"price: ${ price }"         → "price: 9.99"
"literal: $${field}"        → "literal: ${field}"
```

### Whitespace trimming

By default all text outside delimiters is emitted verbatim, including spaces and newlines. The trim markers let you collapse surrounding whitespace without changing the template source layout.

- `{{-` — strips all **trailing** whitespace (spaces, tabs, newlines) from the text immediately before the expression.
- `-}}` — strips all **leading** whitespace from the text immediately after the expression.
- Both markers can be combined on the same expression.
- `{{%-` — same left-trim behaviour, but on a block-`if` condition line.
- If trimming reduces an adjacent text segment to an empty string, it is dropped entirely.

```
aa {{- field1 -}} 
bb
```

With `field1 = "test"` this renders as `aatestbb`: the space before `{{-` is stripped from `"aa "`, and the space + newline after `-}}` is stripped from `" \nbb"`.

```
{{- field1 }}   → trims "aa " to "aa",  result: "aatest"
{{ field1 -}}   → trims " bb" to "bb",  result: "test bb" → "testbb"
{{- field1 -}}  → trims both sides
```

Block-if left-trim strips the newline that would otherwise precede the `{{% if … }}` line:

```
line1
{{%- if flag }}content{{% end }}
```

Renders as `line1content` when `flag` is `true` (the `\n` after `line1` is stripped).

### Field access

```
{{ field }}                  simple field
{{ child.field }}            chained field access
{{ mapField.key }}           map key lookup (when field type is Map<String, V>)
{{ fieldM() }}               no-argument method call
{{ fieldMInt(1) }}           method call with int argument
{{ fieldMDouble(1.2) }}      method call with double argument
{{ fieldMString('str') }}    method call with String argument
```

`@JsonProperty` and `@JsonAlias` names are transparently resolved alongside the Java field name.

### Null safety

- `Optional<T>` fields are automatically unwrapped; renders empty string when empty.
- `@Nullable` fields/methods get a null check inserted; renders empty string when null.
- `@Nonnull` fields get no null check generated.
- A null value anywhere in a chained path short-circuits the whole expression and renders the default value, or empty string if none is specified.

```
{{ child.fieldNullable ?? 'N/A' }}   → "N/A" when child is null or fieldNullable is null
{{ fieldOpt }}                       → "" when Optional is empty
```

### Default values (`??`)

`{{ expr ?? default }}` — if `expr` resolves to null (or empty Optional), the default is used.

Supported literal types:

| Literal | Example |
|---|---|
| String | `?? 'text'` or `?? "text"` |
| Integer / Long | `?? -1`, `?? 42` |
| Float / Double | `?? 0.0` |
| Boolean | `?? true`, `?? false` |
| Empty list | `?? []` |

```
{{ name ?? 'anonymous' }}
{{ score ?? -1 }}
{{ ratio ?? 0.0 }}
{{ active ?? false }}
{{ tags ?? [] }}
```

### Fallback chains (`| default`)

`{{ expr1 | default expr2 | default expr3 }}` — evaluates each expression in order and uses the first non-null, non-empty result. All expressions must resolve to the same type.

```
{{ primaryUrl | default fallbackUrl }}
{{ list | default list2 }}
```

### Concatenation

Concatenation combines multiple fields and string literals into a single output without a separator.

Root concatenation (the whole expression is a concat):

```
${ {field1, "/", field2} }
```

Suffix concatenation after a path:

```
{{ child{field1, "x", field2} }}
{{ child.{field1, "x", field2} }}
```

Items inside `{}` can be: field names, double-quoted strings, single-quoted strings, decimal integers, floats.

```
{{ {scheme, "://", host, "/", path} }}   → "https://example.com/api"
```

### Math

```
{{ numericField + 12.45 }}
{{ intField * 2 }}
{{ price - discount }}
{{ total / count }}
{{ value % 100 }}
```

Operators: `+`, `-`, `*`, `/`, `%`. The right-hand operand must be a numeric literal. The result type is widened as needed.

```
{{ score + 100 }}     → score value + 100
{{ price * 1.1 }}     → price * 1.1
```

### If / then / else (inline)

```
{{ if booleanField then field end }}
{{ if booleanField then field else field2 end }}
```

The condition can be any field path or a compound boolean expression (see below). Truthiness is determined by the field's type — see [Truthiness semantics](#truthiness-semantics).

Can be combined with a default value:

```
{{ if isPremium then premiumField end ?? 'standard' }}
```

#### Compound conditions (inline)

The same `and`, `or`, `not` / `!`, and parenthesised grouping supported by block-if are available in inline if. Operator precedence: `not` / `!` → `and` → `or`.

```
{{ if active and not user.isBanned then user.name end }}
{{ if flagA or flagB then field else field2 end }}
{{ if !booleanField then fallback end }}
{{ if (a and b) or c then field end }}
```

Field types follow the same truthiness rules as block-if — a `String` is truthy when non-empty, a `Collection` or `Map` when non-empty, an array when `length > 0`, any other non-null value is truthy, and `null` is always false.

### If / else / end (block)

Block-level conditionals span multiple lines and can contain arbitrary template content (plain text and expression blocks) in each branch.

```
{{% if booleanField }}
  rendered when true: {{ field1 }}
{{% end }}
```

With an else branch:

```
{{% if booleanField }}
  rendered when true: {{ field1 }}
{{% else }}
  rendered when false: {{ field2 }}
{{% end }}
```

**Rules:**

- The condition is a field path (e.g. `booleanField`, `child.active`) or a compound boolean expression (see below). Any field type is allowed; truthiness is determined by the field's type (see [Truthiness semantics](#truthiness-semantics)).
- Each branch is a full template body — any mix of literal text and `{{ expr }}` / `${ expr }` expression blocks.
- The `{{% else }}` branch is optional.
- Blocks can be nested inside each other's branches.
- Whitespace and newlines inside branches are emitted verbatim.

```
{{% if user.isPremium }}
Price: {{ premiumPrice }}
{{% else }}
Price: {{ standardPrice }}
{{% end }}
```

Nested example:

```
{{% if active }}
  {{% if user.isPremium }}
    Welcome back, premium user {{ user.name }}!
  {{% else }}
    Welcome back, {{ user.name }}.
  {{% end }}
{{% end }}
```

#### Compound conditions

Block-if conditions support `and`, `or`, `not` / `!` operators and parenthesised grouping. Operator precedence (highest to lowest): `not` / `!` → `and` → `or`.

| Operator | Syntax | Example |
|---|---|---|
| AND | `and` | `{{% if active and user.isPremium }}` |
| OR  | `or`  | `{{% if flagA or flagB }}` |
| NOT | `not` | `{{% if not booleanField }}` |
| NOT | `!`   | `{{% if !booleanField }}` |
| Grouping | `( … )` | `{{% if (a and b) or c }}` |

```
{{% if active and not user.isBanned }}
  Welcome, {{ user.name }}!
{{% end }}

{{% if (flagA and flagB) or flagC }}
  condition met
{{% end }}
```

Operators can be applied to any field type — truthiness is evaluated per type before combining.

#### Truthiness semantics

Any field type may appear in a `{{% if … }}` condition. The field value is coerced to a boolean as follows:

| Field type | Truthy when |
|---|---|
| `boolean` | value is `true` |
| `Boolean` | non-null and `true` |
| `String` | non-null and non-empty |
| `Collection<?>` | non-null and non-empty |
| `Map<?, ?>` | non-null and non-empty |
| array | non-null and `length > 0` |
| any other type | non-null |
| `null` | always `false` |

```
{{% if name }}Hello, {{ name }}!{{% end %}}
{{% if tags }}Tags: {{ tags }}{{% end %}}
{{% if name and tags }}{{ name }} has tags{{% end %}}
```

### With scope (inline)

`{{ with (scopePath) bodyExpr end }}` — evaluates `bodyExpr` relative to the object resolved by `scopePath`. At compile time the scope path is prepended to each body expression, so this is purely syntactic sugar for chained field access.

```
{{ with (child) field end }}
```
is equivalent to `{{ child.field }}`.

If `scopePath` resolves to null, the body expression renders its default value, or empty string if no default is set.

**With a default:**

```
{{ with (child) field ?? 'n/a' end }}
```

Renders `n/a` when `child` is null or `child.field` is null.

**Fallback chain in body:**

```
{{ with (child) field | default field2 end }}
```

Both alternatives are evaluated against `child` (expanded to `child.field | default child.field2`); the first non-null result is used.

**Root scope (`$`):** prefix any body expression with `$.` to resolve it from the root object instead of the `with` scope:

```
{{ with (child) field | default $.rootField end }}
```

When `child.field` is null, `$.rootField` is resolved from the root object.

### With scope (block)

`{{% with scopePath }} … {{% end %}}` — all `{{ expr }}` blocks inside the body are resolved relative to the object at `scopePath`.

```
{{% with child }}
{{ field }}-{{ field2 }}
{{% end }}
```

**Null scope behaviour:** when `scopePath` resolves to null, literal text in the body is still emitted. Field expressions render their default value, or empty string if no default is set:

```
{{% with child }}A{{ field ?? 'none' }}B{{% end }}
```

Renders `AnoneB` when `child` is null. Renders `A` + field value + `B` when `child` is non-null.

**Root scope (`$`):** prefix any inner expression with `$.` to resolve it from the root object:

```
{{% with child }}{{ $.rootField }} / {{ field }}{{% end }}
```

**Rules:**

- `scopePath` is a field path (e.g. `child`, `a.b`). All `{{ expr }}` expressions inside the body are resolved against the type at that path.
- `$.fieldName` inside the body always resolves from the original root object regardless of nesting depth.
- Use `??` for literal defaults inside body expressions (`{{ field ?? 'default' }}`). Or-chain fallbacks between two fields (`{{ field | default field2 }}`) are not supported inside block-with bodies.
- Blocks may be nested inside other block constructs (`{{% if … }}`, other `{{% with … }}`).
- The `{{% end }}` tag closes the nearest open block.

### Pipe-to-function

`{{ field ; funcName() }}` — the field value is passed as the first argument to the named function. Additional arguments follow inside the parentheses.

```
{{ url ; urlencode() }}
{{ url ; urlencode(2) }}
{{ name ; toUpperCase() }}
{{ dt ; format('yyyy-MM-dd') }}
{{ obj ; toJson() }}
```

### Cast types

`${ <java.lang.Double>field ?? 0.0 }` — forces the expression result to be interpreted as the given type. Useful when the field is typed as `Object` (e.g., in `Map<String, Object>`) but the actual runtime type is known.

```
${ <java.lang.Double>v.d ?? 0.0 }
${ <java.lang.String>v.s ?? '' }
```

A `ClassCastException` at render time is wrapped in `TemplateException`.

### Block comments

`{{ /* comment */ field }}` — a `/* ... */` block at the start of an expression is stripped; the field after it is still evaluated. Useful for annotating complex templates.

```
{{ /* impression URL macro */ url ; urlencode(2) }}
```

---

## Built-in Functions

Registered automatically from `META-INF/oap-template-macros.list` on the classpath.

| Function | Signature | Description |
|---|---|---|
| `urlencode()` | `(String src)` | URL-encodes the value once (space → `+`) |
| `urlencode(N)` | `(String src, long depth)` | URL-encodes N times; depth 0 is a no-op |
| `urlencodePercent()` | `(String src)` | URL-encodes, replacing `+` with `%20` |
| `urlencodePercent(N)` | `(String src, long depth)` | URL-encodes N times using `%20` |
| `toUpperCase()` | `(String src)` | Converts to upper case; null-safe |
| `toLowerCase()` | `(String src)` | Converts to lower case; null-safe |
| `format(pattern)` | `(DateTime dt, String pattern)` | Formats a Joda `DateTime`; predefined patterns: `SIMPLE`, `MILLIS`, `SIMPLE_CLEAN`, `DATE` |
| `toJson()` | `(Object obj)` | Serialises the value to JSON |
| `default(fallback)` | `(Object in, Object fallback)` | Returns fallback if in is null or empty |

---

## Custom Functions

1. Create a class with `public static` methods. The first parameter is always the piped value; additional parameters are supplied in the template call.

```java
public class MyMacros {
    public static String prefix( String src, String p ) {
        return src == null ? null : p + src;
    }

    @JsonAlias( "pfx" )   // registers an alias "pfx" for the same method
    public static String prefixAlias( String src, String p ) {
        return prefix( src, p );
    }
}
```

2. Register the class by adding its fully-qualified name to a resource file:

`src/main/resources/META-INF/oap-template-macros.list`:
```
com.example.MyMacros
```

Multiple entries are allowed, one per line. The engine scans all files with this path on the classpath at startup.

Usage in templates:
```
{{ name ; prefix('Mr. ') }}
{{ name ; pfx('Dr. ') }}
```

---

## Output Accumulators

The engine is output-agnostic via `TemplateAccumulator<TOut, TOutMutable, Self>`.

### Built-in accumulators

**`TemplateAccumulatorString`** (available as `TemplateAccumulators.STRING`)

Appends everything into a `StringBuilder`. Default delimiter between multiple expressions is `\t`. Default `DateTime` format is `SIMPLE_CLEAN` (configurable via constructor). Lists are rendered as `[item1,item2]` with single-quoted strings and escaped backslashes/apostrophes. Null collections render as `[]`.

```java
// custom DateTime format
TemplateAccumulatorString acc = new TemplateAccumulatorString( "yyyy-MM-dd HH:mm:ss" );
```

**`TemplateAccumulatorObject`** (available as `TemplateAccumulators.OBJECT`)

Holds the last value as a raw `Object`. Useful for single-expression templates where you want the Java object rather than its string representation.

```java
Long score = (Long) engine.getTemplate( "score", new TypeRef<MyBean>() {}, "{{ score }}", OBJECT, null )
    .render( bean ).get();
```

### Custom accumulators

Implement `TemplateAccumulator<TOut, TOutMutable, Self>` and override the `accept(...)` overloads for the types you want to customise:

```java
public class TsvAccumulator implements TemplateAccumulator<String, StringBuilder, TsvAccumulator> {
    private final StringBuilder sb;

    public TsvAccumulator() { this( new StringBuilder() ); }
    public TsvAccumulator( StringBuilder sb ) { this.sb = sb; }

    @Override public void acceptText( String text ) { sb.append( text ); }
    @Override public void accept( String text ) { if ( text != null ) sb.append( text ); }
    @Override public void accept( int i ) { sb.append( i ); }
    @Override public void accept( long l ) { sb.append( l ); }
    // ... implement all remaining accept() overloads ...

    @Override public String get() { return sb.toString(); }
    @Override public TsvAccumulator newInstance() { return new TsvAccumulator(); }
    @Override public TsvAccumulator newInstance( StringBuilder m ) { return new TsvAccumulator( m ); }
    @Override public String getTypeName() { return "String"; }
    @Override public String delimiter() { return "\t"; }
    // ...
}
```

---

## Using the Engine in Java

```java
// One engine instance per application (or inject via OAP module)
TemplateEngine engine = new TemplateEngine( Dates.d( 10 ) );  // 10-day in-memory TTL

// With disk cache (survives JVM restarts)
TemplateEngine engine = new TemplateEngine( Path.of( "/tmp/template" ), Dates.d( 30 ) );
```

**Compile and cache a template:**

```java
Template<MyBean, String, StringBuilder, TemplateAccumulatorString> template =
    engine.getTemplate(
        "myTemplate",                      // logical name (used in disk-cache file naming)
        new TypeRef<MyBean>() {},          // input type
        "id={{ id }}, name={{ name }}",    // template string
        TemplateAccumulators.STRING,       // output accumulator
        ErrorStrategy.ERROR               // throw on unknown fields
    );
```

**Render:**

```java
String result = template.render( bean ).get();               // no trailing newline
String result = template.render( bean, true ).get();         // append \n
template.render( bean, false, existingStringBuilder );       // reuse a buffer
```

**`getTemplate` overloads** (all variants follow `name, type, template, acc, [aliases,] [errorStrategy,] [postProcess]`):

```java
// With aliases
engine.getTemplate( name, type, tmpl, acc, Map.of( "old.field", "new.field" ), ERROR );

// With postProcess hook (inspect/modify the AST after parsing)
engine.getTemplate( name, type, tmpl, acc, ast -> log.debug( ast.print() ) );
```

---

## Runtime Interpreter Mode

By default `getTemplate` generates a Java class, compiles it in memory, and caches it — the compiled code runs at near-native speed on subsequent calls. An alternative path, `getRuntimeTemplate`, interprets the same AST directly via reflection without any code generation or compilation.

**When to use:**

- Environments where dynamic classloading or bytecode generation is restricted (e.g. OSGi containers, some native-image builds)
- Short-lived or one-shot templates where paying the compile cost once is not worth it
- Unit tests that need to verify template logic without the JIT warm-up noise

**API** — mirrors `getTemplate` exactly; replace the method name:

```java
// Compile-time path (default):
Template<MyBean, String, StringBuilder, TemplateAccumulatorString> template =
    engine.getTemplate( "myTemplate", new TypeRef<MyBean>() {}, "id={{ id }}", STRING, null );

// Runtime interpreter path:
Template<MyBean, String, StringBuilder, TemplateAccumulatorString> template =
    engine.getRuntimeTemplate( "myTemplate", new TypeRef<MyBean>() {}, "id={{ id }}", STRING );
```

All overloads accepting `aliases`, `ErrorStrategy`, and `postProcess` are available on both methods.

**Rendering** is identical — call `template.render(obj)` the same way:

```java
String result = template.render( bean ).get();
```

**Limitations compared to `getTemplate`:**

- No disk cache — the AST is always re-walked on each `render` call (though the AST itself is built once and can be cached).
- No compile-time Micrometer metrics.
- Throughput is lower for high-frequency rendering; prefer `getTemplate` in production hot paths.

---

## OAP Module Integration

The module registers a pre-wired service `oap-template-engine`:

```hocon
name = oap-template
services {
  oap-template-engine {
    implementation = oap.template.TemplateEngine
    parameters {
      ttl = 30d
//    diskCache = /tmp/template
    }
    supervision {
      schedule = true
      cron = "10 20 */2 * * ? *"   # clean caches every 2 hours
    }
  }
}
```

Parameters:

| Parameter | Default | Description |
|---|---|---|
| `ttl` | `30d` | Cache entry TTL in OAP duration format (`30d`, `12h`, etc.). Entries expire after this period of inactivity. |
| `diskCache` | _(disabled)_ | Path to a directory for persisting compiled `.java`/`.class` files across JVM restarts. Remove the comment to enable. |

Wire it into a dependent module:

```hocon
services {
  my-service {
    implementation = com.example.MyService
    parameters {
      templateEngine = <modules.oap-template.oap-template-engine>
    }
  }
}
```

---

## Aliases

The `aliases` parameter in `getTemplate` is a `Map<String, String>` that remaps template expression strings before parsing. This allows a configuration layer to redirect fields without changing the template syntax.

```java
// "{{ child.field }}" is evaluated as "{{ child2.field2 }}"
engine.getTemplate( name, type, "{{ child.field }}", acc,
    Map.of( "child.field", "child2.field2" ), ERROR );
```

---

## Error Handling

| Strategy | Behaviour |
|---|---|
| `ErrorStrategy.ERROR` (default) | Unknown field path or function → throws `TemplateException` at `getTemplate()` time (compile phase, not render time) |
| `ErrorStrategy.IGNORE` | Unknown paths emit an empty string silently; useful when the data model may change |

Syntax errors (malformed expression) always throw `TemplateException` regardless of error strategy.

`TemplateException` is an unchecked exception. It wraps underlying causes (`NoSuchFieldException`, `NoSuchMethodException`, `ClassCastException` from cast failures).

---

## Disk Cache

When `diskCache` is configured, compiled classes are written as `<name>_<murmur3hash>.class` and `.java` files in the specified directory.

- On startup, if a matching `.class` file exists and can be loaded, it is reused without recompilation.
- If the class file is incompatible with the current accumulator (detected by comparing class names in the bytecode), the engine recompiles automatically.
- The supervised `run()` method (invoked every two hours by default) deletes any file older than `ttl`.

```java
// Enable disk cache programmatically
TemplateEngine engine = new TemplateEngine( Path.of( "/var/cache/templates" ), Dates.d( 30 ) );
```
