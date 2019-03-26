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
import oap.http.Request;
import oap.http.Session;
import oap.reflect.Reflection;
import oap.security.acl.AclService;
import oap.util.IdAccessorFactory;
import oap.ws.Interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Slf4j
public class SecurityInterceptor2 implements Interceptor {
    private final AclService aclService;
    private final TokenService2 tokenService;

    public SecurityInterceptor2( AclService aclService, TokenService2 tokenService ) {
        this.aclService = aclService;
        this.tokenService = tokenService;
    }

    @Override
    public Optional<HttpResponse> intercept( Request request, Session session, Reflection.Method method,
                                             Function<Reflection.Parameter, Object> getParameterValueFunc ) {
        log.trace( "intercept method={}, request={}", method, request.requestLine );

        var annotation = method.findAnnotation( WsSecurity2.class ).orElse( null );
        if( annotation == null ) return Optional.empty();

        if( session == null ) {
            final HttpResponse httpResponse = HttpResponse.status( 500, "Session doesn't exist; check if service is session aware" );

            log.error( httpResponse.toString() );

            return Optional.of( httpResponse );
        }

        var sessionToken = Interceptor.getSessionToken( request );
        if( sessionToken == null ) {
            final HttpResponse httpResponse = HttpResponse.status( 401, "Session token is missing in header or cookie" );

            log.debug( httpResponse.toString() );

            return Optional.of( httpResponse );
        }

        String userId = ( String ) session.get( USER_ID ).orElse( null );
        if( userId == null ) {
            var token = tokenService.getToken( sessionToken ).orElse( null );
            if( token == null ) {
                final HttpResponse httpResponse = HttpResponse.status( 401, format( "Token id [%s] expired or was "
                    + "not created", sessionToken ) );

                log.debug( httpResponse.toString() );

                return Optional.of( httpResponse );
            }
            userId = token.userId;
            session.set( USER_ID, userId );
        } else {
            log.trace( "User [{}] found in session", userId );
        }

        if( !annotation.object().isEmpty() && !annotation.permission().isEmpty() ) {
            var objectId = getObjectId( method, annotation, getParameterValueFunc );

            if( !aclService.checkOne( objectId, userId, annotation.permission() ) ) {
                var httpResponse = HttpResponse.status( 403, String.format( "User [%s] has no access to method [%s]", userId, method.name() ) );

                log.debug( httpResponse.toString() );

                return Optional.of( httpResponse );
            }
        }

        return Optional.empty();
    }

    @Override
    public Object postProcessing( Object value, Session session, Reflection.Method method ) {
        var annotation = method.findAnnotation( WsSecurityWithPermissions.class ).orElse( null );
        if( annotation == null ) return value;

        if( value instanceof List<?> ) {
            return ( ( List<?> ) value ).stream().map( v -> postProcessing( v, session, method ) ).collect( toList() );
        }

        var userId = ( String ) session.get( USER_ID ).orElse( null );

        var id = IdAccessorFactory.getter( value );

        List<String> res = aclService.checkAll( id, userId );
        if( annotation.includeRootPermissions() ) {
            res = new ArrayList<>( res );
            res.addAll( aclService.checkAll( AclService.ROOT, userId ) );
        }

        return new ObjectWithPermissions<>( res, value );
    }

    private String getObjectId( Reflection.Method method, WsSecurity2 annotation,
                                Function<Reflection.Parameter, Object> getParameterValueFunc ) {
        var parameterName = annotation.object();
        if( parameterName.startsWith( "{" ) ) {
            var parameter = method.getParameter( parameterName.substring( 1, parameterName.length() - 1 ) );
            return ( String ) getParameterValueFunc.apply( parameter );
        } else return parameterName;
    }
}

