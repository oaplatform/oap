package oap.http.test.cookies;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpCookieStore;

import java.net.URI;
import java.util.List;

public class MockHttpCookieStorage implements HttpCookieStore {
    private final HttpCookieStore cookieStore;

    public MockHttpCookieStorage( HttpCookieStore cookieStore ) {
        this.cookieStore = cookieStore;
    }

    public MockHttpCookieStorage() {
        this( new Default() );
    }

    @Override
    public boolean add( URI uri, HttpCookie httpCookie ) {
        return cookieStore.add( uri, new MockCookie( httpCookie ) );
    }

    @Override
    public List<HttpCookie> all() {
        return cookieStore.all();
    }

    @Override
    public List<HttpCookie> match( URI uri ) {
        return cookieStore.match( uri );
    }

    @Override
    public boolean remove( URI uri, HttpCookie httpCookie ) {
        return cookieStore.remove( uri, httpCookie );
    }

    @Override
    public boolean clear() {
        return cookieStore.clear();
    }
}
