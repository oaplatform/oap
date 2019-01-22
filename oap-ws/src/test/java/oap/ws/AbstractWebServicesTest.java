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
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static oap.http.testng.HttpAsserts.reset;

/**
 * Created by igor.petrenko on 16.02.2017.
 */
public class AbstractWebServicesTest extends AbstractTest {
    protected WebServices ws;
    private Server server;
    private SynchronizedThread listener;
    private Kernel kernel;

    @BeforeClass
    public void startServer() {
        Env.resetPorts();
        kernel = new Kernel( emptyList() );
        server = new Server( 100 );
        ws = new WebServices( kernel, server, new SessionManager( 10, null, "/" ),
            GenericCorsPolicy.DEFAULT,
            Lists.map( getConfig(), n -> WsConfig.CONFIGURATION.fromResource( getClass(), n ) )
        );

        kernel.start();
        registerServices( kernel );
        ws.start();
        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    protected List<String> getConfig() {
        return asList( "ws.json", "ws.conf" );
    }

    protected void registerServices( Kernel kernel ) {

    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        ws.stop();
        kernel.stop();
        reset();
    }
}
