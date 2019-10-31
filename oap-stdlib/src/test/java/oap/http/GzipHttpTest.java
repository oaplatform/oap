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
import oap.io.IoStreams;
import oap.testng.Env;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.emptyMap;
import static oap.io.IoStreams.Encoding.GZIP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 12.02.2019.
 */
public class GzipHttpTest {
    private int port;
    private Server server;
    private PlainHttpListener listener;
    private SynchronizedThread thread;

    @BeforeMethod
    public void beforeMethod() {
        port = Env.port( getClass().getName() );
        server = new Server( 1024, false );
        server.start();
        listener = new PlainHttpListener( server, port );
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
            ( request, response ) -> response.respond( HttpResponse.ok( "test", true, ContentTypes.TEXT_PLAIN ).response() ), Protocol.HTTP );

        thread.start();

        var response = Client.DEFAULT.get( "http://localhost:" + port + "/test" );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType ).isEqualTo( ContentTypes.TEXT_PLAIN );
        assertThat( response.contentString() ).isEqualTo( "test" );

        var responseGzip = Client.DEFAULT.get( "http://localhost:" + port + "/test",
            emptyMap(), Map.of( "Accept-encoding", "gzip" ) );

        assertThat( responseGzip.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentType ).isEqualTo( ContentTypes.TEXT_PLAIN );
        assertThat( IoStreams.asString( responseGzip.getInputStream(), GZIP ) ).isEqualTo( "test" );
    }
}
