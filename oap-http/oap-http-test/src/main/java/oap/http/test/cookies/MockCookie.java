package oap.http.test.cookies;

import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;

import java.util.Date;

import static org.joda.time.DateTimeZone.UTC;

public class MockCookie<T extends Cookie> implements Cookie {
    protected final T cookie;

    MockCookie( T cookie ) {
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
    public String getCommentURL() {
        return cookie.getCommentURL();
    }

    @Override
    public Date getExpiryDate() {
        return cookie.getExpiryDate();
    }

    @Override
    public boolean isPersistent() {
        return cookie.isPersistent();
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
    public int[] getPorts() {
        return cookie.getPorts();
    }

    @Override
    public boolean isSecure() {
        return cookie.isSecure();
    }

    @Override
    public int getVersion() {
        return cookie.getVersion();
    }

    @Override
    public boolean isExpired( Date ignored ) {
        Date date = DateTime.now( UTC ).toDate();

        return cookie.isExpired( date );
    }
}
