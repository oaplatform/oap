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
import oap.http.Request;
import oap.http.Session;
import oap.reflect.Reflection;
import oap.ws.interceptor.Interceptor;

import java.util.Optional;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static oap.ws.sso.SSO.USER_KEY;

@Slf4j
public class SecurityInterceptor implements Interceptor {

    private TokenService tokenService;
    private Roles roles;

    public SecurityInterceptor( TokenService tokenService, Roles roles ) {
        this.tokenService = tokenService;
        this.roles = roles;
    }

    @Override
    public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
        Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isPresent() ) {
            if( session == null ) return Optional.of( HttpResponse
                .status( HTTP_INTERNAL_ERROR, "no session provided for security interceptor" )
                .response() );

            if( !session.containsKey( USER_KEY ) ) {
                Optional<Token> authToken = SSO.getToken( request ).flatMap( tokenService::getToken );
                if( authToken.isEmpty() ) {
                    HttpResponse httpResponse = HttpResponse.status( HTTP_UNAUTHORIZED ).response();
                    log.debug( httpResponse.toString() );
                    return Optional.of( httpResponse );
                } else {
                    User user = authToken.get().user;
                    session.set( USER_KEY, user );
                    session.set( SSO.EMAIL_KEY, user.getEmail() );
                }
            }

            return session.<User>get( USER_KEY )
                .filter( u -> !roles.granted( u.getRole(), annotation.get().permissions() ) )
                .map( u -> {
                    log.debug( "denied access to method {}: role: {}, required: {}", method.name(), roles.permissionsOf( u.getRole() ), annotation.get().permissions() );
                    return HttpResponse.status( 403, format( "User [%s] has no access to method [%s]", u.getEmail(), method.name() ) ).response();
                } );
        }
        return Optional.empty();
    }
}

