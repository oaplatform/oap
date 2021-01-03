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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static oap.benchmark.Benchmark.benchmark;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.IoStreams.Encoding.ZSTD;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.TestDirectoryFixture.testPath;

@Test( enabled = true )
public class CompressionPerftest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @DataProvider( name = "encodings" )
    public Object[][] encodings() {
        return new Object[][] { { GZIP }, { LZ4 }, { ZSTD } };
    }

    @Test( dataProvider = "encodings" )
    public void testC( IoStreams.Encoding encoding ) {
        Path path = testPath( "test." + encoding );
        Path source = pathOfTestResource( getClass(), "file.txt" );
        benchmark( "compress " + encoding.name(), 2, () -> {
            try( InputStream is = IoStreams.in( source, PLAIN );
                 OutputStream out = IoStreams.out( path, encoding, 1024 * 1024 * 10, false ) ) {

                byte[] bytes = new byte[1024 * 64];

                int read;
                while( ( read = is.read( bytes ) ) > 0 ) out.write( bytes, 0, read );
            }

        } ).run();

        System.out.println( "compressed size for " + encoding + " = " + path.toFile().length() + " bytes" );

        byte[] bytes = new byte[1024];

        benchmark( "decompress " + encoding.name(), 20, () -> {
            try( InputStream in = IoStreams.in( path, encoding ) ) {
                int read;
                while( ( read = in.read( bytes ) ) > 0 ) read = read + 1;
            }

        } ).run();
    }
}
