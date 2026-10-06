package oap.ws;

import java.io.Serial;
import java.io.Serializable;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.ValidationErrorsBuilder;
import oap.validation.ValidationMessage;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponseBuilder implements Serializable {
    @Serial
    private static final long serialVersionUID = 4917302865134890216L;

    private int statusCode;
    private String error;
    private final List<ErrorResponse.Message> messages = new ArrayList<>();

    public ErrorResponseBuilder statusCode( int statusCode ) {
        this.statusCode = statusCode;
        return this;
    }

    public ErrorResponseBuilder error( String error ) {
        this.error = error;
        return this;
    }

    public ErrorResponseBuilder message( String message ) {
        messages.add( new ErrorResponse.Message( null, message ) );
        return this;
    }

    public ErrorResponseBuilder message( String code, String message ) {
        messages.add( new ErrorResponse.Message( code, message ) );
        return this;
    }

    public ErrorResponseBuilder message( ValidationMessage message ) {
        return message( message.code(), message.message() );
    }

    public ErrorResponse build() {
        return new ErrorResponse( statusCode, error, List.copyOf( messages ) );
    }

    public ValidationErrors toValidationErrors() {
        ValidationErrorsBuilder builder = ValidationErrors.empty().statusCode( statusCode );
        for( ErrorResponse.Message m : messages ) {
            builder = m.code == null ? builder.error( m.message ) : builder.error( new CodedMessage( m.code, m.message ) );
        }
        return builder.endCode();
    }

    /** A message with a code, as a {@link ValidationMessage}. */
    private record CodedMessage( String code, String message ) implements ValidationMessage {
    }
}
