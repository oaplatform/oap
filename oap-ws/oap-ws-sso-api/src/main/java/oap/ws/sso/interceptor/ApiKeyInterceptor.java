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
import oap.ws.InvocationContext;
import oap.ws.Response;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.User;
import oap.ws.sso.UserProvider;

import java.util.Optional;

import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.sso.SSO.ISSUER;
import static oap.ws.sso.SSO.SESSION_USER_KEY;

/**
 * Authenticates requests using an access key + API key pair.
 *
 * <p>Parameter lookup order:
 * <ul>
 *   <li><b>Access key</b>: query param {@code accessKey}, then header {@code X-Access-Token}</li>
 *   <li><b>API key</b>: query param {@code X-API-Key}, then header {@code apiKey}</li>
 * </ul>
 *
 * <p>Behaviour:
 * <ul>
 *   <li>If either parameter is absent — passes silently (no response).</li>
 *   <li>If the key pair is invalid — returns {@code 401 Unauthorized}.</li>
 *   <li>If valid — places the user in the session for the duration of the request;
 *       {@code after()} removes all session keys afterwards.</li>
 * </ul>
 */
@Slf4j
public class ApiKeyInterceptor implements Interceptor {
    public static final String SESSION_API_KEY_AUTHENTICATED = "apiKeyAuthenticated";
    private final UserProvider userProvider;

    public ApiKeyInterceptor( UserProvider userProvider ) {
        super();
        this.userProvider = userProvider;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        String accessKey = context.exchange.getStringParameter( "accessKey" );
        if( accessKey == null ) {
            accessKey = context.exchange.getRequestHeader( "X-Access-Token" );
        }
        if( accessKey == null ) return Optional.empty();
        String apiKey = context.exchange.getStringParameter( "apiKey" );
        if( apiKey == null ) {
            apiKey = context.exchange.getRequestHeader( "X-API-Key" );
        }
        if( apiKey == null ) return Optional.empty();

        User user = userProvider.getAuthenticatedByApiKey( accessKey, apiKey ).orElse( null );
        if( user != null ) {
            context.session.set( SESSION_USER_KEY, user );
            context.session.set( SESSION_API_KEY_AUTHENTICATED, true );
            context.session.set( ISSUER, this.getClass().getSimpleName() );
            log.trace( "set user {} into session {}", user, context.session );
            return Optional.empty();
        }
        return Optional.of( new Response( UNAUTHORIZED ) );
    }

    @Override
    public void after( Response response, InvocationContext context ) {
        context.session.<Boolean>get( SESSION_API_KEY_AUTHENTICATED ).ifPresent( apiKeyAuthentication -> {
            if( apiKeyAuthentication ) {
                log.trace( "removing temporary authentication of {}", context.session.get( SESSION_USER_KEY ) );

                context.session.remove( SESSION_USER_KEY );
                context.session.remove( SESSION_API_KEY_AUTHENTICATED );
                context.session.remove( ISSUER );
            }
        } );
    }
}
