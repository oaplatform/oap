package oap.ws;

import oap.validation.ValidationMessage;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.StatusCode.FORBIDDEN;

public class ErrorResponse403Builder {
    private final ErrorResponseBuilder builder = new ErrorResponseBuilder()
        .statusCode( FORBIDDEN )
        .error( "Forbidden" );

    public ErrorResponse403Builder error( String error ) {
        builder.error( error );
        return this;
    }

    public ErrorResponse403Builder message( String message ) {
        builder.message( message );
        return this;
    }

    public ErrorResponse403Builder message( String code, String message ) {
        builder.message( code, message );
        return this;
    }

    public ErrorResponse403Builder message( ValidationMessage message ) {
        return message( message.code(), message.message() );
    }

    public Response build() {
        return new Response( FORBIDDEN )
            .withContentType( APPLICATION_JSON )
            .withBody( builder.build() );
    }
}
