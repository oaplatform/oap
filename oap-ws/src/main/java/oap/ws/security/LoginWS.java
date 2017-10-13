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

package oap.ws.security;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.joda.time.DateTime;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class LoginWS {

    private final AuthService authService;
    private final String cookieDomain;
    private final int cookieExpiration;

    public LoginWS( AuthService authService, String cookieDomain, int cookieExpiration ) {
        this.authService = authService;
        this.cookieDomain = cookieDomain;
        this.cookieExpiration = cookieExpiration;
    }

    @WsMethod( method = GET, path = "/" )
    public HttpResponse login( @WsParam( from = QUERY ) String email, @WsParam( from = QUERY ) String password ) {
        final Optional<Token> optionalToken = authService.generateToken( email, password );

        if( optionalToken.isPresent() ) {
            final Token token = optionalToken.get();
            final HttpResponse ok = HttpResponse.ok( token );
            return withAuthorization( ok, token );
        } else {
            return HttpResponse.status( HTTP_UNAUTHORIZED, "Username or password is invalid" );
        }
    }

    public HttpResponse withAuthorization( HttpResponse response, Token token ) {
        return response.withHeader( "Authorization", token.id )
                    .withCookie( new HttpResponse.CookieBuilder()
                        .withCustomValue( "Authorization", token.id )
                        .withDomain( cookieDomain )
                        .withPath( "/" )
                        .withExpires( DateTime.now().plusMinutes( cookieExpiration ) )
                        .build()
                    );
    }

}
