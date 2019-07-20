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
import oap.ws.Interceptor;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

@Slf4j
public class SecurityInterceptor implements Interceptor {

    TokenService tokenService;
    PrecedenceRoleService precedenceRoleService;

    public SecurityInterceptor( TokenService tokenService, PrecedenceRoleService precedenceRoleService ) {
        this.tokenService = tokenService;
        this.precedenceRoleService = precedenceRoleService;
    }

    @Override
    public Optional<HttpResponse> intercept( Request request, Session session, Reflection.Method method,
                                             Function<Reflection.Parameter, Object> getParameterValueFunc ) {
        final Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isPresent() ) {
            if( session == null ) {
                final HttpResponse httpResponse = HttpResponse.status( 500, "Session doesn't exist; check if service is session aware" );

                log.error( httpResponse.toString() );

                return Optional.of( httpResponse );
            }

            User user = ( User ) session.get( "user" ).orElse( null );
            if( user != null ) {
                log.trace( "User [{}] found in session", user.getEmail() );

                final int methodRolePrecedence = precedenceRoleService.getPrecedence( annotation.get().role() );

                if( precedenceRoleService.getPrecedence( user.getRole() ) > methodRolePrecedence ) {
                    final HttpResponse httpResponse = HttpResponse.status( 403, format( "User [%s] has no access to method [%s]", user.getEmail(), method.name() ) );

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }
            } else {
                final String sessionToken = request.header( "Authorization" ).orElse( request.cookie( "Authorization" ).orElse( null ) );

                if( sessionToken == null ) {
                    final HttpResponse httpResponse = HttpResponse.status( 401, "Session token is missing in header or cookie" );

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }

                final Token token = tokenService.getToken( sessionToken ).orElse( null );

                if( token == null ) {
                    final HttpResponse httpResponse = HttpResponse.status( 401, format( "Token id [%s] expired or was "
                        + "not created", sessionToken ) );

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }

                user = token.user;

                session.set( "user", user );
                session.set( USER_ID, user.getEmail() );

                final int methodRolePrecedence = precedenceRoleService.getPrecedence( annotation.get().role() );

                if( precedenceRoleService.getPrecedence( user.getRole() ) > methodRolePrecedence ) {
                    final HttpResponse httpResponse = HttpResponse.status( 403, format( "User [%s] has no access to method [%s]", user.getEmail(), method.name() ) );

                    log.debug( httpResponse.toString() );

                    return Optional.of( httpResponse );
                }
            }
        }

        return Optional.empty();
    }
}
