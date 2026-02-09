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

package oap.http.server.nio;

import oap.http.Http;
import oap.http.client.Client;
import oap.http.server.nio.handlers.BlockingReadTimeoutHandler;
import oap.http.server.nio.handlers.CompressionNioHandler;
import oap.http.server.nio.handlers.KeepaliveRequestsHandler;
import oap.http.server.nio.health.HealthHttpHandler;
import oap.io.Resources;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static oap.http.Http.Headers.CONNECTION;
import static oap.http.Http.Headers.DATE;
import static oap.http.test.HttpAsserts.assertGet;
import static org.assertj.core.api.Assertions.assertThat;

public class NioHttpServerTest extends Fixtures {
    @Test
    public void testResponseHeaders() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.start();

            assertGet( "http://localhost:" + port + "/" )
                .hasHeadersSize( 3 )
                .containsHeader( DATE )
                .containsHeader( CONNECTION );
        }

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.alwaysSetDate = false;
            httpServer.alwaysSetKeepAlive = false;
            httpServer.start();

            assertGet( "http://localhost:" + port + "/" )
                .hasHeadersSize( 1 )
                .doesNotContainHeader( DATE )
                .doesNotContainHeader( CONNECTION );
        }
    }

    @Test
    public void testBindToSpecificPort() throws IOException {
        int port = Ports.getFreePort( getClass() );
        int testPort = Ports.getFreePort( getClass() );
        int testPort2 = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.handlers.add( new KeepaliveRequestsHandler( 1000 ) );
            httpServer.handlers.add( new CompressionNioHandler() );
            httpServer.handlers.add( new BlockingReadTimeoutHandler( Dates.s( 60 ) ) );

            httpServer.additionalHttpPorts.put( "test", testPort );
            httpServer.additionalHttpPorts.put( "test2", testPort2 );

            httpServer.bind( "/test", exchange -> exchange.responseOk( "test", Http.ContentType.TEXT_PLAIN ), "test" );
            httpServer.bind( "/test2", exchange -> exchange.responseOk( "test2", Http.ContentType.TEXT_PLAIN ), "test2" );

            httpServer.start();
            httpServer.bind( "/test3", exchange -> exchange.responseOk( "test3", Http.ContentType.TEXT_PLAIN ), "test2" );

            assertGet( "http://localhost:" + testPort + "/test" ).body().isEqualTo( "test" );
            assertGet( "http://localhost:" + testPort2 + "/test2" ).body().isEqualTo( "test2" );
            assertGet( "http://localhost:" + testPort2 + "/test3" ).body().isEqualTo( "test3" );
        }
    }

    /**
     * keytool -genkey -alias ssl -keyalg RSA -keysize 2048 -dname "CN=localhost,OU=IT" -keystore master.jks -storepass 1234567 -keypass 1234567
     */
    @Test
    public void testHttps() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        ClientConnector connector = new ClientConnector();

        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setKeyStorePath( Resources.filePath( getClass(), "/oap/http/test_https.jks" ).get() );
        sslContextFactory.setKeyStorePassword( "1234567" );
        connector.setSslContextFactory( sslContextFactory );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( httpPort, httpsPort, Resources.urlOrThrow( getClass(), "/oap/http/test_https.jks" ), "1234567" ) );
             HttpClient httpClient = Client.customHttpClient( new HttpClientTransportDynamic( connector ) ) ) {
            httpClient.start();

            new TestHttpHandler( httpServer, "/test", "default-https" );
            new HealthHttpHandler( httpServer, "/healtz", "default-http" ).start();

            httpServer.start();

            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
            assertGet( "http://localhost:" + httpPort + "/healtz" ).hasCode( Http.StatusCode.NO_CONTENT );
        }
    }

    public static class TestHttpHandler implements HttpHandler {
        public TestHttpHandler( NioHttpServer server, String prefix, String port ) {
            server.bind( prefix, this, port );
        }

        @Override
        public void handleRequest( HttpServerExchange exchange ) {
            assertThat( Thread.currentThread().isVirtual() ).isTrue();

            exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
        }
    }
}
