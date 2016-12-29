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

package oap.logstream;

import oap.io.Files;
import oap.io.IoStreams;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.testng.Asserts.assertFile;

public class ArchiverTest extends AbstractTest {

    @DataProvider
    public Object[][] encoding() {
        return new Object[][] {
            { PLAIN, LZ4 },
            { PLAIN, PLAIN },
            { GZIP, PLAIN },
            { GZIP, LZ4 }
        };
    }

    @Test( dataProvider = "encoding" )
    public void archive( IoStreams.Encoding srcEncoding, IoStreams.Encoding destEncoding ) throws IOException {
        DateTime now = new DateTime( 2015, 10, 10, 12, 0, 0 );
        DateTimeUtils.setCurrentMillisFixed( now.getMillis() );
        Path logs = Env.tmpPath( "logs" );
        Path archives = Env.tmpPath( "archives" );
        String[] files = {
            "a/a-2015-10-10-11-10.log" + srcEncoding.extension,
            "a/a-2015-10-10-11-11.log" + srcEncoding.extension,
            "a/a-2015-10-10-12-00.log" + srcEncoding.extension,
            "a/b-2015-10-10-11-11.log" + srcEncoding.extension,
            "b/c/a-2015-10-10-11-10.log" + srcEncoding.extension,
            "b/c/a-2015-10-10-11-11.log" + srcEncoding.extension,
            "b/c/a-2015-10-10-12-00.log" + srcEncoding.extension,
            "b/c/a-2015-10-10-12-01.log" + srcEncoding.extension,
            "b/c/b-2015-10-10-11-11.log" + srcEncoding.extension
        };

        for( String file : files ) Files.writeString( logs.resolve( file ), srcEncoding, "data" );

        String corrupted = "a/corrupted-2015-10-10-11-10.txt.gz";
        Files.writeString( logs.resolve( corrupted ), PLAIN, "data" );

        Archiver archiver = new Archiver( logs, archives, 10000, "**/*", destEncoding, 12 );
        archiver.run();

        for( String file : files )
            assertFile( archives.resolve( destEncoding.resolve( Paths.get( file ) ) ) ).doesNotExist();

        DateTimeUtils.setCurrentMillisFixed( now.plusSeconds( 11 ).getMillis() );
        archiver.run();

        for( String file : files ) {
            Path path = archives.resolve( destEncoding.resolve( Paths.get( file ) ) );
            if( file.contains( "-12-00." ) || file.contains( "-12-01." ) )
                assertFile( path ).doesNotExist();
            else {
                assertFile( logs.resolve( file ) ).doesNotExist();
                assertFile( path ).hasContent( "data", destEncoding );
            }
        }
        assertFile( archiver.corruptedDirectory.resolve( corrupted ) ).exists();
        assertFile( logs.resolve( corrupted ) ).doesNotExist();
        assertFile( archives.resolve( corrupted ) ).doesNotExist();
    }
}
