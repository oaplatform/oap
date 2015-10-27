package oap.ws.testng;


import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class HttpNoContentException extends HttpException {
    public HttpNoContentException( String message ) {
        super( HTTP_NO_CONTENT, message );
    }
}
