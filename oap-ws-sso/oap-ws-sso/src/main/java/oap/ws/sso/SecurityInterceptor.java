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
import oap.sso.PrecedenceRoleService;
import oap.sso.Token;
import oap.sso.TokenService;
import oap.sso.User;
import oap.ws.interceptor.Interceptor;

import java.util.Optional;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

@Slf4j
public class SecurityInterceptor implements Interceptor {

    public static final String USER_ID = "userid";
    public static final String AUTHORIZATION = "Authorization";
    TokenService tokenService;
    PrecedenceRoleService precedenceRoleService;

    public SecurityInterceptor( TokenService tokenService, PrecedenceRoleService precedenceRoleService ) {
        this.tokenService = tokenService;
        this.precedenceRoleService = precedenceRoleService;
    }

    public static String getSessionToken( Request request ) {
        return request.header( AUTHORIZATION ).orElse( request.cookie( AUTHORIZATION ).orElse( null ) );
    }

    @Override
    public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
        Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isPresent() ) {
            if( session == null ) return Optional.of( HttpResponse
                .status( HTTP_INTERNAL_ERROR, "no session provided for security interceptor" )
                .response() );

            User user = ( User ) session.get( "user" ).orElse( null );
            if( user != null ) {
                log.trace( "User [{}] found in session", user.getEmail() );

                int methodRolePrecedence = precedenceRoleService.getPrecedence( annotation.get().role() );

                if( precedenceRoleService.getPrecedence( user.getRole() ) > methodRolePrecedence ) {
                    HttpResponse httpResponse = HttpResponse.status( 403, format( "User [%s] has no access to method [%s]", user.getEmail(), method.name() ) ).response();

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }
            } else {
                String sessionToken = request.header( "Authorization" ).orElse( request.cookie( "Authorization" ).orElse( null ) );

                if( sessionToken == null ) {
                    HttpResponse httpResponse = HttpResponse.status( 401, "Session token is missing in header or cookie" ).response();

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }

                Token token = tokenService.getToken( sessionToken ).orElse( null );

                if( token == null ) {
                    HttpResponse httpResponse = HttpResponse.status( 401, format( "Token id [%s] expired or was "
                        + "not created", sessionToken ) ).response();

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }

                user = token.user;

                session.set( "user", user );
                session.set( USER_ID, user.getEmail() );

                int methodRolePrecedence = precedenceRoleService.getPrecedence( annotation.get().role() );

                if( precedenceRoleService.getPrecedence( user.getRole() ) > methodRolePrecedence ) {
                    HttpResponse httpResponse = HttpResponse.status( 403, format( "User [%s] has no access to method [%s]", user.getEmail(), method.name() ) ).response();

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }
            }
        }

        return Optional.empty();
    }

}
