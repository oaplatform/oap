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

package oap.logstream.disk;

import oap.io.IoStreams;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Dates;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static oap.io.IoAsserts.assertFileContent;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

public class WriterTest extends AbstractTest {

    @DataProvider
    public Object[][] encodings() {
        return new Object[][]{ { PLAIN }, { GZIP } };
    }

    @Test( dataProvider = "encodings" )
    public void testWrite( IoStreams.Encoding encoding ) throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        String content = "1234567890";
        Writer writer = new Writer( Env.tmpPath( "logs" ), "test/file", "log", 10, 12, GZIP == encoding );

        writer.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
        writer.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
        writer.write( content.getBytes() );

        writer.close();

        writer = new Writer( Env.tmpPath( "logs" ), "test/file", "log", 10, 12, GZIP == encoding );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
        writer.write( content.getBytes() );
        writer.close();
        assertFileContent( Env.tmpPath( "logs/test/2015-10/10/file-2015-10-10-01-00.log" ), encoding, content );
        assertFileContent( Env.tmpPath( "logs/test/2015-10/10/file-2015-10-10-01-01.log" ), encoding, content );
        assertFileContent( Env.tmpPath( "logs/test/2015-10/10/file-2015-10-10-01-02.log" ), encoding, content + content );
        assertFileContent( Env.tmpPath( "logs/test/2015-10/10/file-2015-10-10-01-11.log" ), encoding, content );
    }
}
