package oap.ws.testng;


import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class HttpOkException extends HttpException {
    public HttpOkException( String message ) {
        super( HTTP_OK, message );
    }
}
