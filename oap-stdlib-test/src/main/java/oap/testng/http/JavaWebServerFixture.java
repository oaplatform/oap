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

package oap.testng.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import oap.testng.AbstractFixture;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class JavaWebServerFixture extends AbstractFixture<JavaWebServerFixture> {
    private final LinkedHashMap<String, HttpHandler> contexts = new LinkedHashMap<>();
    private final int port;
    private HttpServer server;
    private int threads = 10;
    private int backlog = 0;
    private int delaySeconds = 1;

    public JavaWebServerFixture() {
        port = definePort( "HTTP_PORT" );
    }

    @SneakyThrows
    @Override
    protected void before() {
        super.before();

        var threadPoolExecutor = ( ThreadPoolExecutor ) Executors.newFixedThreadPool( threads );
        backlog = 0;
        server = HttpServer.create( new InetSocketAddress( "localhost", port ), backlog );

        contexts.forEach( ( path, context ) -> server.createContext( path, context ) );

        server.setExecutor( threadPoolExecutor );
        server.start();
    }

    @Override
    protected void after() {
        server.stop( delaySeconds );

        super.after();
    }

    public JavaWebServerFixture withThreads( int threads ) {
        this.threads = threads;

        return this;
    }

    public JavaWebServerFixture withBacklog( int backlog ) {
        this.backlog = backlog;

        return this;
    }

    public JavaWebServerFixture withHandler( String path, HttpHandler handler ) {
        contexts.put( path, handler );

        return this;
    }

    public JavaWebServerFixture withWaitUntilExchangesHaveFinishedSeconds( int delaySeconds ) {
        this.delaySeconds = delaySeconds;

        return this;
    }
}
