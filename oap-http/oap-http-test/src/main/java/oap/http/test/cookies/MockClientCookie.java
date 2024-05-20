package oap.http.test.cookies;

import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;

public class MockClientCookie extends MockCookie implements ClientCookie {
    MockClientCookie( Cookie cookie ) {
        super( cookie );
    }

    @Override
    public String getAttribute( String name ) {
        return ( ( ClientCookie ) cookie ).getAttribute( name );
    }

    @Override
    public boolean containsAttribute( String name ) {
        return ( ( ClientCookie ) cookie ).containsAttribute( name );
    }
}
