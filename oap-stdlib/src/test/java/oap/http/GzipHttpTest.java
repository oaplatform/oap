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

package oap.http;

import oap.compression.CompressionUtils;
import oap.http.server.nio.NioHttpServer;
import oap.io.IoStreams;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.Headers.ACCEPT_ENCODING;
import static oap.http.Http.Headers.CONTENT_ENCODING;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.assertj.core.api.Assertions.assertThat;

public class GzipHttpTest extends Fixtures {
    private final EnvFixture envFixture;
    private NioHttpServer server;

    {
        envFixture = fixture( new EnvFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        server = new NioHttpServer( envFixture.portFor( getClass() ) );
        server.start();
    }

    @AfterMethod
    public void afterMethod() {
        server.preStop();
    }

    @Test
    public void gzipOutput() {
        server.bind( "test", exchange ->
            exchange.responseOk( "test", true, TEXT_PLAIN )
        );

        var response = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType ).isEqualTo( TEXT_PLAIN );
        assertThat( response.contentString() ).isEqualTo( "test" );

        var responseGzip = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test",
            Map.of(), Map.of( ACCEPT_ENCODING, "gzip,deflate" ) );

        assertThat( responseGzip.code ).isEqualTo( HTTP_OK );
        assertThat( responseGzip.contentType ).isEqualTo( TEXT_PLAIN );
        assertThat( IoStreams.asString( responseGzip.getInputStream(), GZIP ) ).isEqualTo( "test" );
    }

    @Test
    public void gzipInput() throws IOException {
        server.bind( "test", exchange ->
            exchange.responseOk( new String( exchange.readBody() ), true, TEXT_PLAIN )
        );

        var response = Client.DEFAULT.post( "http://localhost:" + envFixture.portFor( getClass() ) + "/test",
            CompressionUtils.gzip( "test2" ), TEXT_PLAIN, Map.of( CONTENT_ENCODING, "gzip" ) );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType.toString() ).isEqualTo( TEXT_PLAIN );
        assertThat( IoStreams.asString( response.getInputStream(), PLAIN ) ).isEqualTo( "test2" );
    }
}
