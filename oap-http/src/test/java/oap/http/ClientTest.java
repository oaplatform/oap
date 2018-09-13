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

import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.apache.http.entity.ContentType;
import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Slf4j
public class ClientTest extends AbstractTest {
    private static final int PORT = Env.port( ClientTest.class.toString() );
    private ClientAndServer mockServer;
    private Client.Response response;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        mockServer = startClientAndServer( PORT );
        response = null;
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        mockServer.stop( true );

        super.afterMethod();
    }

    @Test
    public void download() {
        mockServer
            .when(
                request()
                    .withMethod( "GET" )
                    .withPath( "/file" ),
                once()
            )
            .respond(
                response()
                    .withStatusCode( HTTP_OK )
                    .withBody( "test1" )
            );

        final Path path = Env.tmpPath( "new.file" );
        AtomicInteger progress = new AtomicInteger();
        final Optional<Path> download = Client.DEFAULT.download( "http://localhost:" + PORT + "/file",
            Optional.empty(), Optional.of( path ), progress::set );

        assertThat( download ).contains( path );
        assertThat( download ).isPresent();
        assertFile( path ).exists().hasSize( 5 );
        assertThat( progress.get() ).isEqualTo( 100 );
    }

    @Test
    public void downloadTempFile() {
        mockServer
            .when(
                request()
                    .withMethod( "GET" )
                    .withPath( "/file.gz" ),
                once()
            )
            .respond(
                response()
                    .withStatusCode( HTTP_OK )
                    .withBody( "test1" )
            );

        AtomicInteger progress = new AtomicInteger();
        final Optional<Path> download = Client.DEFAULT.download( "http://localhost:" + PORT + "/file.gz",
            Optional.empty(), Optional.empty(), progress::set );
        assertThat( download ).isPresent();
        assertFile( download.get() ).exists().hasSize( 5 );
        assertFile( download.get() ).hasExtension( "gz" );
        assertThat( progress.get() ).isEqualTo( 100 );
    }

    @Test
    public void testPostOutputStream() throws IOException, InterruptedException {
        mockServer.when( request()
                .withMethod( "POST" )
                .withPath( "/test" )
                .withBody( "test\ntest1" ),
            once()
        ).respond( response().withStatusCode( HTTP_OK ).withBody( "ok" ) );

        response = Client.DEFAULT.post( "http://localhost:" + PORT + "/test", os -> {
            os.write( "test".getBytes() );
            os.write( '\n' );
            os.write( "test1".getBytes() );

        }, ContentType.TEXT_PLAIN );

        assertThat( response ).isNotNull();
        assertThat( response.contentString() ).isEqualTo( "ok" );

    }
}
