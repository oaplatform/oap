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

import oap.io.Resources;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 20.02.2017.
 */
public class ContentTypeDetectorTest extends AbstractTest {
    @Test
    public void testContentType() throws IOException {
        final Path file = Resources.filePath( getClass(), "SampleVideo_1280x720_1mb.mp4" ).get();
        assertThat( ContentTypeDetector.get( file, Optional.empty() ) )
            .isEqualTo( "video/mp4" );

        final Path fileWithoutExtensions = Env.tmpPath( "1" );
        Files.copy(file, fileWithoutExtensions );

        assertThat( ContentTypeDetector.get( fileWithoutExtensions, Optional.empty() ) )
            .isEqualTo( "video/quicktime" );
        assertThat( ContentTypeDetector.get( fileWithoutExtensions, Optional.of("SampleVideo_1280x720_1mb.mp4") ) )
            .isEqualTo( "video/mp4" );

        assertThat( ContentTypeDetector.get( Resources.filePath( getClass(), "ws-multipart.conf" ).get(), Optional.empty() ) )
            .isEqualTo( "text/plain" );
    }

}