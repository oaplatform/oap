package oap.ws;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.Headers.WWW_AUTHENTICATE;
import static oap.http.Http.StatusCode.UNAUTHORIZED;

public class ErrorResponse401Builder extends AbstractErrorResponseBuilder<ErrorResponse401Builder> {
    public ErrorResponse401Builder() {
        super( new ErrorResponseBuilder().statusCode( UNAUTHORIZED ).error( "Unauthorized" ) );
    }

    @Override
    public Response build() {
        return new Response( UNAUTHORIZED )
            .withContentType( APPLICATION_JSON )
            .withHeader( WWW_AUTHENTICATE, "Bearer" )
            .withBody( builder.build() );
    }
}
