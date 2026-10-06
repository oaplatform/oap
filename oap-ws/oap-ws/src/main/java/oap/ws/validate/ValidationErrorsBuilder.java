package oap.ws.validate;

import oap.reflect.TypeRef;
import oap.template.ErrorStrategy;
import oap.template.TemplateAccumulators;
import oap.template.TemplateEngine;
import oap.util.Dates;
import oap.util.Pair;
import oap.validation.ValidationMessage;

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
 * Messages without a code use the variants without a {@code code} parameter.
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
        return add( List.of( __( ( String ) null, message ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with no message code. Placeholders are {@code ${name}}, resolved from {@code args} by the template engine.
     */
    public ValidationErrorsBuilder error( String message, Map<String, Object> args ) {
        return add( List.of( __( ( String ) null, format( message, args ) ) ) );
    }

    /**
     * Adds the message of a {@link ValidationMessage} (typically an enum constant), with its code.
     */
    public ValidationErrorsBuilder error( ValidationMessage message ) {
        return pairs( List.of( message ) );
    }

    /**
     * Adds the message of a {@link ValidationMessage} formatted with {@code args}, with its code.
     * Placeholders are {@code ${name}}, resolved from {@code args}.
     */
    public ValidationErrorsBuilder error( ValidationMessage message, Map<String, Object> args ) {
        return pairs( List.of( new CodedMessage( message.code(), format( message.message(), args ) ) ) );
    }

    /**
     * Adds messages with no message code.
     */
    public ValidationErrorsBuilder errors( List<String> messages ) {
        return add( messages.stream().map( message -> __( ( String ) null, message ) ).toList() );
    }

    /**
     * Adds messages that all share the given message code.
     */
    public ValidationErrorsBuilder errors( String code, List<String> messages ) {
        return pairs( messages.stream().<ValidationMessage>map( message -> new CodedMessage( code, message ) ).toList() );
    }

    /**
     * Adds messages (enum constants or other {@link ValidationMessage}s), each with its code and text.
     */
    public ValidationErrorsBuilder pairs( List<ValidationMessage> messages ) {
        return add( messages.stream().map( message -> __( message.code(), message.message() ) ).toList() );
    }

    private ValidationErrorsBuilder add( List<Pair<String, String>> messages ) {
        parent.add( httpStatusCode, messages );
        return this;
    }

    /** A message with a code, built from the code and text given to {@link #errors(String, List)}. */
    private record CodedMessage( String code, String message ) implements ValidationMessage {
    }

    private static String format( String message, Map<String, Object> args ) {
        return TEMPLATE_ENGINE.getRuntimeTemplate( message, new TypeRef<Map<String, Object>>() {}, message, TemplateAccumulators.STRING, ErrorStrategy.ERROR, null, null ).render( args ).get();
    }

    /**
     * Returns the {@link ValidationErrors} this builder was created from.
     */
    public ValidationErrors endCode() {
        return parent;
    }
}
