package oap.ws.validate;

import oap.reflect.TypeRef;
import oap.template.ErrorStrategy;
import oap.template.TemplateAccumulators;
import oap.template.TemplateEngine;
import oap.util.Dates;
import oap.util.Pair;

import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;
import static oap.util.Pair.__;

/**
 * Adds validation messages to a {@link ValidationErrors} under one HTTP status code.
 * <p>
 * Obtain it with {@link ValidationErrors#statusCode(int)} and finish with {@link #endCode()}, which returns the
 * {@link ValidationErrors} it was created from, so several status codes can be chained.
 * <p>
 * Each message may carry an optional message code (the {@code code} of {@code ErrorResponse.Message}).
 * Messages without a code use the variants without an {@code int code} parameter.
 * <p>
 * Public methods accept only message codes above {@link #MAX_INTERNAL_CODE}. Codes up to {@link #MAX_INTERNAL_CODE}
 * are reserved for package-internal messages, added with the {@code internal*} methods.
 */
public final class ValidationErrorsBuilder {
    static final int MAX_INTERNAL_CODE = 1_000_000;

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
        return pairs( List.of( __( null, format( message, args ) ) ) );
    }

    /**
     * Adds one message with the given message code, which must be above {@link #MAX_INTERNAL_CODE}.
     *
     * @throws IllegalArgumentException if {@code code} is not above {@link #MAX_INTERNAL_CODE}
     */
    public ValidationErrorsBuilder error( int code, String message ) {
        return pairs( List.of( __( code, message ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with the given message code, which must be above {@link #MAX_INTERNAL_CODE}.
     * Placeholders are {@code ${name}}, resolved from {@code args}.
     *
     * @throws IllegalArgumentException if {@code code} is not above {@link #MAX_INTERNAL_CODE}
     */
    public ValidationErrorsBuilder error( int code, String message, Map<String, Object> args ) {
        return error( code, format( message, args ) );
    }

    /**
     * Adds messages with no message code.
     */
    public ValidationErrorsBuilder errors( List<String> messages ) {
        return pairs( messages.stream().map( message -> __( ( Integer ) null, message ) ).toList() );
    }

    /**
     * Adds messages that all share the given message code, which must be above {@link #MAX_INTERNAL_CODE}.
     *
     * @throws IllegalArgumentException if {@code code} is not above {@link #MAX_INTERNAL_CODE}
     */
    public ValidationErrorsBuilder errors( int code, List<String> messages ) {
        return pairs( messages.stream().map( message -> __( code, message ) ).toList() );
    }

    /**
     * Adds messages, each with its own message code (first element) and text (second element).
     * A non-null code must be above {@link #MAX_INTERNAL_CODE}; a {@code null} code means no code.
     *
     * @throws IllegalArgumentException if a non-null code is not above {@link #MAX_INTERNAL_CODE}
     */
    public ValidationErrorsBuilder pairs( List<Pair<Integer, String>> messages ) {
        for( Pair<Integer, String> message : messages ) {
            if( message._1 != null && message._1 <= MAX_INTERNAL_CODE )
                throw new IllegalArgumentException( s( "message code must be above ${MAX_INTERNAL_CODE}: ${message._1}" ) );
        }
        return add( messages );
    }

    /**
     * Adds one package-internal message with a code up to {@link #MAX_INTERNAL_CODE}.
     *
     * @throws IllegalArgumentException if {@code code} is above {@link #MAX_INTERNAL_CODE}
     */
    ValidationErrorsBuilder internalError( int code, String message ) {
        return internalErrors( code, List.of( message ) );
    }

    /**
     * Adds one package-internal message formatted with {@code args}, with a code up to {@link #MAX_INTERNAL_CODE}.
     *
     * @throws IllegalArgumentException if {@code code} is above {@link #MAX_INTERNAL_CODE}
     */
    ValidationErrorsBuilder internalError( int code, String message, Map<String, Object> args ) {
        return internalError( code, format( message, args ) );
    }

    /**
     * Adds package-internal messages that all share a code up to {@link #MAX_INTERNAL_CODE}.
     *
     * @throws IllegalArgumentException if {@code code} is above {@link #MAX_INTERNAL_CODE}
     */
    ValidationErrorsBuilder internalErrors( int code, List<String> messages ) {
        if( code > MAX_INTERNAL_CODE )
            throw new IllegalArgumentException( s( "internal message code must be at most ${MAX_INTERNAL_CODE}: ${code}" ) );
        return add( messages.stream().map( message -> __( code, message ) ).toList() );
    }

    private ValidationErrorsBuilder add( List<Pair<Integer, String>> messages ) {
        parent.add( httpStatusCode, messages );
        return this;
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
