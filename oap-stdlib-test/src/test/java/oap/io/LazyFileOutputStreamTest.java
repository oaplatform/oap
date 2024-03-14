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

import lombok.SneakyThrows;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyFileOutputStreamTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LazyFileOutputStreamTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    @SneakyThrows
    public void write() {
        var path = testDirectoryFixture.testPath( "file1.txt" );

        new LazyFileOutputStream( path ).close();

        assertThat( path ).doesNotExist();

        try( var lfos = new LazyFileOutputStream( path ) ) {
            lfos.write( '1' );
        }

        assertThat( path ).hasContent( "1" );
    }
}
