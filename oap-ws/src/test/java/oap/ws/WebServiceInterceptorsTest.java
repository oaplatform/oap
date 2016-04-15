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

package oap.ws;

import oap.application.Application;
import oap.concurrent.SynchronizedThread;
import oap.http.*;
import oap.reflect.Reflection;
import oap.testng.Env;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.*;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class WebServiceInterceptorsTest {
    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, new SessionManager( 10, null, "/" ),
        WsConfig.CONFIGURATION.fromResource( getClass(), "ws-interceptors.conf" )
    );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Application.register( "test", new TestWS() );
        Application.register( "empty-interceptor", new EmptyInterceptor() );
        Application.register( "error-interceptor", new ErrorInterceptor() );

        ws.start();

        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        ws.stop();
        reset();
    }

    @Test
    public void testShouldAllowRequestWhenEmptyInterceptor() {
        assertGet( HTTP_PREFIX + "/test/text?value=empty" ).isOk().hasBody( "\"" + "ok" + "\"" );
    }

    @Test
    public void testShouldNotAllowRequestWhenErrorInterceptor() {
        assertGet( HTTP_PREFIX + "/test/text?value=error" )
            .hasCode( 403 )
            .hasBody( "\"" + "caused by interceptor" + "\"" );
    }

    private static class TestWS {

        @WsMethod( path = "/text", method = GET )
        public HttpResponse text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return HttpResponse.ok( "ok" );
        }
    }

    private static class EmptyInterceptor implements Interceptor {
        @Override
        public Optional<HttpResponse> intercept( Request request, Session session, Reflection.Method method ) {
            return Optional.empty();
        }
    }

    private static class ErrorInterceptor implements Interceptor {
        @Override
        public Optional<HttpResponse> intercept( Request request, Session session, Reflection.Method method ) {
            return request.parameter( "value" ).get().equals( "error" ) ?
                Optional.of( new HttpResponse( 403 ).withContent( "caused by interceptor", APPLICATION_JSON ) ) :
                Optional.empty();
        }
    }
}
