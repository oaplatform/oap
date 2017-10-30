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

import oap.http.Context;
import oap.http.HttpResponse;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.Session;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * Created by igor.petrenko on 05.10.2017.
 */
public class SecurityInterceptorTest {

    private static final Reflection REFLECTION = Reflect.reflect( TestAPI.class );

    private final TokenService mockTokenService = mock( TokenService.class );

    private final SecurityInterceptor securityInterceptor = new SecurityInterceptor( mockTokenService );

    @Test
    public void testShouldNotCheckMethodWithoutAnnotation() {
        final Reflection.Method methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithoutAnnotation" ) ).get();

        final Optional<HttpResponse> httpResponse = securityInterceptor.intercept( null, null, methodWithAnnotation );

        assertFalse( httpResponse.isPresent() );
    }

    @Test
    public void testShouldVerifyUserIfPresentInSession() {
        final Reflection.Method methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithAnnotation" ) ).get();

        final User user = new DefaultUser( Role.ADMIN, "org", "test@test.com" );

        final Session session = new Session();
        session.set( "user", user );

        final Optional<HttpResponse> httpResponse = securityInterceptor.intercept( null,
            session, methodWithAnnotation );

        assertFalse( httpResponse.isPresent() );
    }

    @Test
    public void testShouldVerifyAndSetUserInSessionIfAuthorizationHeaderIsPresent() throws UnknownHostException {
        final Reflection.Method methodWithAnnotation = REFLECTION.method(
            method -> method.name().equals( "methodWithAnnotation" ) ).get();

        final Context context = new Context( "/", InetAddress.getLocalHost(), Protocol.HTTP.name() );
        final String tokenId = UUID.randomUUID().toString();

        final HttpRequest httpRequest = new HttpGet();
        httpRequest.setHeader( "Authorization", tokenId );
        httpRequest.setHeader( "Host", "localhost" );

        final Request request = new Request( httpRequest, context );

        final User user = new DefaultUser( Role.ADMIN, "testOrg", "test@example.com" );

        final Token token = new Token();
        token.user = new DefaultUser( user );
        token.id = tokenId;
        token.created = DateTime.now();

        when( mockTokenService.getToken( tokenId ) ).thenReturn( Optional.of( token ) );

        final Session session = new Session();
        final Optional<HttpResponse> httpResponse = securityInterceptor.intercept( request,
            session, methodWithAnnotation );

        assertFalse( httpResponse.isPresent() );
        assertNotNull( session.get( "user" ) );
    }

    private static class TestAPI {

        @WsSecurity( role = Role.USER )
        public void methodWithAnnotation() {}

        public void methodWithoutAnnotation() {}
    }
}