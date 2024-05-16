package oap.ws.sso;

import lombok.ToString;
import oap.http.Cookie;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

@ToString
public class UserWithCookies implements Serializable {
    @Serial
    private static final long serialVersionUID = -1888285603025163121L;

    public final User user;
    public final Optional<Cookie> responseAccessToken;
    public final Optional<Cookie> responseRefreshToken;

    public UserWithCookies( User user, Optional<Cookie> responseAccessToken, Optional<Cookie> responseRefreshToken ) {
        this.user = user;
        this.responseAccessToken = responseAccessToken;
        this.responseRefreshToken = responseRefreshToken;
    }
}
