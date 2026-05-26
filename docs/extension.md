# Ext — Pluggable Field Extension Mechanism

`oap.json.ext.Ext` is a marker base class that lets you attach pluggable typed sub-objects to a Java bean field without knowing the concrete implementation at compile time. The concrete class is resolved at runtime from `oap-module.oap` configuration entries and is used by both the JSON deserializer and the template engine.

## Table of Contents

- [Overview](#overview)
- [How it works](#how-it-works)
- [Declaring an Ext field](#declaring-an-ext-field)
  - [Direct Ext field](#direct-ext-field)
  - [Abstract-interface Ext field](#abstract-interface-ext-field)
- [Registering an extension](#registering-an-extension)
- [JSON deserialization](#json-deserialization)
- [Template engine integration](#template-engine-integration)
  - [Basic field access](#basic-field-access)
  - [Null safety and defaults](#null-safety-and-defaults)
  - [Concatenation inside an Ext scope](#concatenation-inside-an-ext-scope)
  - [Inline if condition](#inline-if-condition)
  - [Block if condition](#block-if-condition)
  - [Block with scope](#block-with-scope)
- [Runtime instantiation via `Ext` helpers](#runtime-instantiation-via-ext-helpers)
- [Multiple implementations](#multiple-implementations)
- [Full working example](#full-working-example)

---

## Overview

`Ext` solves the problem of embedding vendor- or plugin-specific data into a shared data model without polluting the model with concrete dependencies. The data model declares a field of type `Ext` (or a named abstract sub-class). Each deployment registers the concrete implementation class in its own `oap-module.oap`.

---

## How it works

1. Your bean declares a field of type `Ext` or of a named abstract sub-class.
2. The concrete implementation class is registered in `oap-module.oap` under a `configurations` block using the `oap.json.ext.ExtDeserializer` loader.
3. At startup `ExtDeserializer` scans all `META-INF/oap-module.oap` files on the classpath and builds an in-memory map of `(ownerClass, fieldName) → concreteClass`.
4. The JSON deserializer uses this map to instantiate the right class when reading JSON.
5. The template engine calls `ExtDeserializer.extensionOf()` at template-compile time to resolve the concrete class, so generated code uses the correct field types and casts.

---

## Declaring an Ext field

### Direct Ext field

The simplest case: the field is typed as the base `Ext` class. The concrete implementation is entirely opaque to the declaring class.

```java
import oap.json.ext.Ext;
import javax.annotation.Nullable;

public class BidRequest {
    public String id;
    @Nullable
    public Ext ext;   // concrete type registered in oap-module.oap
}
```

### Abstract-interface Ext field

If different modules share a common contract for the extension, declare an inner abstract class that extends `Ext`. The field is typed as this abstract class. Each module registers its own concrete implementation.

```java
import oap.json.ext.Ext;

public class BidRequest {
    public String id;
    public IExt ext;

    public static class IExt extends Ext {
        // common API methods / shared fields go here
        public String custom;
    }
}
```

The concrete implementation:

```java
public class BidRequestExt extends BidRequest.IExt {
    public String buyerUid;
    public int segmentId;
}
```

---

## Registering an extension

In any `META-INF/oap-module.oap` file on the classpath:

```hocon
name = my-module

configurations = [
  {
    loader = oap.json.ext.ExtDeserializer
    config = [
      # Direct Ext field — no abstract declared
      {
        class          = com.example.BidRequest
        field          = ext
        implementation = com.example.BidRequestExt
      }

      # Abstract-interface field — declared abstract class + concrete implementation
      {
        class          = com.example.BidRequest
        field          = ext
        abstract       = com.example.BidRequest.IExt
        implementation = com.example.BidRequestExt
      }
    ]
  }
]
```

| Property | Required | Description |
|---|---|---|
| `class` | yes | Fully-qualified owner class |
| `field` | yes | Field name on the owner class |
| `implementation` | yes | Fully-qualified concrete class to instantiate |
| `abstract` | no | Fully-qualified abstract/interface class; also registers a Jackson deserializer for that type so `@JsonDeserialize` works automatically |

Inner class names use dot notation in the config file (`com.example.BidRequest.IExt`); the loader converts the last dot to `$` internally.

---

## JSON deserialization

When Jackson reads a JSON object that contains the registered field, it calls `ExtDeserializer`, which looks up the concrete class and delegates to Jackson's standard deserializer for that type. No annotation is needed on the field itself.

```json
{
  "id": "req-1",
  "ext": {
    "buyerUid": "user-42",
    "segmentId": 7
  }
}
```

```java
ObjectMapper mapper = Binder.json;                // OAP's pre-configured mapper
BidRequest req = mapper.readValue( json, BidRequest.class );
BidRequestExt ext = (BidRequestExt) req.ext;
System.out.println( ext.buyerUid );   // "user-42"
```

When the field uses the abstract-interface pattern (`abstract = …`), the registered `ExtDeserializer` is also wired as a Jackson deserializer for the abstract type, so it works correctly even if the field is declared as the abstract class:

```java
public BidRequest.IExt ext;   // typed as abstract; Jackson still resolves BidRequestExt
```

---

## Template engine integration

Once the extension mapping is registered in `oap-module.oap`, the template engine automatically resolves the concrete class at template-compile time. No special template syntax is required.

All examples below assume:

```java
BidRequest req = new BidRequest();
req.id = "r1";
req.ext = new BidRequestExt();
((BidRequestExt) req.ext).buyerUid = "user-42";
((BidRequestExt) req.ext).segmentId = 7;
```

### Basic field access

```
{{ ext.buyerUid }}
```

→ `"user-42"`

```
{{ ext.segmentId }}
```

→ `"7"`

Chained access through deeper sub-fields works identically:

```
{{ ext.profile.tier }}
```

### Null safety and defaults

The `@Nullable` annotation on the `ext` field is respected. When `ext` is null, the expression renders the default value or an empty string.

```
{{ ext.buyerUid ?? 'unknown' }}
```

→ `"unknown"` when `ext` is null or `buyerUid` is null.

### Concatenation inside an Ext scope

Use the scoped concatenation syntax `{{ scopePath.{ item1 + item2 } }}` to join multiple fields from the Ext object in a single expression:

```
{{ ext.{ buyerUid + '_' + segmentId } }}
```

→ `"user-42_7"`

The `+` items are simple identifiers (and string/numeric literals) resolved in the context of `ext`.

### Inline if condition

```
{{ if ext.buyerUid then ext.buyerUid else 'anon' end }}
```

→ `"user-42"` when `ext` is non-null and `buyerUid` is non-empty, otherwise `"anon"`.

### Block if condition

```
{{% if ext.buyerUid }}
  buyer: {{ ext.buyerUid }}
{{% else %}}
  anonymous
{{% end %}}
```

### Block with scope

Use `{{% with ext %}}` to set the ext object as the body scope so you can access its fields without the `ext.` prefix:

```
{{% with ext }}
{{ buyerUid }}-{{ segmentId }}
{{% end %}}
```

→ `"user-42-7"`

Combining with root-scope access (`$.`):

```
{{% with ext }}
{{ $.id }}: {{ buyerUid }}
{{% end %}}
```

→ `"r1: user-42"` (`$.id` resolves from the root `BidRequest`, `buyerUid` from `BidRequestExt`).

---

## Runtime instantiation via `Ext` helpers

`Ext` provides protected static helpers for creating instances at runtime via the same registry — useful in constructors of the owner class.

```java
public class BidRequest {
    @Nullable
    public Ext ext = Ext.newExt( BidRequest.class, "ext", new Class[0], new Object[0] );
}
```

`Ext.newExt(ownerClass, field, constructorParamTypes, constructorArgs)` looks up the registered concrete class, finds the matching constructor, and returns a new instance. Returns `null` when no implementation is registered.

The constructor lookup is cached per `(ownerClass, field)` pair for zero overhead on subsequent calls.

---

## Multiple implementations

A field can have different implementations in different deployments by placing the `oap-module.oap` entry only in the module that needs it. The `disableOverwrite` flag prevents later entries from replacing an existing registration:

```hocon
{
  class           = com.example.BidRequest
  field           = ext
  implementation  = com.example.BidRequestExt
  disableOverwrite = true
}
```

When `disableOverwrite = true`, any subsequent `configurations` entry for the same `(class, field)` pair is silently ignored.

---

## Full working example

**Data model (`oap-datamodel` module):**

```java
package com.example.model;

import oap.json.ext.Ext;
import javax.annotation.Nullable;

public class BidRequest {
    public String id;
    @Nullable
    public Ext ext;

    // optional: abstract interface variant
    public IExt typedExt;

    public static class IExt extends Ext {
        public String custom;
    }
}
```

**Concrete extension (`oap-mybidder` module):**

```java
package com.example.mybidder;

import com.example.model.BidRequest;

public class MyBidRequestExt extends BidRequest.IExt {
    public String buyerUid;
    public int segmentId;
}
```

**`oap-mybidder/src/main/resources/META-INF/oap-module.oap`:**

```hocon
name = oap-mybidder

configurations = [
  {
    loader = oap.json.ext.ExtDeserializer
    config = [
      {
        class          = com.example.model.BidRequest
        field          = ext
        implementation = com.example.mybidder.MyBidRequestExt
      }
      {
        class          = com.example.model.BidRequest
        field          = typedExt
        abstract       = com.example.model.BidRequest.IExt
        implementation = com.example.mybidder.MyBidRequestExt
      }
    ]
  }
]
```

**Usage:**

```java
// JSON deserialization — concrete class resolved automatically
BidRequest req = Binder.json.readValue( jsonString, BidRequest.class );
MyBidRequestExt ext = (MyBidRequestExt) req.ext;

// Template rendering — concrete class resolved at compile time
TemplateEngine engine = new TemplateEngine( Dates.d( 30 ) );
Template<BidRequest, String, StringBuilder, TemplateAccumulatorString> t =
    engine.getTemplate(
        "bid-log",
        new TypeRef<BidRequest>() {},
        "{{ id }}\t{{ ext.buyerUid ?? '' }}\t{{ ext.segmentId ?? 0 }}",
        TemplateAccumulators.STRING,
        ErrorStrategy.ERROR
    );

String row = t.render( req ).get();
// → "req-1\tuser-42\t7"
```
