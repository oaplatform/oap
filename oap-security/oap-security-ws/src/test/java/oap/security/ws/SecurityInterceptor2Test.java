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

import lombok.val;
import oap.http.Context;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.Session;
import oap.http.testng.MockRequest;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.security.acl.AclService;
import oap.util.Id;
import oap.ws.WsParam;
import org.apache.http.client.methods.HttpGet;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static oap.ws.Interceptor.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by igor.petrenko on 22.12.2017.
 */
public class SecurityInterceptor2Test {
    private static final Reflection REFLECTION = Reflect.reflect( TestAPI.class );

    private final TokenService2 mockTokenService = mock( TokenService2.class );
    private final AclService mockAclService = mock( AclService.class );

    private final SecurityInterceptor2 securityInterceptor = new SecurityInterceptor2( mockAclService, mockTokenService );

    @Test
    public void testShouldNotCheckMethodWithoutAnnotation() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithoutAnnotation" ) ).get();

        val httpResponse = securityInterceptor.intercept( null, null, methodWithAnnotation, p -> null );

        assertThat( httpResponse ).isEmpty();
    }

    @Test
    public void testShouldVerifyUserIfPresentInSession() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val userId = "testUser";

        final Session session = new Session();
        session.set( USER_ID, userId + "/token1" );

        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( true );

        final MockRequest request = new MockRequest();
        request.headers.put( "authorization", "token1" );
        val httpResponse = securityInterceptor.intercept( request,
            session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isEmpty();
    }

    @Test
    public void testShouldVerifyAndSetUserInSessionIfAuthorizationHeaderIsPresent() throws UnknownHostException {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val context = new Context( "/", InetAddress.getLocalHost(), Protocol.HTTP.name() );
        val tokenId = UUID.randomUUID().toString();

        val httpRequest = new HttpGet();
        httpRequest.setHeader( "Authorization", tokenId );
        httpRequest.setHeader( "Host", "localhost" );

        val request = new Request( httpRequest, context );

        val userId = "testUser";

        val token = new Token2( tokenId, userId, DateTimeUtils.currentTimeMillis() );

        when( mockTokenService.getToken( tokenId ) ).thenReturn( Optional.of( token ) );

        val session = new Session();
        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( true );

        val httpResponse = securityInterceptor.intercept( request,
            session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isEmpty();
        assertThat( session.get( USER_ID ) ).contains( userId + "/" + tokenId );
    }

    @Test
    public void testAccessDenied() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val userId = "testUser";

        final Session session = new Session();
        session.set( USER_ID, userId + "/1" );

        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( false );

        val httpResponse = securityInterceptor.intercept( new MockRequest(), session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isPresent();
    }

    @Test
    public void testPostProcessing() {
        when( mockAclService.checkAll( "1", "testUser" ) ).thenReturn( asList( "test1.read" ) );

        final Session session = new Session();
        session.set( USER_ID, "testUser/1" );

        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();
        val op = ( ObjectWithPermissions ) securityInterceptor.postProcessing( new TestAPI.Res( "1" ), session, methodWithAnnotation );
        assertThat( op.permissions ).containsExactlyInAnyOrder( "test1.read" );
    }

    @Test
    public void testPostProcessingList() {
        when( mockAclService.checkAll( "1", "testUser" ) ).thenReturn( asList( "test1.read" ) );
        when( mockAclService.checkAll( AclService.ROOT, "testUser" ) ).thenReturn( asList( "gl.create" ) );

        final Session session = new Session();
        session.set( USER_ID, "testUser/1" );

        val methodList = REFLECTION.method( method -> method.name().equals( "methodList" ) ).get();
        val op = ( ( List<ObjectWithPermissions> ) securityInterceptor.postProcessing( singletonList( new TestAPI.Res( "1" ) ), session, methodList ) ).get( 0 );
        assertThat( op.permissions ).containsExactlyInAnyOrder( "test1.read" );
    }

    @Test
    public void testPostProcessingIncludeRootPermissions() {
        when( mockAclService.checkAll( "1", "testUser" ) ).thenReturn( asList( "test1.read" ) );
        when( mockAclService.checkAll( AclService.ROOT, "testUser" ) ).thenReturn( asList( "gl.create" ) );

        final Session session = new Session();
        session.set( USER_ID, "testUser/1" );

        val methodWithAnnotation2 = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation2" ) ).get();
        val op = ( ObjectWithPermissions ) securityInterceptor.postProcessing( new TestAPI.Res( "1" ), session, methodWithAnnotation2 );
        assertThat( op.permissions ).containsExactlyInAnyOrder( "test1.read", "gl.create" );
    }

    private static class TestAPI {
        @WsSecurity2( object = "{parent}", permission = "parent.read" )
        @WsSecurityWithPermissions()
        public Res methodWithAnnotation( @WsParam String parent ) {
            return new Res( "1" );
        }

        @WsSecurity2( object = "{parent}", permission = "parent.read" )
        @WsSecurityWithPermissions( includeRootPermissions = true )
        public Res methodWithAnnotation2( @WsParam String parent ) {
            return new Res( "1" );
        }

        @WsSecurityWithPermissions()
        public List<Res> methodList( @WsParam String parent ) {
            return singletonList( new Res( "1" ) );
        }

        public void methodWithoutAnnotation() {}

        public static class Res {
            @Id
            private final String id;

            public Res( String id ) {
                this.id = id;
            }
        }
    }
}