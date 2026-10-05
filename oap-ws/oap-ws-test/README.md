# oap-ws-test

TestNG assertion helpers for OAP web service validation. Provides fluent APIs to assert on HTTP responses (`ValidationAssertion`), on `ValidationErrors` returned from validators (`ValidationErrorsAssertion`), and on validated method invocations (`ValidationErrorsAssertion.validating`).

Depends on: `oap-ws`

## Classes

### `ValidationAssertion`

Entry point for asserting on a JSON error response (`Response`) produced by a web service.

```java
import oap.ws.validate.testng.ValidationAssertion;

ValidationAssertion.assertValidation( response )
    .hasErrors( "field must not be null" );
```

| Method | Description |
|---|---|
| `hasErrors( String... errors )` | Asserts that the response's `errors` contain the given messages |

### `ValidationErrorsAssertion`

Fluent assertions on a `ValidationErrors` instance, via `ValidationErrorsAssertion.assertValidationErrors( errors )`. Assertions use the resolved code and messages (`resolvedCode()`, `resolvedErrors()`).

| Method | Description |
|---|---|
| `hasCode( int code )` | Asserts the resolved status code |
| `containsErrors( String... errors )` | Asserts the resolved messages contain the given messages |
| `isError( int code, String error )` | Asserts the given code and message are present |
| `isFailed()` | Asserts that at least one error is present |
| `isNotFailed()` | Asserts that no errors are present |

`ValidationErrorsAssertion.validating( instance )` runs validation on a proxied instance and returns a `ValidatedInvocation` with the same assertions (`hasCode`, `containsErrors`, `isError`, `isFailed`, `isNotFailed`), finished with `build()`.

## Usage in tests

```java
@Test
public void shouldRejectNullName() {
    ValidationErrors result = myValidator.validate( new Product( null, 9.99 ) );

    ValidationErrorsAssertion.assertValidationErrors( result )
        .isFailed()
        .containsErrors( "name must not be null" );
}

@Test
public void shouldPassForValidProduct() {
    ValidationErrors result = myValidator.validate( new Product( "Widget", 9.99 ) );

    ValidationErrorsAssertion.assertValidationErrors( result )
        .isNotFailed();
}
```
