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

package oap.http.file;

import lombok.val;
import oap.io.FileSync;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.apache.http.client.utils.DateUtils;
import org.joda.time.DateTimeUtils;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpFileSyncTest extends AbstractTest {
    private static final int PORT = Env.port( HttpFileSyncTest.class.toString() );

    private ClientAndServer mockServer;

    @BeforeMethod
    public void start() {
        mockServer = startClientAndServer( PORT );
    }

    @AfterMethod
    public void stop() {
        mockServer.stop( true );
    }

    @Test
    public void testSync() throws Exception {
        val b = new StringBuilder();
        DateTimeUtils.setCurrentMillisFixed( 10 * 1000 );

        val date10 = new Date( 10 * 1000 );
        val date20 = new Date( 20 * 1000 );

        val localFile = Env.tmpPath( "ltest.file" );

        val fileSync = FileSync.create( "http://localhost:" + PORT + "/file", localFile );
        fileSync.addListener( path -> b.append( "f" ) );

        mockServer
            .reset()
            .when( request().withMethod( "GET" ).withPath( "/file" ), once() )
            .respond(
                response()
                    .withStatusCode( HTTP_OK )
                    .withBody( "test" )
                    .withHeader( Header.header( "Last-Modified", DateUtils.formatDate( date10 ) ) )
                    .withHeader( Header.header( "Content-Disposition", "inline; filename=\"test.file\"" ) )
            );

        fileSync.run();
        assertThat( localFile ).hasContent( "test" );
        assertThat( java.nio.file.Files.getLastModifiedTime( localFile ).toMillis() ).isEqualTo( 10L * 1000 );
        assertThat( b ).contains( "f" );

        mockServer
            .reset()
            .when( request().withMethod( "GET" ).withPath( "/file" )
                    .withHeader( Header.header( "If-Modified-Since", DateUtils.formatDate( date10 ) ) ),
                once() )
            .respond(
                response()
                    .withStatusCode( HTTP_NOT_MODIFIED )
                    .withHeader( Header.header( "Last-Modified", DateUtils.formatDate( date10 ) ) )
            );
        fileSync.run();
        assertThat( localFile ).hasContent( "test" );
        assertThat( b ).contains( "f" );

        mockServer
            .reset()
            .when( request().withMethod( "GET" ).withPath( "/file" )
                    .withHeader( Header.header( "If-Modified-Since", DateUtils.formatDate( date10 ) ) ),
                once() )
            .respond(
                response()
                    .withStatusCode( HTTP_OK )
                    .withBody( "test2" )
                    .withHeader( Header.header( "Last-Modified", DateUtils.formatDate( date20 ) ) )
                    .withHeader( Header.header( "Content-Disposition", "inline; filename=\"test.file\"" ) )
            );
        fileSync.run();
        assertThat( localFile ).hasContent( "test2" );
        assertThat( b ).contains( "ff" );
    }
}
