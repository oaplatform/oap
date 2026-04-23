# oap-dictionary-maven

Maven plugin goal that generates a Java `enum` from each dictionary file in `sourceDirectory`. Run during `generate-sources` so the generated sources are visible to the compiler.

## Goal: `oap:generate`

For each `.json` or `.conf` dictionary file found in `sourceDirectory`, the goal emits one (or more, when `$children` is configured) Java enum that implements `oap.dictionary.Dictionary`.

### Parameters

| Parameter | Default | Description |
|---|---|---|
| `sourceDirectory` | `src/main/resources/dictionary` | Directory scanned for `*.json` and `*.conf` dictionary files |
| `sourceDirectoryExts` | `[]` | Additional directories to resolve cross-file dictionary references |
| `outputDirectory` | `target/generated-sources/dictionary` | Root of generated Java sources |
| `dictionaryPackage` | `dictionary` | Java package name for all generated enums |
| `excludes` | `[]` | Ant-style glob patterns for files to skip |

### Dictionary file format

```json
{
  "name": "payment-method",
  "values": [
    { "id": "CARD",   "eid": 1, "enabled": true, "label": "Credit Card" },
    { "id": "WIRE",   "eid": 2, "enabled": true, "label": "Wire Transfer" },
    { "id": "UNKNOWN","eid": 0, "enabled": false }
  ]
}
```

| Field | Description |
|---|---|
| `name` | Dictionary name — also used as the generated class name (kebab-case converted to PascalCase) |
| `values[].id` | Enum constant name |
| `values[].eid` | External integer ID; used in `valueOf(int)` |
| `values[].enabled` | Exposed as `isEnabled()` |
| Additional fields | Become typed fields and accessors on the enum (`String label()`, `int score()`, etc.) |

When a value with `id = "UNKNOWN"` is present, `valueOf(unknownId)` returns it instead of throwing `IllegalArgumentException`.

### `$generator` metadata

Add a `$generator` block to control code generation:

```json
{
  "name": "geo",
  "$generator": {
    "$name": "GeoRegion",
    "$externalIdAs": "character",
    "$children": [
      { "$level": 1, "$name": "GeoCountry" },
      { "$level": 2, "$name": "GeoCity",   "$externalIdAs": "integer" }
    ]
  },
  "values": [ ... ]
}
```

| Key | Default | Description |
|---|---|---|
| `$name` | Dictionary `name` (PascalCase) | Override generated class name |
| `$externalIdAs` | `integer` | `integer` → `int externalId`; `character` → `char externalId` (maps `eid` as char) |
| `$children` | `[]` | Generate additional enums from nested levels of the hierarchy |
| `$children[].$level` | — | Which nesting depth to flatten into the extra enum (1 = direct children, 2 = grandchildren, …) |
| `$children[].$name` | — | Class name for the child enum |
| `$children[].$externalIdAs` | `integer` | External ID type for the child enum |

### Generated enum shape

```java
public enum PaymentMethod implements Dictionary {
    CARD(1, true, "Credit Card"),
    WIRE(2, true, "Wire Transfer"),
    UNKNOWN(0, false, null);

    private final int    externalId;
    private final boolean enabled;
    private final String label;

    public String label() { return label; }

    public static PaymentMethod valueOf( int externalId ) { ... }

    // Dictionary interface methods ...
    public int  externalId()    { return externalId; }
    public boolean isEnabled()  { return enabled; }
}
```

### `pom.xml` wiring

```xml
<plugin>
    <groupId>oap</groupId>
    <artifactId>oap-dictionary-maven</artifactId>
    <version>${oap.version}</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
    <configuration>
        <dictionaryPackage>com.example.dict</dictionaryPackage>
        <!-- sourceDirectory and outputDirectory use their defaults -->
        <excludes>
            <exclude>**/internal-*.json</exclude>
        </excludes>
    </configuration>
</plugin>
```
