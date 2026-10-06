package oap.validation;

/**
 * A validation message defined once, typically as an enum constant: its message code and text.
 * Used by the web-service validation builders. The OpenAPI generator reads the code and text
 * from the constant's constructor arguments, so they must be literals.
 */
public interface ValidationMessage {
    /** The message code. */
    String code();

    String message();
}
