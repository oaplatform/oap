package oap.ws;

import oap.http.Http;

import java.util.Collection;
import java.util.List;

public class WsClientException extends WsException {
    public final ErrorResponseBuilder errorResponse;

    public WsClientException( String message, int code, Collection<String> errors ) {
        super( message );

        this.errorResponse = new ErrorResponseBuilder().statusCode( code ).error( message );
        errors.forEach( errorResponse::message );
    }

    public WsClientException( String message, List<String> errors ) {
        this( message, Http.StatusCode.BAD_REQUEST, errors );
    }

    public WsClientException( String message ) {
        this( message, List.of( message ) );
    }

    public WsClientException( String message, Throwable cause ) {
        super( message, cause );
        this.errorResponse = new ErrorResponseBuilder().statusCode( Http.StatusCode.BAD_REQUEST ).error( message ).message( message );
    }

    public WsClientException( Throwable cause ) {
        this( cause.getMessage(), cause );
    }
}
