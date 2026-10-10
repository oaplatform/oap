package oap.ws;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.StatusCode.NOT_FOUND;

public class ErrorResponse404Builder extends AbstractErrorResponseBuilder<ErrorResponse404Builder> {
    public ErrorResponse404Builder() {
        super( new ErrorResponseBuilder().statusCode( NOT_FOUND ).error( "Not Found" ) );
    }

    @Override
    public Response build() {
        return new Response( NOT_FOUND )
            .withContentType( APPLICATION_JSON )
            .withBody( builder.build() );
    }
}
