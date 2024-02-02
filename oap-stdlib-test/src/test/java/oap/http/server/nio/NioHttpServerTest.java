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

import oap.http.Client;
import oap.http.Http;
import oap.http.server.nio.health.HealthHttpHandler;
import oap.io.Resources;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.io.IOException;

import static oap.http.Http.Headers.CONNECTION;
import static oap.http.Http.Headers.DATE;
import static oap.http.testng.HttpAsserts.assertGet;
import static org.assertj.core.api.Assertions.assertThat;

public class NioHttpServerTest extends Fixtures {
    private final EnvFixture fixture = fixture( new EnvFixture() );

    public NioHttpServerTest() {
    }

    @Test
    public void testResponseHeaders() throws IOException {
        int port = fixture.portFor( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.start();

            Client.Response response = Client.DEFAULT.get( "http://localhost:" + port + "/" );

            assertThat( response.getHeaders() )
                .hasSize( 3 )
                .containsKey( DATE )
                .containsKey( CONNECTION );
        }

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.alwaysSetDate = false;
            httpServer.alwaysSetKeepAlive = false;
            httpServer.start();

            Client.Response response = Client.DEFAULT.get( "http://localhost:" + port + "/" );

            assertThat( response.getHeaders() )
                .hasSize( 1 )
                .doesNotContainKey( DATE )
                .doesNotContainKey( CONNECTION );
        }
    }

    @Test
    public void testBindToSpecificPort() throws IOException {
        int port = fixture.portFor( getClass() );
        int testPort = fixture.portFor( getClass() + "test" );
        int testPort2 = fixture.portFor( getClass() + "test2" );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.additionalHttpPorts.put( "test", testPort );
            httpServer.additionalHttpPorts.put( "test2", testPort2 );

            httpServer.bind( "/test", exchange -> exchange.responseOk( "test", Http.ContentType.TEXT_PLAIN ), "test" );
            httpServer.bind( "/test2", exchange -> exchange.responseOk( "test2", Http.ContentType.TEXT_PLAIN ), "test2" );

            httpServer.start();
            httpServer.bind( "/test3", exchange -> exchange.responseOk( "test3", Http.ContentType.TEXT_PLAIN ), "test2" );

            assertThat( Client.DEFAULT.get( "http://localhost:" + testPort + "/test" ).contentString() ).isEqualTo( "test" );
            assertThat( Client.DEFAULT.get( "http://localhost:" + testPort2 + "/test2" ).contentString() ).isEqualTo( "test2" );
            assertThat( Client.DEFAULT.get( "http://localhost:" + testPort2 + "/test3" ).contentString() ).isEqualTo( "test3" );
        }
    }

    /**
     * keytool -genkey -alias ssl -keyalg RSA -keysize 2048 -dname "CN=localhost,OU=IT" -keystore master.jks -storepass 1234567 -keypass 1234567
     */
    @Test
    public void testHttps() throws IOException {
        int httpPort = fixture.portFor( "TEST_HTTP_PORT" );
        int httpsPort = fixture.portFor( "TEST_HTTPS_PORT" );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( httpPort, httpsPort, Resources.urlOrThrow( getClass(), "/oap/http/test_https.jks" ), "1234567" ) );
             Client client = Client
                 .custom( Resources.filePath( getClass(), "/oap/http/test_https.jks" ).get(), "1234567", 10000, 10000 )
                 .build() ) {

            new TestHttpHandler( httpServer, "/test", "default-https" );
            new HealthHttpHandler( httpServer, "/healtz", "default-http" ).start();

            httpServer.start();

            assertThat( client.get( "https://localhost:" + httpsPort + "/test" )
                .contentString() ).isEqualTo( "ok" );

            assertGet( "http://localhost:" + httpPort + "/healtz" ).hasCode( Http.StatusCode.NO_CONTENT );

        }
    }

    public static class TestHttpHandler implements HttpHandler {
        public TestHttpHandler( NioHttpServer server, String prefix, String port ) {
            server.bind( prefix, this, port );
        }

        @Override
        public void handleRequest( HttpServerExchange exchange ) {
            exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
        }
    }
}