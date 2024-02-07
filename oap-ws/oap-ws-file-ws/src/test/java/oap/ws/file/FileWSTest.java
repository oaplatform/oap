/*
  The MIT License (MIT)
  <p>
  Copyright (c) Open Application Platform Authors
  <p>
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  <p>
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  <p>
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package oap.ws.file;

import oap.application.testng.KernelFixture;
import oap.http.Http;
import oap.io.Files;
import oap.io.content.ContentWriter;
import oap.testng.Env;
import oap.testng.Fixtures;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class FileWSTest extends Fixtures {

    private final KernelFixture kernelFixture;

    public FileWSTest() {
        fixture( oap.testng.TestDirectoryFixture.FIXTURE );
        kernelFixture = fixture( new KernelFixture( urlOfTestResource( getClass(), "application.test.conf" ) ) );
    }

    @Test
    public void upload() {
        assertPost( httpUrl( "/file" ), contentOfTestResource( getClass(), "data-complex.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.TEXT_PLAIN, "file.txt" );
        assertThat( Env.tmpPath( "default/file.txt" ) ).hasContent( "test" );

        assertPost( httpUrl( "/file?bucket=b1" ), contentOfTestResource( getClass(), "data-single.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.TEXT_PLAIN, "file.txt" );
        assertThat( Env.tmpPath( "b1/file.txt" ) ).hasContent( "test" );
    }

    @Test
    public void download() {
        Files.write( Env.tmpPath( "default/test.txt" ), "test", ContentWriter.ofString() );
        assertGet( httpUrl( "/file?path=test.txt" ) )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.TEXT_PLAIN, "test" );

        Files.write( Env.tmpPath( "b1/test.txt" ), "b1test", ContentWriter.ofString() );
        assertGet( httpUrl( "/file?path=test.txt&bucket=b1" ) )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.TEXT_PLAIN, "b1test" );
    }
}
