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

import oap.dictionary.LogConfiguration;
import oap.io.IoStreams.Encoding;
import oap.json.Binder;
import oap.logstream.disk.DiskLoggerBackend;
import oap.template.Engine;
import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.Dates;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.net.Inet.HOSTNAME;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Asserts.assertString;
import static oap.testng.Env.tmpPath;

public class LoggerJsonTest extends Fixtures {
    private LogConfiguration logConfiguration;

    {
        fixture( TestDirectory.FIXTURE );
    }

    @BeforeMethod
    public void beforeMethod() {
        var engine = new Engine( Env.tmpPath( "file-cache" ), 1000 * 60 * 60 * 24 );
        logConfiguration = new LogConfiguration( engine, null, "test-logconfig" );
    }

    @Test
    public void diskJSON() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        var content = "{\"title\":\"response\",\"status\":false,\"values\":[1,2,3]}";

        try( DiskLoggerBackend backend = new DiskLoggerBackend( tmpPath( "logs" ), BPH_12, DEFAULT_BUFFER, logConfiguration ) ) {
            Logger logger = new Logger( backend );

            var o = Binder.json.unmarshalResource( getClass(), SimpleJson.class, "LoggerJsonTest/simple_json.json" );
            String jsonContent = Binder.json.marshal( o );
            assertString( jsonContent ).isEqualTo( content );

            logger.logWithoutTime( "open_rtb_json", "request_response", 0, 3, jsonContent );
        }

        assertFile( tmpPath( "logs/open_rtb_json/2015-10/10/request_response_v3_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( content + "\n", Encoding.GZIP );
    }

    public static class SimpleJson {
        public String title;
        public boolean status;
        public int[] values;
    }
}
