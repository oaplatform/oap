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
import oap.testng.Env;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.io.IoAsserts.assertFileContent;
import static oap.io.IoAsserts.assertFileDoesNotExist;


public class ArchiverTest {

    @Test
    public void testArchive() {
        DateTime now = new DateTime( 2015, 10, 10, 12, 0, 0 );
        DateTimeUtils.setCurrentMillisFixed( now.getMillis() );
        Path logs = Env.tmpPath( "logs" );
        Path archives = Env.tmpPath( "archives" );

        String[] files = {
            "a/1.log",
            "a/2.log",
            "a/3.log",
            "b/c/1.log",
            "b/c/2.log",
            "b/c/3.log"
        };

        for( String file : files ) {
            Path path = logs.resolve( file );
            Files.writeString( path, "data" );
            path.toFile().setLastModified(
                now.minusMinutes( file.contains( "3.log" ) ? 0 : 10 ).getMillis() );
        }


        Archiver archiver = new Archiver( logs, archives, 60 * 1000, "**/*.log" );
        archiver.run();

        for( String file : files ) {
            Path path = archives.resolve( file + ".gz" );
            if( file.contains( "3.log" ) ) assertFileDoesNotExist( path );
            else {
                assertFileContent( path, IoStreams.Encoding.GZIP, "data" );
                assertFileDoesNotExist( logs.resolve( file ) );
            }
        }
    }

}
