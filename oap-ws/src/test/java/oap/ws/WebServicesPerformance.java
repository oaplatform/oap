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
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.testng.Env;
import oap.util.Lists;
import org.apache.http.entity.ContentType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static oap.benchmark.Benchmark.benchmark;

public class WebServicesPerformance {
    private static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );
    private final int samples = 100000;

    @BeforeMethod
    public void init() {
        Env.resetPorts();
    }

    @Test
    public void blockingThreads() {
        Server server = new Server( 5000, false );
        server.start();
        SynchronizedThread listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
        try {
            WebServices ws = new WebServices( new Kernel( Lists.empty() ), server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );
            ws.bind( "x/v/math", GenericCorsPolicy.DEFAULT, new MathWS(), false, SESSION_MANAGER,
                Collections.emptyList(), Protocol.HTTP );

            HttpAsserts.reset();
            benchmark( "Server.invocations", samples, () -> HttpAsserts.assertGet( HttpAsserts.httpUrl( "/x/v/math/id?a=aaa" ) ).responded( 200, "OK",
                ContentType.APPLICATION_JSON, "\"aaa\"" )
            ).inThreads( 5000 ).run();

            HttpAsserts.reset();
        } finally {
            listener.stop();
            server.stop();
        }
    }
}

