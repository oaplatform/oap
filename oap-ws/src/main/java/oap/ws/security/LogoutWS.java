package oap.ws.security;

import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.util.Objects;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;

@Slf4j
public class LogoutWS {

    private final AuthService authService;

    public LogoutWS( AuthService authService ) {
        this.authService = authService;
    }

    @WsMethod( method = GET, path = "/" )
    @WsSecurity( role = Role.USER )
    @WsValidate( { "validateUserAccess" } )
    public void logout( @WsParam( from = QUERY ) String email, @WsParam( from = SESSION ) User user ) {
        log.debug( "Invalidating token for user [{}]", email );

        authService.invalidateUser( email );
    }

    @SuppressWarnings( "unused" )
    public ValidationErrors validateUserAccess( final String email, final User user ) {
        return Objects.equals( user.getEmail(), email )
            ? ValidationErrors.empty()
            : ValidationErrors.error( HTTP_FORBIDDEN, format( "User [%s] doesn't have enough permissions", user.getEmail() ) );
    }
}
