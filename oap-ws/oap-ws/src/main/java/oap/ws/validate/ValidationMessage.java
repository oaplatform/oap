package oap.ws.validate;

/**
 * A validation message defined once, typically as an enum constant: its message code and text.
 * Pass it to {@link ValidationErrorsBuilder#error(ValidationMessage)}. The OpenAPI generator reads the code and text
 * from the constant's constructor arguments, so they must be literals.
 */
public interface ValidationMessage {
    /** The message code; above {@link ValidationErrorsBuilder#MAX_INTERNAL_CODE} for public use. */
    int code();

    String message();
}
