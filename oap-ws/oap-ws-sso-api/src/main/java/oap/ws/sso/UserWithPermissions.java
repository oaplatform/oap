package oap.ws.sso;

import lombok.ToString;
import oap.http.Cookie;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@ToString
public class UserWithPermissions implements Serializable {
    @Serial
    private static final long serialVersionUID = -1888285603025163121L;

    public final User user;
    public final List<String> permissions;
    public final Optional<Cookie> responseAccessToken;
    public final Optional<Cookie> responseRefreshToken;

    public UserWithPermissions( User user, List<String> permissions, Optional<Cookie> responseAccessToken, Optional<Cookie> responseRefreshToken ) {
        this.user = user;
        this.permissions = permissions;
        this.responseAccessToken = responseAccessToken;
        this.responseRefreshToken = responseRefreshToken;
    }
}
