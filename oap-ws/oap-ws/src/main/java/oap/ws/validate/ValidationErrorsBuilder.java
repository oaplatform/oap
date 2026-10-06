package oap.ws.validate;

import oap.reflect.TypeRef;
import oap.template.ErrorStrategy;
import oap.template.TemplateAccumulators;
import oap.template.TemplateEngine;
import oap.util.Dates;
import oap.util.Pair;

import java.util.List;
import java.util.Map;

import static oap.util.Pair.__;

/**
 * Adds validation messages to a {@link ValidationErrors} under one HTTP status code.
 * <p>
 * Obtain it with {@link ValidationErrors#statusCode(int)} and finish with {@link #endCode()}, which returns the
 * {@link ValidationErrors} it was created from, so several status codes can be chained.
 * <p>
 * Each message may carry an optional message code (the {@code code} of {@code ErrorResponse.Message}).
 * Messages without a code use the variants without an {@code int code} parameter.
 */
public final class ValidationErrorsBuilder {
    private static final TemplateEngine TEMPLATE_ENGINE = new TemplateEngine( null, Dates.m( 10 ) );

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
     * Adds a message formatted with {@code args}, with no message code. Placeholders are {@code ${name}}, resolved from {@code args} by the template engine.
     */
    public ValidationErrorsBuilder error( String message, Map<String, Object> args ) {
        String s = TEMPLATE_ENGINE.getRuntimeTemplate( message, new TypeRef<Map<String, Object>>() {}, message, TemplateAccumulators.STRING, ErrorStrategy.ERROR, null, null ).render( args ).get();
        return pairs( List.of( __( null, s ) ) );
    }

    /**
     * Adds one message with the given message code.
     */
    public ValidationErrorsBuilder error( int code, String message ) {
        return pairs( List.of( __( code, message ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with the given message code. Placeholders are {@code ${name}}, resolved from {@code args}.
     */
    public ValidationErrorsBuilder error( int code, String message, Map<String, Object> args ) {
        String s = TEMPLATE_ENGINE.getRuntimeTemplate( message, new TypeRef<Map<String, Object>>() {}, message, TemplateAccumulators.STRING, ErrorStrategy.ERROR, null, null ).render( args ).get();
        return error( code, s );
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
