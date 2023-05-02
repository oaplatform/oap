OAP-template Documentation
===============================

# Contents

* [Motivation](#motivation)
* [Syntax](#syntax)

## Motivation

## Syntax
* [Main](#main)
* [Expression](#expression)
* [Comment](#comment)

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
