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

package oap.ws.validate;

import oap.application.Kernel;
import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.util.Lists;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.Collections;
import java.util.List;

public abstract class AbstractWsValidateTest {
    private static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );

    private Server server;
    private WebServices ws;
    private SynchronizedThread listener;


    @BeforeClass
    public void start() {
        Env.resetPorts();
        Metrics.resetAll();

        server = new Server( 100, false );
        server.start();
        ws = new WebServices( new Kernel( Lists.empty() ), server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );

        for( var wsInstance : getWsInstances() )
            ws.bind( "test", GenericCorsPolicy.DEFAULT, wsInstance, false, SESSION_MANAGER, Collections.emptyList(), Protocol.HTTP );

        PlainHttpListener http = new PlainHttpListener( server, Env.port() );
        listener = new SynchronizedThread( http );
        listener.start();
    }

    protected abstract List<Object> getWsInstances();

    @AfterClass
    public void stop() {
        listener.stop();
        server.stop();
        server.unbind( "test" );

        HttpAsserts.reset();
        Metrics.resetAll();
    }
}
