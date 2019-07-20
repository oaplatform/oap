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

package oap.media;

import oap.application.Kernel;
import oap.concurrent.SynchronizedThread;
import oap.http.PlainHttpListener;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.io.Files;
import oap.media.postprocessing.VastMediaProcessing;
import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.Cuid;
import oap.util.Lists;
import oap.util.Pair;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import oap.ws.WsConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;

import static java.util.Collections.singletonList;
import static oap.http.testng.HttpAsserts.assertUploadFile;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.http.testng.HttpAsserts.reset;
import static oap.io.CommandLine.shell;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class WsFileUploaderTest extends Fixtures {
    private ArrayList<Pair<Media, MediaInfo>> medias = new ArrayList<>();
    private Server server;
    private WebServices ws;
    private SynchronizedThread listener;
    private Kernel kernel;

    {
        fixture( TestDirectory.FIXTURE );
    }

    @BeforeMethod
    public void init() {
        Env.resetPorts();

        kernel = new Kernel( Lists.empty() );
        kernel.start();
        Path path = Env.tmpPath( "tmp" );

        Files.ensureDirectory( path );

        medias.clear();

        server = new Server( 100, false );
        server.start();
        ws = new WebServices( kernel, server, new SessionManager( 10, null, "/" ),
            GenericCorsPolicy.DEFAULT,
            WsConfig.CONFIGURATION.fromResource( getClass(), "ws-multipart.conf" )
        );

        WsFileUploader uploader = new WsFileUploader( path, 1024 * 1024, -1,
            singletonList( new VastMediaProcessing(
                shell( "ffprobe -v quiet -print_format xml -show_format -sexagesimal -show_streams {FILE}" ), 10000L
            ) ),
            Cuid.incremental( 1 )
        );
        uploader.addListener( ( media, mediaInfo, mediaContext ) -> WsFileUploaderTest.this.medias.add( __( media, mediaInfo ) ) );
        kernel.register( "upload", uploader );
        ws.start();
        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    @AfterMethod
    public void stop() {
        listener.stop();
        server.stop();
        ws.stop();
        kernel.stop();
        reset();
    }

    @Test
    public void uploadVideo() {
        Path path = pathOfTestResource( getClass(), "video.mp4" );

        assertUploadFile( httpUrl( "/upload/" ), "test/test2", path )
            .isOk()
            .is( r -> {
                WsFileUploader.MediaResponse resp = r.<WsFileUploader.MediaResponse>unmarshal( WsFileUploader.MediaResponse.class ).get();
                assertThat( resp.id ).isEqualTo( "test/test2/2.mp4" );
                assertThat( resp.info.get( "vast" ) ).isNotNull();
                assertThat( resp.info.get( "Content-Type" ) ).isEqualTo( "video/mp4" );

                assertThat( medias ).hasSize( 1 );
                assertThat( medias.get( 0 )._1.id ).startsWith( "test/test2/2.mp4" );
                assertThat( medias.get( 0 )._1.name ).isEqualTo( "video.mp4" );
                assertThat( medias.get( 0 )._1.contentType ).isEqualTo( "video/mp4" );
                assertThat( medias.get( 0 )._2.get( "vast" ) ).isNotNull();
                assertThat( resp.info.get( "Content-Type" ) ).isEqualTo( "video/mp4" );

            } );
    }

    @Test
    public void uploadImage() {
        Path path = pathOfTestResource( getClass(), "image.png" );

        assertUploadFile( httpUrl( "/upload/" ), "test/test2", path )
            .isOk()
            .is( r -> {
                WsFileUploader.MediaResponse resp = r.<WsFileUploader.MediaResponse>unmarshal( WsFileUploader.MediaResponse.class ).get();
                assertThat( resp.id ).isEqualTo( "test/test2/2.png" );
                assertThat( resp.info.get( "Content-Type" ) ).isEqualTo( "image/png" );

                assertThat( medias ).hasSize( 1 );
                assertThat( medias.get( 0 )._1.id ).isEqualTo( "test/test2/2.png" );
                assertThat( medias.get( 0 )._1.name ).isEqualTo( "image.png" );
                assertThat( medias.get( 0 )._1.contentType ).isEqualTo( "image/png" );
                assertThat( resp.info.get( "Content-Type" ) ).isEqualTo( "image/png" );
            } );

    }
}
