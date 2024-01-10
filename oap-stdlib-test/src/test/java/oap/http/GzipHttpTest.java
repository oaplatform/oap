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

import io.github.bucket4j.Bandwidth;
import oap.http.server.nio.NioHttpServer;
import oap.io.IoStreams;
import oap.io.content.ContentWriter;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.compression.Compression.ContentWriter.ofGzip;
import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.Headers.ACCEPT_ENCODING;
import static oap.http.Http.Headers.CONTENT_ENCODING;
import static oap.http.Http.StatusCode.TOO_MANY_REQUESTS;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.assertj.core.api.Assertions.assertThat;

public class GzipHttpTest extends Fixtures {
    private final EnvFixture envFixture;
    private NioHttpServer server;

    public GzipHttpTest() {
        envFixture = fixture( new EnvFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        server = new NioHttpServer( new NioHttpServer.DefaultPort( envFixture.portFor( getClass() ) ) );
    }

    @AfterMethod
    public void afterMethod() {
        server.preStop();
    }

    @Test
    public void singleBandwidth() {
        server.bandwidths.clear();
        server.bandwidths.add( Bandwidth.simple( 2, Duration.ofMinutes( 1 ) ) );
        server.bind( "test", exchange ->
                exchange.responseOk( "test", true, TEXT_PLAIN )
        );
        server.start();

        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
    }

    @Test
    public void coupleBandwidths() throws Exception {
        server.bandwidths.clear();
        server.bandwidths.add( Bandwidth.simple( 2, Duration.ofMinutes( 1 ) ) );
        server.bandwidths.add( Bandwidth.simple( 1, Duration.ofSeconds( 2 ) ) );
        server.bind( "test", exchange ->
                exchange.responseOk( "test", true, TEXT_PLAIN )
        );
        server.start();

        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
        Thread.currentThread().sleep( 2_100L );
        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( HTTP_OK );
        Thread.currentThread().sleep( 2_100L );
        assertThat( Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
    }

    @Test
    public void gzipOutput() {
        server.bind( "test", exchange ->
                exchange.responseOk( "test", true, TEXT_PLAIN )
        );
        server.start();

        var responseHtml = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" );

        assertThat( responseHtml.code ).isEqualTo( HTTP_OK );
        assertThat( responseHtml.contentType ).isEqualTo( TEXT_PLAIN );
        assertThat( responseHtml.contentString() ).isEqualTo( "test" );

        var responseGzip = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test",
            Map.of(), Map.of( ACCEPT_ENCODING, "gzip,deflate" ) );

        assertThat( responseGzip.code ).isEqualTo( HTTP_OK );
        assertThat( responseGzip.contentType ).isEqualTo( TEXT_PLAIN );
        assertThat( IoStreams.asString( responseGzip.getInputStream(), GZIP ) ).isEqualTo( "test" );
    }

    @Test
    public void gzipInput() {
        server.bind( "test", exchange ->
            exchange.responseOk( new String( exchange.readBody() ), true, TEXT_PLAIN )
        );
        server.start();

        var response = Client.DEFAULT.post( "http://localhost:" + envFixture.portFor( getClass() ) + "/test",
            ContentWriter.write( "test2", ofGzip() ), TEXT_PLAIN, Map.of( CONTENT_ENCODING, "gzip" ) );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType ).isEqualTo( TEXT_PLAIN );
        assertThat( IoStreams.asString( response.getInputStream(), PLAIN ) ).isEqualTo( "test2" );
    }
}
