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

import oap.io.Files;
import oap.io.IoStreams.Encoding;
import oap.testng.AbstractTest;
import oap.util.Dates;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.tmpPath;

public class WriterTest extends AbstractTest {

    @DataProvider
    public Object[][] encodings() {
        return new Object[][] { { PLAIN }, { GZIP } };
    }

    @Test( dataProvider = "encodings" )
    public void write( Encoding encoding ) {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        String content = "1234567890";
        byte[] bytes = content.getBytes();
        Path logs = tmpPath( "logs" );
        if( encoding == GZIP ) Files.writeString(
            logs.resolve( "test/2015-10/10/file-2015-10-10-01-00.log.gz" ),
            PLAIN, "corrupted file" );
        String ext = ".log" + encoding.extension;
        Writer writer = new Writer( logs, "test/file", ext, 10, 12 );

        writer.write( bytes );

        Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
        writer.write( bytes );

        Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
        writer.write( bytes );

        writer.close();

        writer = new Writer( logs, "test/file", ext, 10, 12 );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( bytes );

        Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
        writer.write( bytes );
        writer.close();
        assertFile( logs.resolve( "test/2015-10/10/file-2015-10-10-01-01" + ext ) )
            .hasContent( content, encoding );
        assertFile( logs.resolve( "test/2015-10/10/file-2015-10-10-01-02" + ext ) )
            .hasContent( content + content, encoding );
        assertFile( logs.resolve( "test/2015-10/10/file-2015-10-10-01-11" + ext ) )
            .hasContent( content, encoding );
        if( encoding == GZIP )
            assertFile( logs.resolve( ".corrupted/test/2015-10/10/file-2015-10-10-01-00.log.gz" ) )
                .hasContent( "corrupted file" );
        else
            assertFile( logs.resolve( "test/2015-10/10/file-2015-10-10-01-00" + ext ) )
                .hasContent( content, encoding );

    }
}
