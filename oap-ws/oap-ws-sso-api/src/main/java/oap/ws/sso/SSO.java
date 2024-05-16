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

import lombok.AllArgsConstructor;
import lombok.ToString;
import oap.http.Cookie;
import oap.http.server.nio.HttpServerExchange;
import oap.ws.Response;
import oap.ws.SessionManager;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.joda.time.DateTimeZone.UTC;

public class SSO {
    public static final String AUTHENTICATION_KEY = "Authorization";
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String SESSION_USER_KEY = "loggedUser";
    public static final String ISSUER = "issuer";

    @Nullable
    public static String getAuthentication( HttpServerExchange exchange ) {
        String value = Objects.requireNonNull( exchange ).getRequestHeader( AUTHENTICATION_KEY );
        if( value != null ) return value;
        return exchange.getRequestCookieValue( AUTHENTICATION_KEY );
    }

    @Nonnull
    public static Optional<String> getRefreshAuthentication( HttpServerExchange exchange ) {
        String value = Objects.requireNonNull( exchange ).getRequestHeader( REFRESH_TOKEN_KEY );
        if( value != null ) {
            return Optional.of( value );
        }
        return Optional.ofNullable( exchange.getRequestCookieValue( REFRESH_TOKEN_KEY ) );
    }

    public static Response authenticatedResponse( Authentication authentication, String cookieDomain, Boolean cookieSecure ) {
        return Response
            .jsonOk()
            .withHeader( AUTHENTICATION_KEY, authentication.accessToken.jwt )
            .withCookie( Cookie.builder( AUTHENTICATION_KEY, authentication.accessToken.jwt )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( authentication.accessToken.expires ) )
                .withHttpOnly( true )
                .withSecure( cookieSecure )
                .build()
            )
            .withCookie( Cookie.builder( REFRESH_TOKEN_KEY, authentication.refreshToken.jwt )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( authentication.refreshToken.expires ) )
                .withHttpOnly( true )
                .withSecure( cookieSecure )
                .build()
            )
            .withBody( authentication.view, false );
    }

    public static Response authenticatedResponse( Authentication authentication, String cookieDomain ) {
        return authenticatedResponse( authentication, cookieDomain, false );
    }

    private static DateTime getExpirationTimeCookie( Date expirationInToken, Date cookieExpiration ) {
        return expirationInToken != null ? new DateTime( expirationInToken ) : new DateTime( cookieExpiration );
    }

    public static Response logoutResponse( String cookieDomain ) {
        return Response
            .noContent()
            .withCookie( Cookie.builder( AUTHENTICATION_KEY, "<logged out>" )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( 1970, 1, 1, 1, 1, UTC ) )
                .build()
            )
            .withCookie( Cookie.builder( REFRESH_TOKEN_KEY, "<logged out>" )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( 1970, 1, 1, 1, 1, UTC ) )
                .build()
            )
            .withCookie( Cookie.builder( SessionManager.COOKIE_ID, "<logged out>" )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( 1970, 1, 1, 1, 1, UTC ) )
                .build()
            );
    }

    public static Response notAuthenticatedResponse( int code, String reasonPhrase, String cookieDomain ) {
        return new Response( code, reasonPhrase )
            .withCookie( Cookie.builder( AUTHENTICATION_KEY, "<logged out>" )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( 1970, 1, 1, 1, 1, UTC ) )
                .build()
            );
    }

    public static Tokens createAccessAndRefreshTokensFromRefreshToken( Authentication authentication, String cookieDomain, Boolean cookieSecure ) {
        return new Tokens(
            Cookie.builder( AUTHENTICATION_KEY, authentication.accessToken.jwt )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( authentication.accessToken.expires ) )
                .withHttpOnly( true )
                .withSecure( cookieSecure )
                .build(),
            Cookie.builder( REFRESH_TOKEN_KEY, authentication.refreshToken.jwt )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( new DateTime( authentication.refreshToken.expires ) )
                .withHttpOnly( true )
                .withSecure( cookieSecure )
                .build()
        );
    }

    @ToString
    @AllArgsConstructor
    public static class Tokens implements Serializable {
        @Serial
        private static final long serialVersionUID = 3139324331418579632L;

        public final Cookie accessToken;
        public final Cookie refreshToken;
    }
}
