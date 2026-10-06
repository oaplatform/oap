package oap.ws.validate;

import oap.util.Pair;

import java.util.List;

import static oap.util.Pair.__;

/**
 * Adds validation messages to a {@link ValidationErrors} under one HTTP status code.
 * <p>
 * Obtain it with {@link ValidationErrors#statusCode(int)} and finish with {@link #endCode()}, which returns the
 * {@link ValidationErrors} it was created from, so several status codes can be chained.
 * <p>
 * Each message may carry an optional message code (the {@code code} of {@code ErrorResponse.Message}).
 * Pass {@code null} for no message code.
 */
public final class ValidationErrorsBuilder {
    private final ValidationErrors parent;
    private final int httpStatusCode;

    ValidationErrorsBuilder( ValidationErrors parent, int httpStatusCode ) {
        this.parent = parent;
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Adds one message with no message code.
     */
    public ValidationErrorsBuilder error( String message ) {
        return pairs( List.of( __( null, message ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with no message code.
     */
    public ValidationErrorsBuilder error( String message, Object... args ) {
        return pairs( List.of( __( null, message.formatted( args ) ) ) );
    }

    /**
     * Adds one message with the given message code.
     */
    public ValidationErrorsBuilder error( int code, String message ) {
        return pairs( List.of( __( code, message ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with the given message code.
     */
    public ValidationErrorsBuilder error( int code, String message, Object... args ) {
        return error( code, message.formatted( args ) );
    }

    /**
     * Adds messages with no message code.
     */
    public ValidationErrorsBuilder errors( List<String> messages ) {
        return pairs( messages.stream().map( message -> __( ( Integer ) null, message ) ).toList() );
    }

    /**
     * Adds messages that all share the given message code.
     */
    public ValidationErrorsBuilder errors( int code, List<String> messages ) {
        return pairs( messages.stream().map( message -> __( code, message ) ).toList() );
    }

    /**
     * Adds messages, each with its own message code (first element) and text (second element).
     */
    public ValidationErrorsBuilder pairs( List<Pair<Integer, String>> messages ) {
        parent.add( httpStatusCode, messages );
        return this;
    }

    /**
     * Returns the {@link ValidationErrors} this builder was created from.
     */
    public ValidationErrors endCode() {
        return parent;
    }
}
