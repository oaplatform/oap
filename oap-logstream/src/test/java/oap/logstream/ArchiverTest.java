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
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.io.IoAsserts.assertFileContent;
import static oap.io.IoAsserts.assertFileDoesNotExist;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;


public class ArchiverTest extends AbstractTest {

    @DataProvider
    public Object[][] compress() {
        return new Object[][]{
            { true },
            { false }
        };
    }

    @Test( dataProvider = "compress" )
    public void archive( boolean compress ) {
        DateTime now = new DateTime( 2015, 10, 10, 12, 0, 0 );
        DateTimeUtils.setCurrentMillisFixed( now.getMillis() );
        Path logs = Env.tmpPath( "logs" );
        Path archives = Env.tmpPath( "archives" );
        System.out.println( Filename.formatDate( DateTime.now(), 12 ) );
        String[] files = {
            "a/a_2015-10-10-11-10.log",
            "a/a_2015-10-10-11-11.log",
            "a/a_2015-10-10-12-00.log",
            "a/b_2015-10-10-11-11.log",
            "b/c/a_2015-10-10-11-10.log",
            "b/c/a_2015-10-10-11-11.log",
            "b/c/a_2015-10-10-12-00.log",
            "b/c/b_2015-10-10-11-11.log"
        };

        for( String file : files ) {
            Path path = logs.resolve( file );
            Files.writeString( path, "data" );
            path.toFile().setLastModified( now.minusSeconds( file.contains( "b_2015" ) ? 0 : 11 ).getMillis() );
        }

        Archiver archiver = new Archiver( logs, archives, 10000, "**/*.log", compress, 12 );
        archiver.run();

        for( String file : files ) {
            Path path = archives.resolve( file + ( compress ? ".gz" : "" ) );
            if( file.contains( "b_2015" ) || file.contains( "12-00" ) )
                assertFileDoesNotExist( path );
            else {
                assertFileContent( path, compress ? GZIP : PLAIN, "data" );
                assertFileDoesNotExist( logs.resolve( file ) );
            }
        }
    }
}
