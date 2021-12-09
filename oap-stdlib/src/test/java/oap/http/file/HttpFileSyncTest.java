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

import oap.http.HttpStatusCodes;
import oap.io.AbstractFileSync;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import org.apache.http.client.utils.DateUtils;
import org.joda.time.DateTimeUtils;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpFileSyncTest extends Fixtures {
    private static final String PORT = HttpFileSyncTest.class.toString();
    private final EnvFixture envFixture;
    private ClientAndServer mockServer;

    {
        fixture( SystemTimerFixture.FIXTURE );
        fixture( TestDirectoryFixture.FIXTURE );
        envFixture = fixture( new EnvFixture() );
    }

    @BeforeMethod
    public void start() {
        mockServer = ClientAndServer.startClientAndServer( envFixture.portFor( PORT ) );
    }

    @AfterMethod
    public void stop() {
        mockServer.stop( true );
    }

    @Test
    public void sync() throws Exception {
        var b = new StringBuilder();
        DateTimeUtils.setCurrentMillisFixed( 10 * 1000 );

        var date10 = new Date( 10 * 1000 );
        var date20 = new Date( 20 * 1000 );

        var localFile = TestDirectoryFixture.testPath( "ltest.file" );

        var fileSync = AbstractFileSync.create( "http://localhost:" + envFixture.portFor( PORT ) + "/file", localFile );
        fileSync.addListener( path -> b.append( "f" ) );

        mockServer
            .reset()
            .when( HttpRequest.request().withMethod( "GET" ).withPath( "/file" ), Times.once() )
            .respond(
                HttpResponse.response()
                    .withStatusCode( HttpStatusCodes.OK )
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
            .when( HttpRequest.request().withMethod( "GET" ).withPath( "/file" )
                    .withHeader( Header.header( "If-Modified-Since", DateUtils.formatDate( date10 ) ) ),
                Times.once() )
            .respond(
                HttpResponse.response()
                    .withStatusCode( HttpStatusCodes.NOT_MODIFIED )
                    .withHeader( Header.header( "Last-Modified", DateUtils.formatDate( date10 ) ) )
            );
        fileSync.run();
        assertThat( localFile ).hasContent( "test" );
        assertThat( b ).contains( "f" );

        mockServer
            .reset()
            .when( HttpRequest.request().withMethod( "GET" ).withPath( "/file" )
                    .withHeader( Header.header( "If-Modified-Since", DateUtils.formatDate( date10 ) ) ),
                Times.once() )
            .respond(
                HttpResponse.response()
                    .withStatusCode( HttpStatusCodes.OK )
                    .withBody( "test2" )
                    .withHeader( Header.header( "Last-Modified", DateUtils.formatDate( date20 ) ) )
                    .withHeader( Header.header( "Content-Disposition", "inline; filename=\"test.file\"" ) )
            );
        fileSync.run();
        assertThat( localFile ).hasContent( "test2" );
        assertThat( b ).contains( "ff" );
    }
}
