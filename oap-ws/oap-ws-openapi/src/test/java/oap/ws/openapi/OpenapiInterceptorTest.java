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

package oap.ws.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponses;
import oap.http.server.nio.HttpServerExchange.HttpMethod;
import oap.ws.InvocationContext;
import oap.ws.Response;
import oap.ws.WsConfig;
import oap.ws.WsMethod;
import oap.ws.interceptor.Interceptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenapiInterceptorTest {
    private OpenAPI api;

    @BeforeClass
    public void generate() {
        OpenapiGenerator generator = new OpenapiGenerator( "title", "description" );
        generator.processWebservice( Fixture.class, "ctx", List.of( Interceptor401.class, Interceptor403.class ) );
        api = generator.build();
    }

    @Test
    public void interceptorCodesAreAddedToEveryOperation() {
        for( String name : List.of( "a", "b" ) ) {
            ApiResponses responses = api.getPaths().get( "/ctx/" + name ).getGet().getResponses();

            assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "401", "403" );
            assertThat( responses.get( "401" ).get$ref() ).isEqualTo( "#/components/responses/UnauthorizedError" );
        }
    }

    @Test
    public void interceptorCodesAreMergedWithMethodCodes() {
        ApiResponses responses = api.getPaths().get( "/ctx/c" ).getGet().getResponses();

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "401", "403", "404" );
    }

    @Test
    public void noInterceptorsMeansNoInterceptorCodes() {
        OpenapiGenerator generator = new OpenapiGenerator( "title", "description" );
        generator.processWebservice( Fixture.class, "plain" );
        OpenAPI plain = generator.build();

        assertThat( plain.getPaths().get( "/plain/a" ).getGet().getResponses().keySet() ).containsExactly( "200" );
    }

    @Test
    public void walkerResolvesInterceptorClassesFromModuleConfig() {
        Map<Class<?>, List<Class<?>>> interceptors = new HashMap<>();
        WebServiceVisitor visitor = new WebServiceVisitor() {
            @Override
            public void visit( WsConfig wsService, Class<?> aClass, String basePath, List<Class<?>> classes ) {
                interceptors.put( aClass, classes );
            }

            @Override
            public List<URL> getWebServiceUrls() {
                return List.of( OpenapiInterceptorTest.class.getResource( "/oap/ws/openapi/walker/oap-module.oap" ) );
            }
        };

        WebServicesWalker.walk( visitor );

        assertThat( interceptors.get( Fixture.class ) ).containsExactly( Interceptor401.class, Interceptor403.class );
    }

    public static class Fixture {
        @WsMethod( path = "/a", method = HttpMethod.GET )
        public Response a() {
            return Response.ok();
        }

        @WsMethod( path = "/b", method = HttpMethod.GET )
        public void b() {
        }

        @WsMethod( path = "/c", method = HttpMethod.GET )
        public Response c() {
            return Response.build404().build();
        }
    }

    public static class Interceptor401 implements Interceptor {
        @Override
        public Optional<Response> before( InvocationContext context ) {
            return Optional.of( Response.build401().message( "no token" ).build() );
        }
    }

    public static class Interceptor403 implements Interceptor {
        @Override
        public Optional<Response> before( InvocationContext context ) {
            return Optional.of( Response.build403().message( "no access" ).build() );
        }
    }
}
