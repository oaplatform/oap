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

package oap.fs;

import com.google.common.base.Charsets;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalFileManagerTest extends Fixtures {
    public LocalFileManagerTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    private LocalFileManager fileManager;
    private final Path tmp = TestDirectoryFixture.testPath( "/tmp/test" );

    @BeforeClass
    public void setUp() {
        fileManager = new LocalFileManager( Map.of( FileManager.DEFAULT_BUCKET, tmp ) );
    }

    @Test
    public void readWrite() throws Exception {
        fileManager.write( new Data( "file.txt", "text/plain", "dGVzdA==" ) );
        var read = fileManager.read( TestDirectoryFixture.testPath( "/tmp/test/file.txt" ).toString() );

        assertThat( read.isPresent() ).isTrue();
        assertThat( new String( read.get(), StandardCharsets.UTF_8 ) ).isEqualTo( "test" );
    }

    @AfterClass
    public void cleanUp() throws Exception {
        Files.deleteIfExists( tmp );
    }
}
