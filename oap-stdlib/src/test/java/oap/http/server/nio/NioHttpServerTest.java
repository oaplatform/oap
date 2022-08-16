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
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static oap.http.Http.Headers.CONNECTION;
import static oap.http.Http.Headers.DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NioHttpServerTest extends Fixtures {
    private final EnvFixture fixture;

    public NioHttpServerTest() {
        fixture = fixture( new EnvFixture() );
    }

    @Test
    public void testResponseHeaders() throws IOException {
        int port = fixture.portFor( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( port ) ) {
            httpServer.start();

            Client.Response response = Client.DEFAULT.get( "http://localhost:" + port + "/" );

            assertThat( response.getHeaders() )
                .hasSize( 3 )
                .containsKey( DATE )
                .containsKey( CONNECTION );

        }

        try( NioHttpServer httpServer = new NioHttpServer( port ) ) {
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

        try( NioHttpServer httpServer = new NioHttpServer( port ) ) {
            httpServer.bind( "/test", exchange -> exchange.responseOk( "test", Http.ContentType.TEXT_PLAIN ), testPort );

            httpServer.start();

            Client.Response response = Client.DEFAULT.get( "http://localhost:" + testPort + "/test" );

            assertThat( response.contentString() ).isEqualTo( "test" );
        }
    }

    @Test
    public void testDenyBindAfterStart() throws IOException {
        int port = fixture.portFor( getClass() );
        int testPort = fixture.portFor( getClass() + "test" );

        assertThatThrownBy( () -> {
            try( NioHttpServer httpServer = new NioHttpServer( port ) ) {
                httpServer.start();

                httpServer.bind( "/test", exchange -> exchange.responseOk( "test", Http.ContentType.TEXT_PLAIN ), testPort );
            }
        } ).isInstanceOf( UncheckedIOException.class ).getCause().hasMessage( "Bind failed: Server is already running" );
    }
}
