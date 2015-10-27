package oap.ws.testng;


import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class HttpBadRequestException extends HttpException {
    public HttpBadRequestException( String message ) {
        super( HTTP_BAD_REQUEST, message );
    }
}
