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

package oap.ws.sso.interceptor;

import lombok.extern.slf4j.Slf4j;
import oap.http.Cookie;
import oap.util.Result;
import oap.ws.InvocationContext;
import oap.ws.Response;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.SSO;
import oap.ws.sso.User;
import oap.ws.sso.UserProvider;
import oap.ws.sso.UserWithCookies;
import oap.ws.sso.WsSecurity;

import java.util.Objects;
import java.util.Optional;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.sso.SSO.ISSUER;
import static oap.ws.sso.SSO.SESSION_USER_KEY;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.sso.WsSecurity.USER;

@Slf4j
public class JWTSecurityInterceptor implements Interceptor {

    private final UserProvider userProvider;

    public JWTSecurityInterceptor( UserProvider userProvider ) {
        this.userProvider = Objects.requireNonNull( userProvider );
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() ) {
            return Optional.empty();
        }

        String accessToken = SSO.getAuthentication( context.exchange );
        Optional<String> refreshToken = SSO.getRefreshAuthentication( context.exchange );
        Optional<User> sessionUserKey = context.session.get( SESSION_USER_KEY );
        String issuerName = this.getClass().getSimpleName();

        Result<UserWithCookies, String> validUser;

        Optional<String> realm = switch( wss.get().realm() ) {
            case SYSTEM, USER -> Optional.of( wss.get().realm() );
            default -> context.getParameter( wss.get().realm() );
        };

        if( realm.isEmpty() ) {
            return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );
        }

        String realmString = realm.get();
        String[] wssPermissions = wss.get().permissions();

        validUser = userProvider.getAuthenticatedByAccessToken( Optional.ofNullable( accessToken ), refreshToken, sessionUserKey.map( User::getEmail ), realmString, wssPermissions );

        if( !validUser.isSuccess() ) {
            return Optional.of( new Response( UNAUTHORIZED, validUser.failureValue ) );
        }
        context.session.set( SESSION_USER_KEY, validUser.successValue );
        context.session.set( ISSUER, issuerName );
        validUser.successValue.responseAccessToken.ifPresent( cookie -> context.session.set( SSO.AUTHENTICATION_KEY, cookie ) );
        validUser.successValue.responseRefreshToken.ifPresent( cookie -> context.session.set( SSO.REFRESH_TOKEN_KEY, cookie ) );

        return Optional.empty();
    }

    @Override
    public void after( Response response, InvocationContext context ) {
        context.session.<Cookie>get( SSO.AUTHENTICATION_KEY ).ifPresent( response::withCookie );
        context.session.<Cookie>get( SSO.REFRESH_TOKEN_KEY ).ifPresent( response::withCookie );
    }
}
