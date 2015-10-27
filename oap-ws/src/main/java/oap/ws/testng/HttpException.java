package oap.ws.testng;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public abstract class HttpException extends RuntimeException {
    public int code;

    protected HttpException( int code, String message ) {
        super( code + ":" + message );
        this.code = code;
    }
}
