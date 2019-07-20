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

import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static oap.testng.Asserts.pathOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class MediaUtilsTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    @Test
    public void getContentType() throws IOException {
        Path file = pathOfTestResource( WsFileUploaderTest.class, "video.mp4" );
        assertThat( MediaUtils.getContentType( file, Optional.empty() ) )
            .isEqualTo( "video/mp4" );

        Path fileWithoutExtensions = Env.tmpPath( "1" );
        Files.copy( file, fileWithoutExtensions );

        assertThat( MediaUtils.getContentType( fileWithoutExtensions, Optional.empty() ) )
            .isEqualTo( "video/quicktime" );
        assertThat( MediaUtils.getContentType( fileWithoutExtensions, Optional.of( "video.mp4" ) ) )
            .isEqualTo( "video/mp4" );

        assertThat( MediaUtils.getContentType( pathOfTestResource( getClass(), "test.txt" ), Optional.empty() ) )
            .isEqualTo( "text/plain" );
    }

}
