# oap-ws-test

TestNG assertion helpers for OAP web service validation errors. Provides a fluent API to assert on `ValidationErrors` returned from validators.

Depends on: `oap-ws`

## Classes

### `ValidationAssertion`

Entry point for asserting on a `ValidationErrors` instance.

```java
import oap.ws.validate.testng.ValidationAssertion;

ValidationErrors errors = validator.validate( myObject );

ValidationAssertion.assertValidation( errors )
    .hasErrors()
    .containsError( "field must not be null" );
```

### `ValidationErrorsAssertion`

Fluent assertion chain returned by `ValidationAssertion.assertValidation()`.

| Method | Description |
|---|---|
| `hasErrors()` | Asserts that at least one error is present |
| `hasNoErrors()` | Asserts that the errors list is empty |
| `containsError( String message )` | Asserts that the error list contains the given message |
| `hasErrorCount( int count )` | Asserts the exact number of errors |

## Usage in tests

```java
@Test
public void shouldRejectNullName() {
    ValidationErrors result = myValidator.validate( new Product( null, 9.99 ) );

    ValidationAssertion.assertValidation( result )
        .hasErrors()
        .containsError( "name must not be null" );
}

@Test
public void shouldPassForValidProduct() {
    ValidationErrors result = myValidator.validate( new Product( "Widget", 9.99 ) );

    ValidationAssertion.assertValidation( result )
        .hasNoErrors();
}
```
