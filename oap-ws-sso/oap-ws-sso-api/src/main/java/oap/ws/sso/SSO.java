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

import oap.http.HttpResponse;
import oap.http.Request;
import org.joda.time.DateTime;

import java.util.Optional;

public class SSO {
    public static final String AUTHENTICATION_KEY = "Auth";
    public static final String USER_KEY = "user";
    public static final String EMAIL_KEY = "email";

    public static Optional<String> getToken( Request request ) {
        return request.header( AUTHENTICATION_KEY ).or( () -> request.cookie( AUTHENTICATION_KEY ) );
    }

    public static HttpResponse authenticatedResponse( Token token, String cookieDomain, int cookieExpiration ) {
        return HttpResponse.ok( token )
            .withHeader( SSO.AUTHENTICATION_KEY, token.id )
            .withCookie( new HttpResponse.CookieBuilder()
                .withValue( SSO.AUTHENTICATION_KEY, token.id )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( DateTime.now().plusMinutes( cookieExpiration ) )
                .build()
            ).response();
    }
}
