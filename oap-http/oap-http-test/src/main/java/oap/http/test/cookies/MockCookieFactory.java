package oap.http.test.cookies;

import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;

public class MockCookieFactory {
    public static Cookie wrap( Cookie cookie ) {
        return cookie instanceof ClientCookie ? new MockClientCookie( cookie ) : new MockCookie( cookie );
    }
}
