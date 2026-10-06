package oap.ws;

import java.io.Serial;
import java.io.Serializable;
import oap.ws.validate.ValidationErrors;
import oap.util.Pair;

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

    public ErrorResponseBuilder message( int code, String message ) {
        messages.add( new ErrorResponse.Message( code, message ) );
        return this;
    }

    public ErrorResponseBuilder message( Integer code, String message ) {
        messages.add( new ErrorResponse.Message( code, message ) );
        return this;
    }

    public ErrorResponse build() {
        return new ErrorResponse( statusCode, error, List.copyOf( messages ) );
    }

    public ValidationErrors toValidationErrors() {
        return ValidationErrors.empty().statusCode( statusCode ).pairs( messages.stream().map( m -> Pair.__( m.code, m.message ) ).toList() ).endCode();
    }
}
