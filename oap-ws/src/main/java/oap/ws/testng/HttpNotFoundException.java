package oap.ws.testng;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class HttpNotFoundException extends HttpException {
    public HttpNotFoundException( String message ) {
        super( HTTP_NOT_FOUND, message );
    }
}
