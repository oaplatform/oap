package oap.ws.testng;

import static org.testng.Assert.assertEquals;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class TestHttpTry<T> {
    private final HttpAsserts.Response response;
    private final int code;
    private final T t;

    public TestHttpTry( T t, HttpAsserts.Response response, int code ) {
        this.t = t;
        this.response = response;
        this.code = code;
    }

    public void orElse( int code, String message ) {
        assertEquals( response.code, code );
        assertEquals( response.body, message );
    }

    public T get() {
        response.assertResponse( code );
        return t;
    }
}
