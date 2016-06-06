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
import oap.http.HttpResponse;
import oap.http.PlainHttpListener;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.testng.Env;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.reset;

public class WebServiceLocalTest {
    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, new SessionManager( 10, null, "/" ),
       GenericCorsPolicy.DEFAULT, WsConfig.CONFIGURATION.fromResource( getClass(), "ws-local.conf" )
    );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Application.register( "test", new TestWS() );

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

    private static class TestWS {

        @WsMethod( path = "/text", method = GET )
        public HttpResponse text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return HttpResponse.ok( "ok" );
        }
    }
}
