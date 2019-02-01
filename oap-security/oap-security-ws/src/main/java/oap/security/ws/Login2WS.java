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
import oap.security.acl.TemporaryTokenService;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.joda.time.DateTime;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class Login2WS {
    private final AuthService2 authService;
    private final String cookieDomain;
    private final int cookieExpiration;
    private final TemporaryTokenService temporaryTokenService;

    public Login2WS( AuthService2 authService,
                     String cookieDomain, int cookieExpiration,
                     TemporaryTokenService temporaryTokenService ) {
        this.authService = authService;
        this.cookieDomain = cookieDomain;
        this.cookieExpiration = cookieExpiration;
        this.temporaryTokenService = temporaryTokenService;
    }

    @WsMethod( method = GET, path = "/" )
    public HttpResponse login( @WsParam( from = QUERY ) String email,
                               @WsParam( from = QUERY ) String password ) {
        log.debug( "login email = {}, password = {}", email, "XXX" );
        final Optional<Token2> optionalToken = authService.generateToken( email, password );

        return login( optionalToken );
    }

    private HttpResponse login( Optional<Token2> optionalToken ) {
        if( optionalToken.isPresent() ) {
            final Token2 token = optionalToken.get();
            final HttpResponse ok = HttpResponse.ok( token );
            return withAuthorization( ok, token );
        } else {
            return HttpResponse.status( HTTP_UNAUTHORIZED, "Username or password is invalid" );
        }
    }

    @WsMethod( method = GET, path = "/{tokenId}" )
    public HttpResponse loginByTemporaryToken( @WsParam( from = PATH ) String tokenId ) {
        log.debug( "loginByTemporaryToken tokenId = {}", tokenId );

        return login( temporaryTokenService.get( tokenId ).flatMap( tt -> authService.generateToken( tt.objectId ) ) );
    }

    public HttpResponse withAuthorization( HttpResponse response, Token2 token ) {
        return response.withHeader( "Authorization", token.id )
            .withCookie( new HttpResponse.CookieBuilder()
                .withCustomValue( "Authorization", token.id )
                .withDomain( cookieDomain )
                .withPath( "/" )
                .withExpires( DateTime.now().plusMinutes( cookieExpiration ) )
                .build()
            );
    }

    @WsMethod( method = GET, path = "/as/{loginAs}" )
    @WsSecurity2( object = "{loginAs}", permission = "user.adminLogin" )
    public HttpResponse adminLogin( @WsParam( from = PATH ) String loginAs ) {
        log.debug( "admin login as = {}", loginAs );

        return login( authService.generateToken( loginAs ) );
    }
}
