package oap.ws.testng;


import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class HttpMovedTempException extends HttpException {
    public HttpMovedTempException( String message ) {
        super( HTTP_MOVED_TEMP, message );
    }
}
