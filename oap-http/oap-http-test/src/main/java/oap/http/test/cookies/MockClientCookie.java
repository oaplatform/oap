package oap.http.test.cookies;

import org.apache.http.cookie.ClientCookie;

public class MockClientCookie<T extends ClientCookie> extends MockCookie<T> implements ClientCookie {
    MockClientCookie( T cookie ) {
        super( cookie );
    }

    @Override
    public String getAttribute( String name ) {
        return cookie.getAttribute( name );
    }

    @Override
    public boolean containsAttribute( String name ) {
        return cookie.containsAttribute( name );
    }
}
