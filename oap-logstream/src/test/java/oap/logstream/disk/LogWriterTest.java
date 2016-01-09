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

import oap.io.IoAsserts;
import oap.logstream.disk.LogWriter;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;

public class LogWriterTest extends AbstractTest {

    @Test
    public void write() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        String content = "1234567890\n";
        LogWriter logWriter = new LogWriter( Env.tmpPath( "logs" ), "file", "log", 10, 5 );

        logWriter.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
        logWriter.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
        logWriter.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        logWriter.write( content.getBytes() );

        Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
        logWriter.write( content.getBytes() );

        logWriter.close();
        IoAsserts.assertFileContent( Env.tmpPath( "logs/2015-10/10/file-2015-10-10-01-00.log" ), content );
        IoAsserts.assertFileContent( Env.tmpPath( "logs/2015-10/10/file-2015-10-10-01-01.log" ), content );
        IoAsserts.assertFileContent( Env.tmpPath( "logs/2015-10/10/file-2015-10-10-01-02.log" ), content + content );
        IoAsserts.assertFileContent( Env.tmpPath( "logs/2015-10/10/file-2015-10-10-01-11.log" ), content );

    }
}
