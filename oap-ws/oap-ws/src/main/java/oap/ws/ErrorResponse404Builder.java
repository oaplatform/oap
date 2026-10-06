package oap.ws;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.StatusCode.NOT_FOUND;

public class ErrorResponse404Builder {
    private final ErrorResponseBuilder builder = new ErrorResponseBuilder()
        .statusCode( NOT_FOUND )
        .error( "Not Found" );

    public ErrorResponse404Builder error( String error ) {
        builder.error( error );
        return this;
    }

    public ErrorResponse404Builder message( String message ) {
        builder.message( message );
        return this;
    }

    public ErrorResponse404Builder message( String code, String message ) {
        builder.message( code, message );
        return this;
    }

    public Response build() {
        return new Response( NOT_FOUND )
            .withContentType( APPLICATION_JSON )
            .withBody( builder.build() );
    }
}
