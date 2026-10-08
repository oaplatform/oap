package oap.ws;

import java.util.Collection;
import java.util.List;

import static oap.http.Http.StatusCode.BAD_REQUEST;

public class WsClientException extends WsException {
    public final ErrorResponseBuilder errorResponse;

    public WsClientException( String message, int code, Collection<String> errors ) {
        super( message );

        this.errorResponse = new ErrorResponseBuilder().statusCode( code ).error( message );
        errors.forEach( errorResponse::message );
    }

    public WsClientException( String message, int httpStatusCode, List<ErrorResponse.Message> errors ) {
        super( message );

        this.errorResponse = new ErrorResponseBuilder().statusCode( httpStatusCode ).error( message );
        errors.forEach( this.errorResponse::message );
    }

    public WsClientException( String message, List<String> errors ) {
        this( message, BAD_REQUEST, errors );
    }

    public WsClientException( String message ) {
        this( message, List.of( message ) );
    }

    public WsClientException( String message, Throwable cause ) {
        super( message, cause );
        this.errorResponse = new ErrorResponseBuilder().statusCode( BAD_REQUEST ).error( message ).message( message );
    }

    public WsClientException( Throwable cause ) {
        this( cause.getMessage(), cause );
    }
}
