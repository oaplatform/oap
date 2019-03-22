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

package oap.security.ws;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.sso.User;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import oap.ws.validate.ValidationErrors;
import org.joda.time.DateTime;

import java.util.Objects;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.SESSION;

@Slf4j
public class Logout2WS {
    private final AuthService2 authService;
    private final String cookieDomain;

    public Logout2WS( AuthService2 authService, String cookieDomain ) {
        this.authService = authService;
        this.cookieDomain = cookieDomain;
    }

    @WsMethod( method = GET, path = "/" )
    @WsSecurity2
    public HttpResponse logout( @WsParam( from = SESSION ) String userid ) {
        log.debug( "Invalidating token for user [{}]", userid );

        authService.invalidateUser( userid );


        return HttpResponse
            .status( HTTP_NO_CONTENT )
            .withCookie( new HttpResponse.CookieBuilder()
                .withCustomValue( "Authorization", "expired" )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( DateTime.now() )
                .build()
            );
    }

    @SuppressWarnings( "unused" )
    public ValidationErrors validateUserAccess( final String email, final User user ) {
        return Objects.equals( user.getEmail(), email )
            ? ValidationErrors.empty()
            : ValidationErrors.error( HTTP_FORBIDDEN, format( "User [%s] doesn't have enough permissions", user.getEmail() ) );
    }
}
