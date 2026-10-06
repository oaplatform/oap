package oap.ws;

import oap.validation.ValidationMessage;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.Headers.WWW_AUTHENTICATE;
import static oap.http.Http.StatusCode.UNAUTHORIZED;

public class ErrorResponse401Builder {
    private final ErrorResponseBuilder builder = new ErrorResponseBuilder()
        .statusCode( UNAUTHORIZED )
        .error( "Unauthorized" );

    public ErrorResponse401Builder error( String error ) {
        builder.error( error );
        return this;
    }

    public ErrorResponse401Builder message( String message ) {
        builder.message( message );
        return this;
    }

    public ErrorResponse401Builder message( String code, String message ) {
        builder.message( code, message );
        return this;
    }

    public ErrorResponse401Builder message( ValidationMessage message ) {
        return message( message.code(), message.message() );
    }

    public Response build() {
        return new Response( UNAUTHORIZED )
            .withContentType( APPLICATION_JSON )
            .withHeader( WWW_AUTHENTICATE, "Bearer" )
            .withBody( builder.build() );
    }
}
