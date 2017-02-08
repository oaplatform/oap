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

package oap.ws;

import oap.application.Application;
import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.io.Files;
import oap.io.IoStreams;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Cuid;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertUploadFile;
import static oap.http.testng.HttpAsserts.reset;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 06.02.2017.
 */
public class WsFileUploaderTest extends AbstractTest {
    private ArrayList<WsFileUploader.Item> items = new ArrayList<>();
    private Server server;
    private WebServices ws;
    private SynchronizedThread listener;
    private Path path;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        path = Env.tmpPath( "/tmp" );

        items.clear();

        server = new Server( 100 );
        ws = new WebServices( server, new SessionManager( 10, null, "/" ),
            GenericCorsPolicy.DEFAULT, WsConfig.CONFIGURATION.fromResource( getClass(), "ws-multipart.conf" )
        );

        final WsFileUploader service = new WsFileUploader( path, 1024 * 9, -1 );
        service.addListener( item -> WsFileUploaderTest.this.items.add( item ) );
        Application.register( "upload", service );
        ws.start();
        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        listener.stop();
        server.stop();
        ws.stop();
        reset();

        Cuid.resetToDefaults();
    }

    @Test
    public void testUpload() throws IOException {
        final Path path = Env.tmpPath( "file.txt.gz" );
        Files.writeString( path, IoStreams.Encoding.GZIP, "v1" );

        Cuid.reset( "p", 1 );

        assertUploadFile( HTTP_PREFIX + "/upload/", "test/test2", path )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "{\"id\":\"1p\"}" );
        assertThat( items ).hasSize( 1 );
        assertThat( items.get( 0 ).prefix ).isEqualTo( "test/test2" );
        assertThat( items.get( 0 ).name ).isEqualTo( "file.txt.gz" );
        assertThat( items.get( 0 ).contentType ).isEqualTo( "application/gzip" );
        assertThat( IoStreams.asString( items.get( 0 ).isF.get(), IoStreams.Encoding.GZIP ) ).isEqualTo( "v1" );

    }

}
