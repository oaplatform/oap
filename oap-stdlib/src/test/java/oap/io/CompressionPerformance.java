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

import oap.testng.AbstractPerformance;
import oap.testng.Env;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;

@Test( enabled = false )
public class CompressionPerformance extends AbstractPerformance {
    @DataProvider( name = "encodings" )
    public Object[][] encodings() {
        return new Object[][] { { GZIP }, { LZ4 } };
    }

    @Test( dataProvider = "encodings" )
    public void testC( IoStreams.Encoding encoding ) throws IOException {
        Path path = Env.tmpPath( "test." + encoding );

        benchmark( "compress " + encoding.name(), 2, () -> {
            try( InputStream is = IoStreams.in( Resources.filePath( getClass(), "/file.txt" ).get(), PLAIN );
                 OutputStream out = IoStreams.out( path, encoding, 1024 * 1024 * 10, false ) ) {

                byte[] bytes = new byte[1024 * 64];

                int read = -1;
                while( ( read = is.read( bytes ) ) > 0 ) {
                    out.write( bytes, 0, read );
                }
            }

        } ).run();

        System.out.println( encoding + " = " + path.toFile().length() + " bytes" );

        byte[] bytes = new byte[1024];

        benchmark( "decompress " + encoding.name(), 100, () -> {
            try( InputStream in = IoStreams.in( path, encoding ) ) {
                int read = 0;
                while( ( read = in.read( bytes ) ) > 0 ) {
                    read = read + 1;
                }
            }

        } ).run();

        System.out.println( encoding + " = " + path.toFile().length() + " bytes" );
    }
}
