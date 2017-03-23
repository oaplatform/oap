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
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.http.nio.NioServer;
import oap.http.testng.HttpAsserts;
import oap.testng.AbstractPerformance;
import oap.testng.Env;
import org.apache.http.entity.ContentType;
import org.testng.annotations.Test;

import java.util.Collections;

import static oap.http.testng.HttpAsserts.HTTP_PREFIX;

public class WebServicesPerformance extends AbstractPerformance {
    public static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );
    private final int samples = 100000;

    @Test
    public void blocking_threads() {
        Server server = new Server( 100 );
        SynchronizedThread listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
        try {
            WebServices ws = new WebServices( server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );
            ws.bind( "x/v/math", GenericCorsPolicy.DEFAULT, new MathWS(), false, SESSION_MANAGER,
                Collections.emptyList(), Protocol.HTTP );

            HttpAsserts.reset();
            benchmark( builder( "Server.invocations" ).samples( samples ).threads( 5000 ).build(),
                number -> HttpAsserts.assertGet( HTTP_PREFIX + "/x/v/math/id?a=aaa" ).responded( 200, "OK",
                    ContentType.APPLICATION_JSON, "\"aaa\"" ) );

            HttpAsserts.reset();
        } finally {
            listener.stop();
            server.stop();
        }
    }

    @Test
    public void nio_threads() throws Exception {
        NioServer server = new NioServer( Env.port() );
        try {
            WebServices ws = new WebServices( server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );
            ws.bind( "x/v/math", GenericCorsPolicy.DEFAULT, new MathWS(), false, SESSION_MANAGER,
                Collections.emptyList(), Protocol.HTTP );
            server.start();
            Thread.sleep( 3000 ); // ??? TODO: fix me

            HttpAsserts.reset();
            benchmark( builder( "NioServer.invocations" ).samples( samples ).threads( 5000 ).build(), ( number ) -> {
                try {
                    HttpAsserts.assertGet( HTTP_PREFIX + "/x/v/math/id?a=aaa" )
                        .responded( 200, "OK", ContentType.APPLICATION_JSON, "\"aaa\"" );
                } catch( Throwable e ) {
                    e.printStackTrace();
                }
            } );

            HttpAsserts.reset();
        } finally {
            server.stop();
        }
    }
}

