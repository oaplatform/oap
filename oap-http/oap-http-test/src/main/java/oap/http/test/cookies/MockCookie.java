package oap.http.test.cookies;

import org.eclipse.jetty.http.HttpCookie;
import org.joda.time.DateTime;

import java.time.Instant;
import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;

public class MockCookie implements HttpCookie {
    protected final HttpCookie cookie;

    MockCookie( HttpCookie cookie ) {
        this.cookie = cookie;
    }

    @Override
    public String getName() {
        return cookie.getName();
    }

    @Override
    public String getValue() {
        return cookie.getValue();
    }

    @Override
    public String getComment() {
        return cookie.getComment();
    }

    @Override
    public String getDomain() {
        return cookie.getDomain();
    }

    @Override
    public String getPath() {
        return cookie.getPath();
    }

    @Override
    public boolean isSecure() {
        return cookie.isSecure();
    }

    @Override
    public SameSite getSameSite() {
        return cookie.getSameSite();
    }

    @Override
    public boolean isHttpOnly() {
        return cookie.isHttpOnly();
    }

    @Override
    public boolean isPartitioned() {
        return cookie.isPartitioned();
    }

    @Override
    public int getVersion() {
        return cookie.getVersion();
    }

    @Override
    public Map<String, String> getAttributes() {
        return cookie.getAttributes();
    }

    @Override
    public Instant getExpires() {
        return cookie.getExpires();
    }

    @Override
    public long getMaxAge() {
        return cookie.getMaxAge();
    }

    @Override
    public boolean isExpired() {
        DateTime date = new DateTime( UTC );

        return date.isAfter( cookie.getExpires().toEpochMilli() );
    }
}
