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

package oap.io;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.content.ContentWriter.ofString;
import static oap.testng.Asserts.assertFile;

public class SafeFileOutputStreamTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void rename() throws IOException {
        Path path = TestDirectoryFixture.testPath( "1" );
        SafeFileOutputStream stream = new SafeFileOutputStream( path, false, PLAIN );
        stream.write( "111".getBytes() );
        stream.flush();
        assertFile( path ).doesNotExist();
        stream.close();
        assertFile( path ).hasContent( "111" );
    }

    @Test
    public void removeIfEmpty() throws IOException {
        Path path = TestDirectoryFixture.testPath( "1" );
        SafeFileOutputStream stream = new SafeFileOutputStream( path, false, PLAIN );
        stream.flush();
        stream.close();
        assertFile( path ).doesNotExist();

    }

    @Test
    public void append() throws IOException {
        Path path = TestDirectoryFixture.testPath( "1" );
        Files.write( path, "test", ofString() );
        try( SafeFileOutputStream stream = new SafeFileOutputStream( path, true, PLAIN ) ) {
            stream.write( "2".getBytes() );
        }
        assertFile( path ).hasContent( "test2" );
    }
}
