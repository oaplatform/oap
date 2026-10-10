package oap.ws;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.StatusCode.FORBIDDEN;

public class ErrorResponse403Builder extends AbstractErrorResponseBuilder<ErrorResponse403Builder> {
    public ErrorResponse403Builder() {
        super( new ErrorResponseBuilder().statusCode( FORBIDDEN ).error( "Forbidden" ) );
    }

    @Override
    public Response build() {
        return new Response( FORBIDDEN )
            .withContentType( APPLICATION_JSON )
            .withBody( builder.build() );
    }
}
