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

package oap.ws.interceptor;

import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Session;
import oap.reflect.Reflection;
import oap.testng.Fixtures;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.testng.WsFixture;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class InterceptorTest extends Fixtures {
    {
        fixture( new WsFixture( getClass(), ( ws, kernel ) -> {
            kernel.register( "test", new TestWS() );
            kernel.register( "pass-interceptor", new PassInterceptor() );
            kernel.register( "error-interceptor", new ErrorInterceptor() );

        }, "ws-interceptors.conf" ) );
    }

    @Test
    public void shouldAllowRequestWhenEmptyInterceptor() {
        assertGet( httpUrl( "/test/text?value=empty" ) )
            .isOk()
            .hasReason( "modified by interceptor" )
            .hasBody( "ok" );
    }

    @Test
    public void shouldNotAllowRequestWhenErrorInterceptor() {
        assertGet( httpUrl( "/test/text?value=error" ) )
            .hasCode( 403 )
            .hasBody( "caused by interceptor" );
    }

    @SuppressWarnings( "unused" )
    private static class TestWS {

        @WsMethod( path = "/text", method = GET, produces = "plain/text" )
        public String text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return "ok";
        }
    }

    private static class PassInterceptor implements Interceptor {
        @Override
        public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
            return Optional.empty();
        }

        @Override
        public HttpResponse after( HttpResponse response, Session session ) {
            return response.modify().withReason( "modified by interceptor" ).response();
        }
    }

    private static class ErrorInterceptor implements Interceptor {
        @Override
        public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
            return request.parameter( "value" ).filter( s -> s.equals( "error" ) ).isPresent()
                ? Optional.of(
                HttpResponse.status( 403 )
                    .withContent( "caused by interceptor", APPLICATION_JSON )
                    .response()
            ) : Optional.empty();
        }
    }
}
