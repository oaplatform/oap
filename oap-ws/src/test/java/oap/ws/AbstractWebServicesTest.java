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

import java.io.Closeable;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;
import static oap.http.testng.HttpAsserts.reset;

public class AbstractWebServicesTest extends AbstractTest {
    protected TestWebServer webServer() {
        return webServer( ( ws, k ) -> {} );
    }

    protected TestWebServer webServer( BiConsumer<WebServices, Kernel> registerServices ) {
        return webServer( registerServices, "ws.json", "ws.conf" );
    }

    protected TestWebServer webServer( String... configs ) {
        return webServer( ( ws, k ) -> {}, configs );
    }

    protected TestWebServer webServer( BiConsumer<WebServices, Kernel> registerServices, String... configs ) {
        return new TestWebServer( List.of( configs ), registerServices );
    }

    protected class TestWebServer implements Closeable {
        protected WebServices ws;
        private Server server;
        private SynchronizedThread listener;
        private Kernel kernel;

        private TestWebServer( List<String> configs, BiConsumer<WebServices, Kernel> registerServices ) {
            Env.resetPorts();
            kernel = new Kernel( emptyList() );
            server = new Server( 100, false );
            ws = new WebServices( kernel, server, new SessionManager( 10, null, "/" ),
                GenericCorsPolicy.DEFAULT,
                Lists.map( configs, n -> WsConfig.CONFIGURATION.fromResource( getClass(), n ) )
            );

            kernel.start();
            registerServices.accept( ws, kernel );
            ws.start();
            listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
            listener.start();
        }

        @Override
        public void close() {
            listener.stop();
            server.stop();
            ws.stop();
            kernel.stop();
            reset();
        }
    }
}
