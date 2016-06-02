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

import oap.concurrent.SynchronizedThread;
import oap.http.*;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.ws.security.User;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;

import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.SESSION;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.testng.Assert.assertEquals;

public class WebServicesSessionTest {

    private static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );

    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Metrics.resetAll();
        server.start();
        ws.bind( "test", GenericCorsPolicy.DEFAULT, new TestWS(), true, SESSION_MANAGER, Collections.emptyList(), Protocol.HTTP );

        PlainHttpListener http = new PlainHttpListener( server, Env.port() );
        listener = new SynchronizedThread( http );
        listener.start();
    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        server.unbind( "test" );

        HttpAsserts.reset();
        Metrics.resetAll();
    }

    @Test
    public void testShouldVerifySessionPropagation() throws IOException {
        final HttpGet httpGet = new HttpGet( HttpAsserts.HTTP_PREFIX + "/test/" );
        httpGet.addHeader( "Cookie", "Authorization=987654321; SID=123456" );

        final User user = new User();
        user.email = "test@example.com";

        final Session session = new Session();
        session.set( "user", user );

        SESSION_MANAGER.put( "123456", session );

        final CloseableHttpResponse response = HttpClientBuilder.create().build().execute( httpGet );

        assertEquals( response.getStatusLine().getStatusCode(), 200 );
    }

    private class TestWS {

        @WsMethod( path = "/", method = GET )
        public Object test( @WsParam( from = SESSION ) User user ) {
            return HttpResponse.ok( user.email, true, TEXT_PLAIN );
        }
    }
}
