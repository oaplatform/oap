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

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import oap.concurrent.SynchronizedThread;
import oap.http.*;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.Env;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.SESSION;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class WebServicesSessionTest {

    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, null );
    private final SessionManager sessionManager = new SessionManager();

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Metrics.resetAll();
        server.start();
        ws.bind( "login", Cors.DEFAULT, new LoginWS(), true, sessionManager, Protocol.HTTP );

        PlainHttpListener http = new PlainHttpListener( server, Env.port() );
        listener = new SynchronizedThread( http );
        listener.start();
    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        server.unbind( "login" );

        HttpAsserts.reset();
        Metrics.resetAll();
    }

    @Test
    public void testShouldVerifySessionPropagation() throws IOException {
        final BasicCookieStore basicCookieStore = new BasicCookieStore();
        final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultCookieStore( basicCookieStore )
            .build();

        final HttpGet httpGet = new HttpGet( HttpAsserts.HTTP_PREFIX + "/login/" );

        final CloseableHttpResponse response = client.execute( httpGet );

        final Cookie cookie = Iterables.getOnlyElement( basicCookieStore.getCookies() );

        assertEquals( "Session",cookie.getName() );
        assertNotNull( cookie.getValue() );
        assertEquals( response.getStatusLine().getStatusCode(), 200 );

        String session;
        try (InputStream inputStream = response.getEntity().getContent()) {
            final byte[] bytes = ByteStreams.toByteArray( inputStream );
            session = new String( bytes, "UTF-8" );
        }
        final Session sessionById = sessionManager.getSessionById( session );
        final Optional<Object> username = sessionById.get( "username" );

        assertNotNull( sessionById );
        assertTrue( username.isPresent() );
    }

    private class LoginWS {

        @WsMethod( path = "/", method = GET )
        public Object login( @WsParam( from = SESSION ) String session ) {
            sessionManager.putSessionData( session, "username","test" );

            return HttpResponse.ok( session, true, TEXT_PLAIN );
        }
    }

}
