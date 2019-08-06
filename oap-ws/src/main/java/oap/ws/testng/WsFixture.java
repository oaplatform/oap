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

package oap.ws.testng;

import oap.application.Kernel;
import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Server;
import oap.io.Closeables;
import oap.testng.Env;
import oap.testng.Fixture;
import oap.util.Cuid;
import oap.util.Lists;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import oap.ws.WsConfig;

import java.io.Closeable;
import java.util.List;
import java.util.function.BiConsumer;

import static oap.http.cors.GenericCorsPolicy.DEFAULT;
import static oap.http.testng.HttpAsserts.reset;

public class WsFixture implements Fixture {

    private Class<?> contextClass;
    private final BiConsumer<WebServices, Kernel> registerServices;
    private final String[] configs;
    public TestWebServer server;

    public WsFixture( Class<?> contextClass, BiConsumer<WebServices, Kernel> registerServices, String... configs ) {
        this.contextClass = contextClass;
        this.registerServices = registerServices;
        this.configs = configs;
    }

    @Override
    public void beforeMethod() {
        server = new TestWebServer( Lists.of( configs ), registerServices );
    }

    @Override
    public void afterMethod() {
        Closeables.close( server );
    }

    public class TestWebServer implements Closeable {
        protected WebServices ws;
        private Server server;
        private SynchronizedThread listener;
        public Kernel kernel;

        private TestWebServer( List<String> configs, BiConsumer<WebServices, Kernel> registerServices ) {
            Env.resetPorts();
            kernel = new Kernel( List.of() );
            server = new Server( 100, false );
            server.start();
            ws = new WebServices( kernel, server, new SessionManager( 10, null, "/" ) {{
                this.cuid = Cuid.incremental( 0 );
            }}, DEFAULT, Lists.map( configs, n -> WsConfig.CONFIGURATION.fromResource( contextClass, n ) ) );

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
