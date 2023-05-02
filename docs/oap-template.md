OAP-template Documentation
===============================

# Contents

* [Motivation](#motivation)
* [Syntax](#syntax)

## Motivation
<p>The main application is in systems with a high load, where the speed of template rendering is important. The framework converts the template to pure Java code without using reflection and compiles to bytecode.</p>

## Syntax
* [Main](#main)
* [Expression](#expression)
* [Comment](#comment)
* [Nullable and Optional](#nullable-and-optional)
* [Built-in functions](#built-in-functions)

### Main
`(` *expression* `|` *comment* `|` *text* `){1..N}`
  
* **text** → any character set except *expr*
* **expression** → [expression](#expresssion)

### Expresssion

`${` `(`*expression_comment*`){0..1}` `(`*result_type*`){0..1}` *exprs* `(`*defauult_type*`){0..1}` `(`*function*`){0..1}` `}`

* **expression_comment** → _/*_ &lt;ANY> _*/_<br>
* **default_type** → *??* *value*<br>
  * **value** → *STRING* `|` *LONG* `|` *FLOAT* `|` *BOOLEAN* | *[]*
    * **STRING** → `"` *&lt;TEXT>* `"` `|` `'` *&lt;TEXT>* `'`
* **function** → *;* *function_name* *(* function_args *)*<br>
  * **function_args**  → `(` *STRING* `|` *LONG* `|` *FLOAT* `|` *BOOLEAN* `)` `|` (` *function_args* `(` *,* *function_args* `){0..N} )`
    * **STRING** → `"` *&lt;TEXT>* `"` `|` `'` *&lt;TEXT>* `'`
* **exprs** → *expr* `(` *|* &nbsp;*expr*`){0..N}`* <br>
  * **expr**  → `[` *path* `|` *concatenation* `]` `(` *math* `){0..1}`
    * **path**  →  `[` *field* `|` *property* `|` *function* `]` `(` *.* *path* `){0..N}`
    * **concatenation** → *\[* *items* *]*
      * **items** → `[` *STRING* `|` *LONG* `|` *FLOAT* `|` *BOOLEAN* `|` *field_or_property* `]` `(` *,* *items* `){0..N}`
    * **math**  → *operation* *number*
      * **operation**  → _*_ `|` */* `|` *+* `|` *-*
      * **number** → *LONG* `|` *FLOAT*


### Comment

### Nullable and Optional
* `Optional<?>` fields cannot be null.
* By default, all non-primitive types at the end of a path are treated as possibly nullable, except for `Optional<?>` and the direct specification of the `@javax.annotation.Nonnull` annotation.
* All types at the beginning of the path are treated as non-nullable, except `Optional<?>` or `@javax.annotation.Nullable` annotated.

### Built-in functions
* urlencode(depth) → depth=3 → urlencode(urlencode(urlencode(data)))
* urlencode→   [URLEncoder.encode( src, StandardCharsets.UTF_8.name() )](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/net/URLEncoder.html#encode(java.lang.String,java.nio.charset.Charset))
* urlencodePercent → urlencode and `+` replaced by `%20`
* toUpperCase → [String#toUpperCase](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/lang/String.html#toUpperCase())
* toLowerCase → [String#toLowerCase](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/lang/String.html#toLowerCase())
* format(pattern) → 
  * [string pattern](https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html) or predefinded:
    * `"SIMPLE"` → `"yyyy-MM-dd'T'HH:mm:ss"`
    * `"MILLIS"`→ `"yyyy-MM-dd'T'HH:mm:ss.SSS"`
    * `"SIMPLE_CLEAN"` → `"yyyy-MM-dd HH:mm:ss"`
    *  `"DATE"` → `"yyyy-MM-dd"`
