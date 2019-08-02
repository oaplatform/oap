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

import oap.application.Kernel;
import oap.concurrent.SynchronizedThread;
import oap.http.HttpResponse;
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.Session;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.util.Cuid;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.WsParam.From.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class WebServiceSessionTest {

    public static final Cuid.IncrementalCuid INCREMENTAL = Cuid.incremental( 0 );
    private final SessionManager sessionManager = new SessionManager( 10, null, "/" ) {{
        this.cuid = INCREMENTAL;
    }};

    private Server server;
    private WebServices ws;

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Env.resetPorts();
        Metrics.resetAll();
        server = new Server( 100, false );
        server.start();

        ws = new WebServices( new Kernel( Lists.empty() ), server, sessionManager, GenericCorsPolicy.DEFAULT );

        ws.bind( "test", GenericCorsPolicy.DEFAULT, new TestWS(), true, sessionManager, Lists.empty(), Protocol.HTTP );

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


    @BeforeMethod
    public void restSessionId() {
        INCREMENTAL.reset( 0 );
    }

    @Test
    public void sessionViaResponse() {
        assertGet( httpUrl( "/test/put" ), Maps.of( __( "value", "vvv" ) ), Maps.empty() )
            .hasCode( 204 );
        assertThat( sessionManager.get( "1" ) ).isNotNull();
        assertGet( httpUrl( "/test/get" ), Maps.empty(), Maps.of( __( "Cookie", "SID=1" ) ) )
            .hasCode( 200 )
            .hasBody( "vvv" );

    }

    @Test
    public void sessionDirectly() {
        assertGet( httpUrl( "/test/putDirectly" ), Maps.of( __( "value", "vvv" ) ), Maps.empty() )
            .hasCode( 204 );
        assertThat( sessionManager.get( "1" ) ).isNotNull();
        assertGet( httpUrl( "/test/get" ), Maps.empty(), Maps.of( __( "Cookie", "SID=1" ) ) )
            .hasCode( 200 )
            .hasBody( "vvv" );

    }

    @SuppressWarnings( "unused" )
    private class TestWS {

        public static final String IN_SESSION = "inSession";

        public HttpResponse put( String value ) {
            return HttpResponse.status( HTTP_NO_CONTENT )
                .withSessionValue( IN_SESSION, value )
                .response();
        }

        public void putDirectly( String value, Session session ) {
            session.set( IN_SESSION, value );
        }

        @WsMethod( path = "/get", method = GET, produces = "text/plain" )
        public String get( @WsParam( from = SESSION ) String inSession ) {
            return inSession;
        }
    }
}
