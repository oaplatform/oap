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

import lombok.val;
import oap.dictionary.LogConfiguration;
import oap.io.IoStreams.Encoding;
import oap.logstream.disk.DiskLoggerBackend;
import oap.logstream.net.SocketLoggerBackend;
import oap.logstream.net.SocketLoggerServer;
import oap.template.Engine;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Dates;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_FREE_SPACE_REQUIRED;
import static oap.net.Inet.HOSTNAME;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.tmpPath;
import static oap.util.Dates.formatDateWithMillis;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LoggerTest extends AbstractTest {
    private LogConfiguration logConfiguration;

    @BeforeMethod
    public void beforeMethod() {
        val engine = new Engine( Paths.get( "/tmp/file-cache" ), 1000 * 60 * 60 * 24 );
        logConfiguration = new LogConfiguration( engine, null, "test-logconfig" );
    }

    @Test
    public void disk() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        val content = "12345678";
        val contentWithHeaders = "DATETIME\tREQUEST_ID\tREQUEST_ID2\n" + formatDateWithMillis( currentTimeMillis() ) + "\t12345678";
        val content2WithHeaders = "DATETIME\tREQUEST_ID2\n" + formatDateWithMillis( currentTimeMillis() ) + "\t12345678";
        try( DiskLoggerBackend backend = new DiskLoggerBackend( tmpPath( "logs" ), BPH_12, DEFAULT_BUFFER, logConfiguration ) ) {
            Logger logger = new Logger( backend );
            logger.log( "lfn1", "log", 2, content );
            logger.log( "lfn2", "log", 2, content );
            logger.log( "lfn1", "log", 2, content );
            logger.log( "lfn1", "log2", 2, content );
        }

        assertFile( tmpPath( "logs/lfn1/2015-10/10/log_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( contentWithHeaders + "\n"
                + formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/lfn2/2015-10/10/log_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( contentWithHeaders + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/lfn1/2015-10/10/log2_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( content2WithHeaders + "\n", Encoding.GZIP );
    }

    @Test
    public void net() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        val content = "12345678";
        val contentWithHeaders = "DATETIME\tREQUEST_ID\tREQUEST_ID2\n" + formatDateWithMillis( currentTimeMillis() ) + "\t12345678";
        val content2WithHeaders = "DATETIME\tREQUEST_ID2\n" + formatDateWithMillis( currentTimeMillis() ) + "\t12345678";

        try( val serverBackend = new DiskLoggerBackend( tmpPath( "logs" ), BPH_12, DEFAULT_BUFFER, logConfiguration ) ) {
            SocketLoggerServer server = new SocketLoggerServer( Env.port( "net" ), 1024, serverBackend, tmpPath( "control" ) );
            try( val clientBackend = new SocketLoggerBackend( ( byte ) 1, "localhost", Env.port( "net" ),
                tmpPath( "buffers" ), 70 ) ) {

                serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED * 1000L;
                assertFalse( serverBackend.isLoggingAvailable() );
                val logger = new Logger( clientBackend );
                logger.log( "lfn1", "log", 2, content );
                clientBackend.send();
                assertFalse( logger.isLoggingAvailable() );
                server.start();
                clientBackend.send();
                assertFalse( logger.isLoggingAvailable() );
                serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
                assertTrue( serverBackend.isLoggingAvailable() );
                clientBackend.send();
                assertTrue( logger.isLoggingAvailable() );
                logger.log( "lfn2", "log", 2, content );
                logger.log( "lfn1", "log", 2, content );
                logger.log( "lfn1", "log2", 2, content );
                clientBackend.send();
            } finally {
                server.stop();
            }
        }

        assertFile( tmpPath( "logs/lfn1/2015-10/10/log_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( contentWithHeaders + "\n"
                + formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/lfn2/2015-10/10/log_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( contentWithHeaders + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/lfn1/2015-10/10/log2_v2_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( content2WithHeaders + "\n", Encoding.GZIP );
    }
}
