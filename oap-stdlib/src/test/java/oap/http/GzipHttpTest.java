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

import oap.concurrent.SynchronizedThread;
import oap.http.cors.GenericCorsPolicy;
import oap.http.server.apache.ApacheHttpServer;
import oap.io.IoStreams;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.io.IoStreams.Encoding.GZIP;
import static org.assertj.core.api.Assertions.assertThat;

public class GzipHttpTest extends Fixtures {
    private ApacheHttpServer server;
    private SynchronizedThread thread;

    private final EnvFixture envFixture;

    {
        envFixture = fixture( new EnvFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        server = new ApacheHttpServer( 1024, 0, false );
        server.start();
        PlainHttpListener listener = new PlainHttpListener( server, envFixture.portFor( getClass() ) );
        thread = new SynchronizedThread( listener, 5000 );
        listener.readyListener( thread );
    }

    @AfterMethod
    public void afterMethod() {
        thread.stop();
        server.stop();
    }

    @Test
    public void gzipOutput() {
        server.bind( "test", GenericCorsPolicy.DEFAULT,
            ( request, response ) -> response.respond( HttpResponse.ok( "test", true, TEXT_PLAIN ).response() ), Protocol.HTTP );

        thread.start();

        var response = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test" );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType.toString() ).isEqualTo( TEXT_PLAIN.toString() );
        assertThat( response.contentString() ).isEqualTo( "test" );

        var responseGzip = Client.DEFAULT.get( "http://localhost:" + envFixture.portFor( getClass() ) + "/test",
            Map.of(), Map.of( "Accept-encoding", "gzip,deflate" ) );

        assertThat( responseGzip.code ).isEqualTo( HTTP_OK );
        assertThat( responseGzip.header( "Content-encoding" ).get() ).isEqualTo( "gzip,deflate" );
        assertThat( IoStreams.asString( responseGzip.getInputStream(), GZIP ) ).isEqualTo( "test" );
    }
}
