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

import oap.http.Context;
import oap.http.HttpResponse;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.ServerHttpContext;
import oap.http.Session;
import oap.http.testng.MockHttpContext;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.sso.DefaultUser;
import oap.sso.PrecedenceRoleService;
import oap.sso.Token;
import oap.sso.TokenService;
import oap.sso.User;
import org.apache.http.client.methods.HttpGet;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class SecurityInterceptorTest {

    private static final Reflection REFLECTION = Reflect.reflect( TestAPI.class );

    private final TokenService mockTokenService = mock( TokenService.class );

    private final PrecedenceRoleService precedenceRoleService = mock( PrecedenceRoleService.class );

    private final SecurityInterceptor securityInterceptor = new SecurityInterceptor( mockTokenService, precedenceRoleService );

    @Test
    public void shouldNotCheckMethodWithoutAnnotation() {
        final Reflection.Method methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithoutAnnotation" ) ).get();

        final Optional<HttpResponse> httpResponse = securityInterceptor.intercept( null, null, methodWithAnnotation, p -> null );

        assertFalse( httpResponse.isPresent() );
    }

    @Test
    public void shouldVerifyUserIfPresentInSession() {
        final Reflection.Method methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithAnnotation" ) ).get();

        final User user = new DefaultUser( "ADMIN", "org", "test@test.com" );

        final Session session = new Session();
        session.set( "user", user );

        final Optional<HttpResponse> httpResponse = securityInterceptor.intercept( null,
            session, methodWithAnnotation, ( p ) -> null );

        assertFalse( httpResponse.isPresent() );
    }

    @Test
    public void shouldVerifyAndSetUserInSessionIfAuthorizationHeaderIsPresent() throws UnknownHostException {
        var methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithAnnotation" ) ).get();

        var context = new Context( "/", InetAddress.getLocalHost(), new ServerHttpContext( new MockHttpContext(), Protocol.HTTP, null ) );
        var tokenId = UUID.randomUUID().toString();

        var httpRequest = new HttpGet();
        httpRequest.setHeader( "Authorization", tokenId );
        httpRequest.setHeader( "Host", "localhost" );

        var request = new Request( httpRequest, context );

        var user = new DefaultUser( "ADMIN", "testOrg", "test@example.com" );

        var token = new Token();
        token.user = new DefaultUser( user );
        token.id = tokenId;
        token.created = LocalDateTime.now();

        when( mockTokenService.getToken( tokenId ) ).thenReturn( Optional.of( token ) );

        var session = new Session();
        var httpResponse = securityInterceptor.intercept( request,
            session, methodWithAnnotation, p -> null );

        assertFalse( httpResponse.isPresent() );
        assertNotNull( session.get( "user" ) );
    }

    private static class TestAPI {

        @WsSecurity( role = "USER" )
        public void methodWithAnnotation() {}

        public void methodWithoutAnnotation() {}
    }
}
