package oap.ws.validate;

import oap.reflect.TypeRef;
import oap.template.ErrorStrategy;
import oap.template.TemplateAccumulators;
import oap.template.TemplateEngine;
import oap.util.Dates;
import oap.validation.ValidationMessage;
import oap.ws.ErrorResponse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Adds validation messages to a {@link ValidationErrors} under one HTTP status code.
 * <p>
 * Obtain it with {@link ValidationErrors#statusCode(int)} and finish with {@link #endCode()}, which returns the
 * {@link ValidationErrors} it was created from, so several status codes can be chained.
 * <p>
 * Each message may carry an optional message code and an optional JSON path (the {@code code} and {@code path} of
 * {@code ErrorResponse.Message}); both are {@code null} when absent. {@link #error(String)} adds a message with
 * neither. The other {@code error} methods take the code from a {@link ValidationMessage} (typically an enum
 * constant) or as a literal {@code String}, and all take an explicit {@code path} argument — pass {@code null} when
 * there is none.
 */
public final class ValidationErrorsBuilder {
    private static final TemplateEngine TEMPLATE_ENGINE = new TemplateEngine( null, Dates.m( 10 ) );

    private final ValidationErrors parent;
    private final int httpStatusCode;

    ValidationErrorsBuilder( ValidationErrors parent, int httpStatusCode ) {
        this.parent = parent;
        this.httpStatusCode = httpStatusCode;
    }

    private static String format( String message, Map<String, Object> args ) {
        return TEMPLATE_ENGINE.getRuntimeTemplate( message, new TypeRef<Map<String, Object>>() {}, message, TemplateAccumulators.STRING, ErrorStrategy.ERROR, null, null ).render( args ).get();
    }

    /**
     * Adds one message with no message code and no path.
     */
    public ValidationErrorsBuilder error( String message ) {
        return add( List.of( new ErrorResponse.Message( null, message, null ) ) );
    }

    /**
     * Adds the message of a {@link ValidationMessage} (typically an enum constant), with its code and {@code path}
     * ({@code null} when there is none).
     */
    public ValidationErrorsBuilder error( ValidationMessage message, @Nullable String path ) {
        return add( List.of( new ErrorResponse.Message( message.code(), message.message(), path ) ) );
    }

    /**
     * Adds the message of a {@link ValidationMessage} formatted with {@code args}, with its code and {@code path}
     * ({@code null} when there is none). Placeholders are {@code ${name}}, resolved from {@code args}.
     */
    public ValidationErrorsBuilder error( ValidationMessage message, Map<String, Object> args, @Nullable String path ) {
        return add( List.of( new ErrorResponse.Message( message.code(), format( message.message(), args ), path ) ) );
    }

    /**
     * Adds a message formatted with {@code args}, with the given message code ({@code null} for none) and
     * {@code path} ({@code null} when there is none). Placeholders are {@code ${name}}, resolved from {@code args}.
     */
    public ValidationErrorsBuilder error( @Nullable String code, String message, Map<String, Object> args, String path ) {
        return add( List.of( new ErrorResponse.Message( code, format( message, args ), path ) ) );
    }

    private ValidationErrorsBuilder add( List<ErrorResponse.Message> messages ) {
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
