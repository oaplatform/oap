/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.sso;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.Permissions.MANAGE_SELF;
import static oap.ws.sso.SSO.authenticatedResponse;

@Slf4j
public class AuthWS {

    private final AuthService authService;
    private final String cookieDomain;
    private final int cookieExpiration;

    public AuthWS( AuthService authService, String cookieDomain, int cookieExpiration ) {
        this.authService = authService;
        this.cookieDomain = cookieDomain;
        this.cookieExpiration = cookieExpiration;
    }

    @WsMethod( method = GET, path = "/login" )
    public HttpResponse login( @WsParam( from = QUERY ) String email, @WsParam( from = QUERY ) String password ) {
        return authService.authenticate( email, password )
            .map( t -> authenticatedResponse( t, cookieDomain, cookieExpiration ) )
            .orElse( HttpResponse.status( HTTP_UNAUTHORIZED, "Username or password is invalid" ).response() );
    }

    @WsMethod( method = GET, path = "/logout" )
    @WsSecurity( permissions = MANAGE_SELF )
    @WsValidate( { "validateUserAccess" } )
    public void logout( @WsParam( from = QUERY ) String email, @WsParam( from = SESSION ) User user ) {
        log.debug( "Invalidating token for user [{}]", email );

        authService.invalidateUser( email );
    }

    @SuppressWarnings( "unused" )
    public ValidationErrors validateUserAccess( String email, User user ) {
        return Objects.equals( user.getEmail(), email )
            ? ValidationErrors.empty()
            : ValidationErrors.error( HTTP_FORBIDDEN, format( "User [%s] doesn't have enough permissions", user.getEmail() ) );
    }

    @WsMethod( method = GET, path = "/token/{id}" )
    public Optional<Token> getToken( @WsParam( from = PATH ) String id ) {
        return authService.getToken( id );
    }
}
